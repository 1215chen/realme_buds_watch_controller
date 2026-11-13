package com.cyhao.realmebuds.data

import com.cyhao.realmebuds.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 演示模式实现：不访问蓝牙，用延时模拟真实交互，方便在无耳机/未授权时完整体验 UI。
 * 演示设备的型号能力按「支持全部功能」给出。
 */
class DemoBudsRepository : BudsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val demoCapabilities = DeviceCapabilities(
        known = true,
        inEarDetection = true,
        dualConnection = true,
        ancLevel = true,
        gameMode = true,
        findBuds = true,
    )

    private val _state = MutableStateFlow(
        BudsState(
            connection = ConnectionState.Connected,
            name = "realme Buds Air 6 Pro",
            model = "realme Buds Air 6 Pro",
            firmware = "1.2.3.4",
            address = "A4:CF:12:00:00:01",
            battery = BatteryInfo(left = 82, right = 78, case = 91),
            capabilities = demoCapabilities,
            isRealme = true,
            demo = true,
        )
    )
    override val state: StateFlow<BudsState> = _state.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val scanResults: StateFlow<List<ScannedDevice>> = _scanResults.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    override val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    override val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    override fun startScan() {
        if (_scanning.value) return
        scanJob?.cancel()
        scanJob = scope.launch {
            _scanning.value = true
            _scanResults.value = emptyList()
            delay(700)
            _scanResults.value = listOf(
                ScannedDevice("realme Buds Air 6 Pro", "A4:CF:12:00:00:01", -42, realme = true),
                ScannedDevice("realme Buds T300", "A4:CF:12:00:00:02", -67, realme = true),
                ScannedDevice("Buds-Air-6", "70:3A:CB:11:22:33", -83, realme = false),
            )
            delay(450)
            _scanning.value = false
            _messages.emit(R.string.msg_scan_finished)
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _scanning.value = false
    }

    override fun connect(device: ScannedDevice) {
        connectJob?.cancel()
        connectJob = scope.launch {
            _state.value = _state.value.copy(
                connection = ConnectionState.Connecting,
                name = device.name,
                address = device.address,
                isRealme = device.realme,
                demo = true,
            )
            delay(1200)
            _state.value = _state.value.copy(
                connection = ConnectionState.Connected,
                model = if (device.realme) device.name else "未知设备",
                firmware = if (device.realme) "1.2.3.4" else "—",
                battery = BatteryInfo(left = 82, right = 78, case = 91),
                capabilities = if (device.realme) demoCapabilities else DeviceCapabilities(),
            )
            _messages.emit(R.string.msg_connected)
        }
    }

    override fun disconnect() {
        connectJob?.cancel()
        _state.value = _state.value.copy(
            connection = ConnectionState.Disconnected,
            battery = BatteryInfo(),
        )
        _messages.tryEmit(R.string.msg_disconnected)
    }

    override fun setAncMode(mode: AncMode) = updateState {
        copy(ancMode = mode)
    }

    override fun setAncLevel(level: AncLevel) = updateState {
        copy(ancLevel = level)
    }

    override fun setEq(preset: EqPreset) = updateState {
        copy(eqPreset = preset)
    }

    override fun setGesture(side: EarSide, type: GestureType, action: GestureAction) =
        updateState {
            val sideGestures = gestures[side].orEmpty() + (type to action)
            copy(gestures = gestures + (side to sideGestures))
        }

    override fun setGameMode(enabled: Boolean) = updateState {
        copy(gameMode = enabled)
    }

    override fun setDualConnection(enabled: Boolean) = updateState {
        copy(dualConnection = enabled)
    }

    override fun setInEarDetection(enabled: Boolean) = updateState {
        copy(inEarDetection = enabled)
    }

    override fun findBuds() {
        _messages.tryEmit(R.string.msg_find_buds)
    }

    override fun stopFindBuds() {
        _messages.tryEmit(R.string.msg_find_stopped)
    }

    override fun factoryReset() {
        _state.value = _state.value.copy(
            ancMode = AncMode.Off,
            ancLevel = AncLevel.Smart,
            eqPreset = EqPreset.Original,
            gestures = defaultGestures(),
            gameMode = false,
            dualConnection = false,
            inEarDetection = true,
        )
        _messages.tryEmit(R.string.msg_reset_done)
    }

    private fun updateState(transform: BudsState.() -> BudsState) {
        scope.launch {
            delay(180)
            _state.value = _state.value.transform()
        }
    }

    override fun close() {
        scope.cancel()
    }
}
