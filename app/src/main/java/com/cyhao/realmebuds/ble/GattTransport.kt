package com.cyhao.realmebuds.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.UUID

/**
 * BLE GATT 传输层：
 * 连接 → 服务发现 → 请求 MTU → 订阅通知 → 读取 BAS/DIS → 就绪回调。
 *
 * 私有控制通道选择策略：
 * 1. 先找 OPO 服务（[BleConstants.OPO_SERVICE]）中的写/通知特征；
 * 2. 找不到时退化为「非标准服务里同时具备可写 + 可通知特征」的通用通道
 *    （realme Link 的私有 UUID 可能随型号配置下发，静态分析未拿到字面量）。
 *
 * 所有回调都 post 到仓库的 Handler 线程。
 */
internal class GattTransport(
    private val context: Context,
    private val adapter: BluetoothAdapter,
    private val handler: Handler,
    private val address: String,
    private val callbacks: Callbacks,
) {

    internal interface Callbacks {
        fun onGattReady(opoChannelAvailable: Boolean)
        fun onGattData(data: ByteArray)
        fun onGattBatteryLevel(level: Int)
        fun onGattModel(model: String)
        fun onGattFirmware(firmware: String)
        fun onGattDisconnected()
        fun onGattFailed()
    }

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private val notifyCharacteristics = mutableListOf<BluetoothGattCharacteristic>()
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    private var modelCharacteristic: BluetoothGattCharacteristic? = null
    private var firmwareCharacteristic: BluetoothGattCharacteristic? = null
    private val readQueue = ArrayDeque<BluetoothGattCharacteristic>()

    private var mtu: Int = 23
    private var ready = false
    private var closed = false
    private var setupFinished = false
    private var opoChannelAvailable = false

    private val connectTimeoutRunnable = Runnable {
        if (!ready && !closed) {
            Log.w(TAG, "GATT connect timeout")
            callbacks.onGattFailed()
            close()
        }
    }

    private val setupRunnable = Runnable { beginSetup() }

    private val readyRunnable = Runnable { finishSetup() }

    @SuppressLint("MissingPermission")
    fun start() {
        if (closed) return
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "invalid address $address", e)
            callbacks.onGattFailed()
            return
        }
        handler.postDelayed(connectTimeoutRunnable, BleConstants.CONNECT_TIMEOUT_MS)
        try {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.w(TAG, "connectGatt failed", e)
            gatt = null
        }
        if (gatt == null) {
            handler.removeCallbacks(connectTimeoutRunnable)
            callbacks.onGattFailed()
        }
    }

    fun write(bytes: ByteArray) {
        if (closed || !ready) return
        val characteristic = writeCharacteristic ?: return
        val activeGatt = gatt ?: return
        if (bytes.isEmpty()) return
        val chunkSize = (mtu - 3).coerceAtLeast(20)
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + length)
            try {
                val status = activeGatt.writeCharacteristic(
                    characteristic,
                    chunk,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                )
                if (status != BluetoothStatusCodes.SUCCESS) {
                    Log.w(TAG, "writeCharacteristic status=$status")
                }
            } catch (e: Exception) {
                Log.w(TAG, "writeCharacteristic failed", e)
            }
            offset += length
        }
    }

    fun close() {
        if (closed) return
        closed = true
        ready = false
        handler.removeCallbacks(connectTimeoutRunnable)
        handler.removeCallbacks(setupRunnable)
        handler.removeCallbacks(readyRunnable)
        readQueue.clear()
        notifyCharacteristics.clear()
        writeCharacteristic = null
        batteryCharacteristic = null
        modelCharacteristic = null
        firmwareCharacteristic = null
        val activeGatt = gatt
        gatt = null
        try {
            activeGatt?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "gatt.disconnect failed", e)
        }
        try {
            activeGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "gatt.close failed", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            handler.post {
                if (closed) return@post
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    val started = try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        false
                    }
                    if (!started) {
                        Log.w(TAG, "discoverServices failed to start")
                        callbacks.onGattFailed()
                        close()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    handler.removeCallbacks(connectTimeoutRunnable)
                    ready = false
                    callbacks.onGattDisconnected()
                    close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            handler.post {
                if (closed) return@post
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    handleServices(gatt)
                } else {
                    Log.w(TAG, "onServicesDiscovered status=$status")
                    callbacks.onGattFailed()
                    close()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            handler.post {
                if (closed) return@post
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this@GattTransport.mtu = mtu
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handler.post {
                if (closed) return@post
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (characteristic.uuid) {
                        BleConstants.CHAR_BATTERY_LEVEL ->
                            StandardGattProfile.parseBatteryLevel(value)?.let(callbacks::onGattBatteryLevel)

                        BleConstants.CHAR_MODEL_NUMBER ->
                            StandardGattProfile.parseText(value)
                                .takeIf { it.isNotBlank() }
                                ?.let(callbacks::onGattModel)

                        BleConstants.CHAR_FIRMWARE_REVISION ->
                            StandardGattProfile.parseText(value)
                                .takeIf { it.isNotBlank() }
                                ?.let(callbacks::onGattFirmware)

                        BleConstants.CHAR_MANUFACTURER_NAME -> Unit
                    }
                }
                readNext()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handler.post {
                if (closed) return@post
                if (value.isNotEmpty()) callbacks.onGattData(value)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            handler.post {
                if (!closed && status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "CCCD write status=$status ${descriptor.uuid}")
                }
            }
        }
    }

    private fun handleServices(activeGatt: BluetoothGatt) {
        val services = activeGatt.services.orEmpty()
        val opoService: BluetoothGattService? =
            services.firstOrNull { it.uuid == BleConstants.OPO_SERVICE }

        var writeCharacteristic = opoService?.getCharacteristic(BleConstants.OPO_WRITE_CHARACTERISTIC)
        val notifiable = mutableListOf<BluetoothGattCharacteristic>()
        opoService?.characteristics?.forEach { characteristic ->
            if (characteristic.isNotifiable) notifiable.add(characteristic)
        }
        services.firstOrNull { it.uuid == BleConstants.FAST_PAIR_SERVICE }
            ?.characteristics
            ?.forEach { characteristic ->
                if (characteristic.uuid == BleConstants.FE2C_TELEMETRY_CHARACTERISTIC &&
                    characteristic.isNotifiable
                ) {
                    notifiable.add(characteristic)
                }
            }

        if (writeCharacteristic == null || notifiable.isEmpty()) {
            // 兜底：非标准服务中同时具备「可写」与「可通知」特征的通用私有通道。
            val genericService = services.firstOrNull { service ->
                service.uuid !in STANDARD_SERVICES_UUIDS &&
                    service.characteristics.any { it.isWritable } &&
                    service.characteristics.any { it.isNotifiable }
            }
            if (genericService != null) {
                writeCharacteristic = genericService.characteristics.firstOrNull { it.isWritable }
                notifiable.clear()
                genericService.characteristics.forEach { characteristic ->
                    if (characteristic.isNotifiable) notifiable.add(characteristic)
                }
                Log.i(TAG, "using generic private GATT channel service=${genericService.uuid}")
            }
        }

        this.writeCharacteristic = writeCharacteristic
        notifyCharacteristics.clear()
        notifyCharacteristics.addAll(notifiable)
        opoChannelAvailable = writeCharacteristic != null && notifiable.isNotEmpty()

        val standard = services
        batteryCharacteristic = standard.firstOrNull { it.uuid == BleConstants.SERVICE_BATTERY }
            ?.getCharacteristic(BleConstants.CHAR_BATTERY_LEVEL)
        val deviceInfo = standard.firstOrNull { it.uuid == BleConstants.SERVICE_DEVICE_INFO }
        modelCharacteristic = deviceInfo?.getCharacteristic(BleConstants.CHAR_MODEL_NUMBER)
        firmwareCharacteristic = deviceInfo?.getCharacteristic(BleConstants.CHAR_FIRMWARE_REVISION)

        try {
            activeGatt.requestMtu(BleConstants.MTU_TARGET)
        } catch (e: Exception) {
            Log.w(TAG, "requestMtu failed", e)
        }
        handler.postDelayed(setupRunnable, BleConstants.GATT_SETUP_DELAY_MS)
    }

    private fun beginSetup() {
        if (closed) return
        val activeGatt = gatt ?: return
        notifyCharacteristics.forEach { characteristic ->
            try {
                activeGatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(BleConstants.DESCRIPTOR_CCCD)
                if (descriptor != null) {
                    val status = activeGatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                    )
                    if (status != BluetoothStatusCodes.SUCCESS) {
                        Log.w(TAG, "writeDescriptor status=$status")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "enable notification failed", e)
            }
        }

        readQueue.clear()
        batteryCharacteristic?.let(readQueue::addLast)
        modelCharacteristic?.let(readQueue::addLast)
        firmwareCharacteristic?.let(readQueue::addLast)
        readNext()

        handler.removeCallbacks(readyRunnable)
        handler.postDelayed(readyRunnable, BleConstants.GATT_READY_FALLBACK_MS)
    }

    private fun readNext() {
        if (closed) return
        val activeGatt = gatt ?: return
        val characteristic = readQueue.removeFirstOrNull() ?: return
        try {
            activeGatt.readCharacteristic(characteristic)
        } catch (e: Exception) {
            Log.w(TAG, "readCharacteristic failed", e)
        }
    }

    private fun finishSetup() {
        if (closed || setupFinished) return
        setupFinished = true
        handler.removeCallbacks(connectTimeoutRunnable)
        ready = true
        callbacks.onGattReady(opoChannelAvailable)
    }

    private val BluetoothGattCharacteristic.isWritable: Boolean
        get() = (properties and (
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            )) != 0

    private val BluetoothGattCharacteristic.isNotifiable: Boolean
        get() = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

    private companion object {
        const val TAG = "GattTransport"

        val STANDARD_SERVICES_UUIDS: Set<UUID> = setOf(
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"), // Generic Access
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"), // Generic Attribute
            BleConstants.SERVICE_DEVICE_INFO,
            BleConstants.SERVICE_BATTERY,
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"), // Heart Rate
        )
    }
}
