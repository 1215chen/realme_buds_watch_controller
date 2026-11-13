package com.cyhao.realmebuds.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.AncLevel
import com.cyhao.realmebuds.data.AncMode
import com.cyhao.realmebuds.data.BatteryInfo
import com.cyhao.realmebuds.data.BudsRepository
import com.cyhao.realmebuds.data.BudsState
import com.cyhao.realmebuds.data.ConnectionState
import com.cyhao.realmebuds.data.AppSettings
import com.cyhao.realmebuds.data.DeviceCapabilities
import com.cyhao.realmebuds.data.EarSide
import com.cyhao.realmebuds.data.EqPreset
import com.cyhao.realmebuds.data.GestureAction
import com.cyhao.realmebuds.data.GestureType
import com.cyhao.realmebuds.data.eqPresetFromId
import com.cyhao.realmebuds.data.ScannedDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 真实 BLE 实现。
 *
 * 架构（详见 `docs/BLE_IMPL.md`）：
 * - [BleScanner]：BLE 广播扫描、设备识别/去重、RSSI 刷新、15s 自动停止；
 * - [GattTransport]：`connectGatt(TRANSPORT_LE)` → 服务发现 → MTU → 通知订阅 →
 *   BAS/DIS 标准读取；命中私有写/通知特征时提供 OPO 控制通道；
 * - [RfcommTransport]：realme Air 系列实际使用的经典蓝牙 RFCOMM 控制通道兜底；
 * - [RealmeProtocol] + [FrameEncoder]/[FrameAssembler]：OPO v1 / Telink 两种帧方言的
 *   命令构造与通知解析；
 * - 本类：连接状态机、串行命令队列（ACK/超时/重试）、状态映射、自动重连、生命周期。
 *
 * 所有 BLE 回调与命令均串行化到单个 HandlerThread，StateFlow 更新线程安全。
 */
class BleBudsRepository(
    context: Context,
    initialDevice: ScannedDevice? = null,
) : BudsRepository {

    private val appContext: Context = context.applicationContext
    private val settings = AppSettings(appContext)

    private val _state = MutableStateFlow(BudsState())
    override val state: StateFlow<BudsState> = _state.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val scanResults: StateFlow<List<ScannedDevice>> = _scanResults.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    override val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    override val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private val handlerThread = HandlerThread("budswear-ble").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter: BluetoothAdapter? get() = try {
        bluetoothManager?.adapter
    } catch (e: SecurityException) {
        // 未授予 BLUETOOTH_CONNECT 时平台可能直接抛 SecurityException；扫描/连接会再检查权限。
        null
    }

    // --------------------------------------------------------------- 协议/帧层
    private val frameEncoder = FrameEncoder()
    private val protocol = RealmeProtocol(BleConstants.BLE_HANDSHAKE_TOKEN)
    private val assembler = FrameAssembler()
    private var dialect: FrameDialect = FrameDialect.OPO
    private var receivedFrameCount: Int = 0

    // ------------------------------------------------------------------- 扫描器
    private val scanner = BleScanner(
        appContext = appContext,
        adapter = adapter,
        handler = handler,
        onResults = { results -> _scanResults.value = results },
        onScanningChanged = { scanningNow -> _scanning.value = scanningNow },
        onFailed = { _messages.tryEmit(R.string.msg_bt_not_ready) },
        onFinished = { _messages.tryEmit(R.string.msg_scan_finished) },
    )

    // ----------------------------------------------------------------- 命令队列
    private val commandQueue = ArrayDeque<PendingCommand>()
    private var pendingCommand: PendingCommand? = null
    private var pendingTimeoutRunnable: Runnable? = null

    // ----------------------------------------------------------------- 连接状态
    private var gattTransport: GattTransport? = null
    private var rfcommTransport: RfcommTransport? = null
    private var controlChannel: ControlChannel = ControlChannel.NONE
    private var gattReady: Boolean = false
    private var rfcommReady: Boolean = false
    private var rfcommTried: Boolean = false
    private var targetAddress: String? = null
    private var targetName: String = ""
    private var targetIsRealme: Boolean = false
    private var lastDevice: ScannedDevice? = null
    private var modelFromGatt: String = ""
    private var firmwareFromGatt: String = ""
    private var reconnectAttempts: Int = 0
    private var userDisconnect: Boolean = false
    private var connectedAnnounced: Boolean = false
    private var closed: Boolean = false

    private val connectTimeoutRunnable = Runnable { onConnectTimeout() }
    private val reconnectRunnable = Runnable { reconnectLastDevice() }

    // ================================================================== 扫描
    override fun startScan() = runOnBleThread {
        if (closed) return@runOnBleThread
        if (!hasScanPermission()) {
            _messages.tryEmit(R.string.msg_bt_not_ready)
            return@runOnBleThread
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !isAdapterReady(bluetoothAdapter)) {
            _messages.tryEmit(R.string.msg_bt_not_ready)
            return@runOnBleThread
        }
        if (scanner.start()) {
            _messages.tryEmit(R.string.msg_scanning)
        } else {
            _messages.tryEmit(R.string.msg_bt_not_ready)
        }
    }

    override fun stopScan() = runOnBleThread { scanner.stop() }

    // ================================================================== 连接
    override fun connect(device: ScannedDevice) = runOnBleThread {
        connectInternal(device, isReconnect = false)
    }

    override fun disconnect() = runOnBleThread {
        userDisconnect = true
        settings.clearDevice()
        handler.removeCallbacks(connectTimeoutRunnable)
        handler.removeCallbacks(reconnectRunnable)
        closeTransports()
        resetLinkState()
        updateState {
            copy(
                connection = ConnectionState.Disconnected,
                battery = BatteryInfo(),
            )
        }
        _messages.tryEmit(R.string.msg_disconnected)
        userDisconnect = false
    }

    @SuppressLint("MissingPermission")
    private fun connectInternal(device: ScannedDevice, isReconnect: Boolean) {
        if (closed) return
        if (!hasConnectPermission()) {
            _messages.tryEmit(R.string.msg_bt_not_ready)
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !isAdapterReady(bluetoothAdapter)) {
            _messages.tryEmit(R.string.msg_bt_not_ready)
            return
        }

        scanner.stop()
        handler.removeCallbacks(connectTimeoutRunnable)
        handler.removeCallbacks(reconnectRunnable)
        closeTransports()
        resetLinkState()
        commandQueue.clear()
        pendingCommand = null
        cancelPendingTimeout()

        lastDevice = device
        targetAddress = device.address
        targetName = device.name
        targetIsRealme = device.realme
        modelFromGatt = ""
        firmwareFromGatt = ""
        dialect = FrameDialect.OPO
        receivedFrameCount = 0
        userDisconnect = false
        if (!isReconnect) reconnectAttempts = 0

        updateState {
            copy(
                connection = ConnectionState.Connecting,
                name = device.name,
                address = device.address,
                isRealme = device.realme,
                model = "",
                firmware = "",
                battery = BatteryInfo(),
            )
        }
        _messages.tryEmit(R.string.msg_connecting)

        handler.postDelayed(connectTimeoutRunnable, BleConstants.CONNECT_TIMEOUT_MS)
        val transport = GattTransport(
            context = appContext,
            adapter = bluetoothAdapter,
            handler = handler,
            address = device.address,
            callbacks = gattCallbacks,
        )
        gattTransport = transport
        transport.start()
    }

    private fun startRfcommConnection() {
        if (closed || rfcommTried) return
        val bluetoothAdapter = adapter ?: return
        val address = targetAddress ?: return
        rfcommTried = true
        handler.postDelayed(connectTimeoutRunnable, BleConstants.RFCOMM_CONNECT_TIMEOUT_MS + 2_000L)
        val transport = RfcommTransport(
            adapter = bluetoothAdapter,
            handler = handler,
            address = address,
            callbacks = rfcommCallbacks,
        )
        rfcommTransport = transport
        transport.start()
    }

    private fun onConnectTimeout() {
        if (closed) return
        if (connectedAnnounced && controlChannel == ControlChannel.NONE) return
        if (controlChannel != ControlChannel.NONE) return
        Log.i(TAG, "connect timeout, gattReady=$gattReady rfcommTried=$rfcommTried")
        closeGattTransport()
        if (targetIsRealme && !rfcommTried) {
            startRfcommConnection()
        } else {
            failConnection()
        }
    }

    private fun reconnectLastDevice() {
        val device = lastDevice
        if (device == null) {
            failConnection()
            return
        }
        connectInternal(device, isReconnect = true)
    }

    private fun failConnection() {
        if (closed) return
        handler.removeCallbacks(connectTimeoutRunnable)
        handler.removeCallbacks(reconnectRunnable)
        closeTransports()
        resetLinkState()
        commandQueue.clear()
        pendingCommand = null
        cancelPendingTimeout()
        updateState {
            copy(
                connection = ConnectionState.Disconnected,
                battery = BatteryInfo(),
            )
        }
        _messages.tryEmit(R.string.msg_failed)
    }

    private fun handleLinkLost() {
        if (closed) return
        closeTransports()
        resetLinkState()
        commandQueue.clear()
        pendingCommand = null
        cancelPendingTimeout()
        if (userDisconnect) {
            userDisconnect = false
            updateState { copy(connection = ConnectionState.Disconnected, battery = BatteryInfo()) }
            _messages.tryEmit(R.string.msg_disconnected)
            return
        }
        if (reconnectAttempts < BleConstants.MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            updateState { copy(connection = ConnectionState.Connecting) }
            _messages.tryEmit(R.string.msg_connecting)
            handler.postDelayed(reconnectRunnable, reconnectDelayMs(reconnectAttempts))
        } else {
            updateState { copy(connection = ConnectionState.Disconnected, battery = BatteryInfo()) }
            _messages.tryEmit(R.string.msg_disconnected)
        }
    }

    private fun resetLinkState() {
        controlChannel = ControlChannel.NONE
        gattReady = false
        rfcommReady = false
        rfcommTried = false
        connectedAnnounced = false
        assembler.reset()
        receivedFrameCount = 0
    }

    private fun closeGattTransport() {
        gattTransport?.close()
        gattTransport = null
        gattReady = false
    }

    private fun closeTransports() {
        closeGattTransport()
        rfcommTransport?.close()
        rfcommTransport = null
        rfcommReady = false
    }

    private val gattCallbacks = object : GattTransport.Callbacks {
        override fun onGattReady(opoChannelAvailable: Boolean) {
            if (closed) return
            gattReady = true
            if (opoChannelAvailable) {
                activeControlChannel(ControlChannel.BLE_GATT)
            } else {
                // 标准 GATT 可用：先展示电量/型号/固件，再尝试 RFCOMM 拿私有控制通道。
                announceConnectedIfNeeded()
                if (targetIsRealme && !rfcommTried) startRfcommConnection()
            }
        }

        override fun onGattData(data: ByteArray) = onTransportData(data)

        override fun onGattBatteryLevel(level: Int) {
            if (closed) return
            // 私有协议电量更精确（左右/盒分别上报）；只有纯标准兜底时才用 BAS 整机电量。
            if (controlChannel != ControlChannel.NONE) return
            updateState { copy(battery = battery.copy(right = level)) }
        }

        override fun onGattModel(model: String) {
            modelFromGatt = model
            if (controlChannel == ControlChannel.NONE && model.isNotBlank()) {
                updateState { copy(model = model) }
            }
        }

        override fun onGattFirmware(firmware: String) {
            firmwareFromGatt = firmware
            if (controlChannel == ControlChannel.NONE && firmware.isNotBlank()) {
                updateState { copy(firmware = firmware) }
            }
        }

        override fun onGattDisconnected() {
            if (closed) return
            gattReady = false
            gattTransport = null
            when {
                !connectedAnnounced -> {
                    if (targetIsRealme && !rfcommTried) startRfcommConnection() else failConnection()
                }

                controlChannel == ControlChannel.BLE_GATT -> handleLinkLost()
                // RFCOMM 控制通道仍在：GATT 掉线不影响控制，忽略。
                else -> Unit
            }
        }

        override fun onGattFailed() {
            if (closed) return
            gattReady = false
            if (connectedAnnounced) return
            if (targetIsRealme && !rfcommTried) {
                startRfcommConnection()
            } else {
                failConnection()
            }
        }
    }

    private val rfcommCallbacks = object : RfcommTransport.Callbacks {
        override fun onRfcommReady() {
            if (closed) return
            rfcommReady = true
            activeControlChannel(ControlChannel.RFCOMM)
        }

        override fun onRfcommData(data: ByteArray) = onTransportData(data)

        override fun onRfcommDisconnected() {
            if (closed) return
            rfcommReady = false
            rfcommTransport = null
            if (controlChannel == ControlChannel.RFCOMM) {
                handleLinkLost()
            } else if (!connectedAnnounced) {
                if (gattReady) announceConnectedIfNeeded() else failConnection()
            }
        }

        override fun onRfcommFailed() {
            if (closed) return
            rfcommReady = false
            rfcommTransport = null
            if (!connectedAnnounced) {
                if (gattReady) announceConnectedIfNeeded() else failConnection()
            }
            // 已经处于连接状态：保留当前（标准 GATT 或已有控制通道）。
        }
    }

    /** 私有控制通道就绪：广播连接成功并开始拉取初始状态。 */
    private fun activeControlChannel(channel: ControlChannel) {
        if (closed) return
        controlChannel = channel
        handler.removeCallbacks(connectTimeoutRunnable)
        assembler.reset()
        receivedFrameCount = 0
        announceConnectedIfNeeded()
        startInitSequence()
    }

    private fun announceConnectedIfNeeded() {
        if (connectedAnnounced) return
        connectedAnnounced = true
        reconnectAttempts = 0
        val current = _state.value
        val resolvedModel = current.model.ifBlank { modelFromGatt.ifBlank { targetName } }
        updateState {
            copy(
                connection = ConnectionState.Connected,
                name = targetName,
                address = targetAddress.orEmpty(),
                isRealme = targetIsRealme || controlChannel != ControlChannel.NONE,
                model = resolvedModel,
                firmware = current.firmware.ifBlank { firmwareFromGatt },
                // 能力项完全由协议通知动态确认（入耳/多点/ANC 等级），不做机型写死
            )
        }
        // 记住本次成功连接的设备，下次启动自动回连
        settings.saveDevice(
            ScannedDevice(
                name = targetName,
                address = targetAddress.orEmpty(),
                rssi = 0,
                realme = targetIsRealme,
            )
        )
        _messages.tryEmit(R.string.msg_connected)
    }

    // ============================================================== 命令队列
    private fun sendCommand(
        command: ProtocolCommand,
        onSuccess: (() -> Unit)? = null,
        silent: Boolean = false,
        probeDialect: Boolean = false,
    ) {
        if (closed) return
        if (controlChannel == ControlChannel.NONE) {
            if (!silent) _messages.tryEmit(R.string.msg_failed)
            return
        }
        if (!silent) dropQueuedIntrospection()
        commandQueue.addLast(
            PendingCommand(
                protocolCommand = command,
                onSuccess = onSuccess,
                silent = silent,
                probeDialect = probeDialect,
            )
        )
        if (pendingCommand == null) sendNextCommand()
    }

    /** 用户操作优先：丢掉尚未发送的静默初始化查询，避免设置命令等待过久。 */
    private fun dropQueuedIntrospection() {
        commandQueue.removeAll { it.silent }
    }

    private fun sendNextCommand() {
        cancelPendingTimeout()
        val next = commandQueue.removeFirstOrNull()
        if (next == null) {
            pendingCommand = null
            return
        }
        pendingCommand = next
        writeCommand(next)
        val timeout = Runnable { onCommandTimeout(next) }
        pendingTimeoutRunnable = timeout
        handler.postDelayed(timeout, BleConstants.COMMAND_TIMEOUT_MS)
    }

    private fun writeCommand(command: PendingCommand) {
        val frame = frameEncoder.encode(
            dialect,
            command.protocolCommand.command,
            command.protocolCommand.payload,
        )
        writeFrame(frame)
    }

    private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }

    private fun writeFrame(frame: ByteArray) {
        Log.d(TAG, "TX ch=$controlChannel dialect=$dialect ${frame.size}B ${frame.hex()}")
        when (controlChannel) {
            ControlChannel.BLE_GATT -> gattTransport?.write(frame)
            ControlChannel.RFCOMM -> rfcommTransport?.write(frame)
            ControlChannel.NONE -> Unit
        }
    }

    private fun onCommandTimeout(command: PendingCommand) {
        if (pendingCommand !== command) return
        pendingCommand = null
        cancelPendingTimeout()

        // 帧方言探测：OPO 完全无回包时切换 Telink 帧再试一次。
        if (command.probeDialect && receivedFrameCount == 0 && dialect == FrameDialect.OPO) {
            dialect = FrameDialect.TL
            Log.i(TAG, "no frames received, probing TL frame dialect")
            commandQueue.addFirst(command.copy(probeDialect = false))
            sendNextCommand()
            return
        }

        if (!command.silent && command.retries < BleConstants.COMMAND_MAX_RETRIES) {
            commandQueue.addFirst(command.copy(retries = command.retries + 1))
            sendNextCommand()
            return
        }

        if (!command.silent) _messages.tryEmit(R.string.msg_failed)
        sendNextCommand()
    }

    private fun cancelPendingTimeout() {
        val runnable = pendingTimeoutRunnable ?: return
        handler.removeCallbacks(runnable)
        pendingTimeoutRunnable = null
    }

    // ================================================================== 收包
    private fun onTransportData(data: ByteArray) {
        Log.d(TAG, "RX ch=$controlChannel ${data.size}B ${data.hex()}")
        if (closed) return
        assembler.feed(data) { frame ->
            receivedFrameCount++
            if (frame.dialect != dialect) {
                dialect = frame.dialect
                Log.i(TAG, "detected frame dialect ${frame.dialect}")
            }
            handleFrame(frame)
        }
    }

    private fun handleFrame(frame: ProtocolFrame) {
        val pending = pendingCommand
        if (pending != null && frame.command == pending.protocolCommand.expectedAck) {
            Log.d(TAG, "ACK cmd=0x%04X".format(pending.protocolCommand.command))
            pendingCommand = null
            cancelPendingTimeout()
            pending.onSuccess?.invoke()
            sendNextCommand()
        }
        protocol.parse(frame).forEach { applyEvent(it) }
    }

    private fun applyEvent(event: ProtocolEvent) {
        when (event) {
            is ProtocolEvent.Battery -> updateState { copy(battery = event.info) }
            is ProtocolEvent.Anc -> updateState {
                copy(
                    ancMode = event.mode,
                    ancLevel = event.level ?: ancLevel,
                    // 只有收到带等级的 ANC 状态才认为该型号支持降噪等级
                    capabilities = if (event.level != null) {
                        capabilities.copy(ancLevel = true)
                    } else {
                        capabilities
                    },
                )
            }

            is ProtocolEvent.Eq -> updateState {
                val resolved = eqPresetFromId(event.presetId)
                if (resolved != null) copy(eqPreset = resolved) else this
            }
            is ProtocolEvent.GameMode -> updateState { copy(gameMode = event.enabled) }
            is ProtocolEvent.DualConnection -> updateState {
                copy(
                    dualConnection = event.enabled,
                    capabilities = capabilities.copy(dualConnection = true),
                )
            }

            is ProtocolEvent.InEar -> updateState {
                copy(
                    inEarDetection = event.enabled,
                    // 只有真正收到「已佩戴」事件才认为该型号支持入耳检测，避免把 0x04 状态位误判为支持
                    capabilities = if (event.enabled) {
                        capabilities.copy(inEarDetection = true)
                    } else {
                        capabilities
                    },
                )
            }

            is ProtocolEvent.Gestures -> updateState {
                val merged = gestures.toMutableMap()
                event.gestures.forEach { (side, sideGestures) ->
                    merged[side] = merged[side].orEmpty() + sideGestures
                }
                copy(gestures = merged)
            }

            is ProtocolEvent.Firmware -> updateState { copy(firmware = event.version) }
        }
    }

    // ============================================================== 初始化序列
    private fun startInitSequence() {
        if (closed) return
        assembler.reset()
        receivedFrameCount = 0
        handler.removeCallbacks(connectTimeoutRunnable)
        if (BleConstants.BLE_HANDSHAKE_ENABLED && controlChannel == ControlChannel.BLE_GATT) {
            performBleHandshake()
        } else {
            enqueueInitCommands()
        }
    }

    /** BLE GATT 形态的 OPO v1 需要 HELLO → REGISTER 会话建立（realme RFCOMM 不需要）。 */
    private fun performBleHandshake() {
        val hello = protocol.hello()
        writeFrame(frameEncoder.encode(dialect, hello.command, hello.payload))
        handler.postDelayed({
            if (closed || controlChannel != ControlChannel.BLE_GATT) return@postDelayed
            if (dialect == FrameDialect.OPO) {
                val register = protocol.register()
                writeFrame(frameEncoder.encode(dialect, register.command, register.payload))
            }
            handler.postDelayed({
                if (!closed) enqueueInitCommands()
            }, BleConstants.BLE_HANDSHAKE_REGISTER_DELAY_MS)
        }, BleConstants.BLE_HANDSHAKE_HELLO_DELAY_MS)
    }

    private fun enqueueInitCommands() {
        if (closed) return
        sendCommand(protocol.batteryRequest(), silent = true, probeDialect = true)
        sendCommand(protocol.miscConfigRequest(), silent = true)
        sendCommand(protocol.statusRequest(), silent = true)
        sendCommand(protocol.ancModeRequest(), silent = true)
        sendCommand(protocol.ancLevelRequest(), silent = true)
        sendCommand(protocol.touchConfigRequest(), silent = true)
        sendCommand(protocol.subscriptionRequest(), silent = true)
        sendCommand(protocol.firmwareRequest(), silent = true)
        sendCommand(protocol.eqRequest(), silent = true)
    }

    // ================================================================ 控制命令
    override fun setAncMode(mode: AncMode) = runOnBleThread {
        sendCommand(protocol.ancSet(mode, _state.value.ancLevel), onSuccess = {
            updateState { copy(ancMode = mode) }
        })
    }

    override fun setAncLevel(level: AncLevel) = runOnBleThread {
        val current = _state.value
        if (current.ancMode == AncMode.NoiseCancellation) {
            sendCommand(protocol.ancSet(AncMode.NoiseCancellation, level), onSuccess = {
                updateState {
                    copy(ancMode = AncMode.NoiseCancellation, ancLevel = level)
                }
            })
        } else {
            // 未开启降噪时耳机不接受等级设置：只记录本地状态，开启降噪时随模式一起下发。
            updateState { copy(ancLevel = level) }
        }
    }

    override fun setEq(preset: EqPreset) = runOnBleThread {
        sendCommand(protocol.setEq(preset), onSuccess = {
            updateState { copy(eqPreset = preset) }
        })
    }

    override fun setGesture(side: EarSide, type: GestureType, action: GestureAction) = runOnBleThread {
        val command = protocol.setGesture(side, type, action)
        if (command == null) {
            // ToggleAnc 等动作在 realme 协议里没有确认取值（TODO 真机确认）。
            _messages.tryEmit(R.string.msg_failed)
            return@runOnBleThread
        }
        sendCommand(command, onSuccess = {
            updateState {
                copy(gestures = gestures + (side to (gestures[side].orEmpty() + (type to action))))
            }
        })
    }

    override fun setGameMode(enabled: Boolean) = runOnBleThread {
        sendCommand(protocol.setGameMode(enabled), onSuccess = {
            updateState { copy(gameMode = enabled) }
        })
    }

    override fun setDualConnection(enabled: Boolean) = runOnBleThread {
        sendCommand(protocol.setDualConnection(enabled), onSuccess = {
            updateState { copy(dualConnection = enabled) }
        })
    }

    override fun setInEarDetection(enabled: Boolean) = runOnBleThread {
        sendCommand(protocol.setInEarDetection(enabled), onSuccess = {
            updateState { copy(inEarDetection = enabled) }
        })
    }

    override fun findBuds() = runOnBleThread {
        sendCommand(protocol.findBuds(true), onSuccess = {
            _messages.tryEmit(R.string.msg_find_buds)
        })
    }

    override fun stopFindBuds() = runOnBleThread {
        sendCommand(protocol.findBuds(false), onSuccess = {
            _messages.tryEmit(R.string.msg_find_stopped)
        })
    }

    override fun factoryReset() = runOnBleThread {
        // 恢复出厂在 APK 中对应 setRestoreData，但 opcode 未确认且属于高风险操作；
        // 在拿到真机抓包前不下发，避免误触发。
        Log.w(TAG, "factoryReset: opcode not confirmed, refusing to send")
        _messages.tryEmit(R.string.msg_failed)
    }

    init {
        // 应用启动时自动回连上次成功连接过的耳机
        val device = initialDevice
        if (device != null) {
            Log.i(TAG, "auto-reconnect last device: ${device.name} ${device.address}")
            handler.postDelayed({
                if (!closed && _state.value.connection == ConnectionState.Disconnected) {
                    connectInternal(device, isReconnect = false)
                }
            }, BleConstants.AUTO_CONNECT_DELAY_MS)
        }
    }

    /** 断开后的重连退避：2s, 4s, 8s, 16s, 之后固定 30s。 */
    private fun reconnectDelayMs(attempt: Int): Long {
        val base = BleConstants.RECONNECT_DELAY_MS shl (attempt - 1).coerceIn(0, 5)
        return base.coerceAtMost(BleConstants.MAX_RECONNECT_DELAY_MS)
    }

    // ================================================================ 生命周期
    override fun close() {
        if (closed) return
        closed = true
        handler.post {
            handler.removeCallbacks(connectTimeoutRunnable)
            handler.removeCallbacks(reconnectRunnable)
            cancelPendingTimeout()
            scanner.close()
            closeTransports()
            commandQueue.clear()
            pendingCommand = null
            handlerThread.quitSafely()
        }
    }

    // ================================================================ 工具方法
    private fun runOnBleThread(block: () -> Unit) {
        if (closed) return
        handler.post {
            if (!closed) block()
        }
    }

    private fun updateState(transform: BudsState.() -> BudsState) {
        _state.value = _state.value.transform()
    }

    private fun hasScanPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasConnectPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun isAdapterReady(bluetoothAdapter: BluetoothAdapter): Boolean = try {
        bluetoothAdapter.isEnabled && bluetoothAdapter.bluetoothLeScanner != null
    } catch (e: SecurityException) {
        false
    }

    private data class PendingCommand(
        val protocolCommand: ProtocolCommand,
        val onSuccess: (() -> Unit)? = null,
        val silent: Boolean = false,
        val retries: Int = 0,
        val probeDialect: Boolean = false,
    )

    private enum class ControlChannel {
        NONE,
        BLE_GATT,
        RFCOMM,
    }

    private companion object {
        const val TAG = "BleBudsRepository"
    }
}

