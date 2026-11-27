package com.cyhao.realmebuds.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import com.cyhao.realmebuds.data.ScannedDevice

/**
 * BLE 扫描器：发现耳机/真我系设备、去重并实时刷新 RSSI，15 秒自动停止。
 *
 * 线程模型：所有内部状态只在仓库的 Handler 线程上访问；系统回调 post 到该线程。
 */
internal class BleScanner(
    private val appContext: Context,
    private val adapter: BluetoothAdapter?,
    private val handler: Handler,
    private val onResults: (List<ScannedDevice>) -> Unit,
    private val onScanningChanged: (Boolean) -> Unit,
    private val onFailed: () -> Unit,
    private val onFinished: () -> Unit,
) {

    private val results = LinkedHashMap<String, ScannedDevice>()
    private var callback: ScanCallback? = null
    private var scanning = false
    private var closed = false

    private val timeoutRunnable = Runnable { stopInternal(auto = true) }

    fun hasScanPermission(): Boolean {
        // Android 12+ 用蓝牙扫描权限；Android 11 及以下扫描 BLE 需要定位权限
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /** @return false 表示蓝牙未就绪/未授权/无法开始扫描。 */
    fun start(): Boolean {
        if (closed) return false
        val bluetoothAdapter = adapter ?: return false
        if (!hasScanPermission()) return false
        if (!isReady(bluetoothAdapter)) return false

        if (scanning) stopInternal(auto = false)

        val leScanner = try {
            bluetoothAdapter.bluetoothLeScanner
        } catch (e: SecurityException) {
            Log.w(TAG, "bluetoothLeScanner denied", e)
            null
        } ?: return false

        results.clear()
        onResults(emptyList())

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (closed) return
                handler.post { handleResult(result) }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                if (closed) return
                handler.post { results.forEach { handleResult(it) } }
            }

            override fun onScanFailed(errorCode: Int) {
                if (closed) return
                handler.post {
                    Log.w(TAG, "scan failed: $errorCode")
                    if (scanning) {
                        stopInternal(auto = false)
                        onFailed()
                    }
                }
            }
        }
        callback = scanCallback
        scanning = true
        onScanningChanged(true)

        return try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            leScanner.startScan(null, settings, scanCallback)
            handler.postDelayed(timeoutRunnable, BleConstants.SCAN_TIMEOUT_MS)
            true
        } catch (e: Exception) {
            Log.w(TAG, "startScan failed", e)
            callback = null
            scanning = false
            onScanningChanged(false)
            false
        }
    }

    fun stop() {
        stopInternal(auto = false)
    }

    fun close() {
        closed = true
        stopInternal(auto = false)
        handler.removeCallbacks(timeoutRunnable)
    }

    private fun stopInternal(auto: Boolean) {
        handler.removeCallbacks(timeoutRunnable)
        val scanCallback = callback
        callback = null
        val wasScanning = scanning
        scanning = false
        if (scanCallback != null) {
            try {
                adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "stopScan failed", e)
            }
        }
        if (wasScanning) {
            onScanningChanged(false)
            if (auto) onFinished()
        }
    }

    private fun handleResult(result: ScanResult) {
        if (!scanning || closed) return
        val record = result.scanRecord
        val device = result.device
        val address = try {
            device.address
        } catch (e: SecurityException) {
            return
        }
        val name = record?.deviceName?.takeIf { it.isNotBlank() } ?: deviceName(device)
        if (name.isNullOrBlank()) return

        val vendorHit = DeviceMatcher.hasVendorAdvertisement(record)
        if (!DeviceMatcher.isHeadphoneName(name) && !vendorHit) return

        val realme = DeviceMatcher.isRealme(name, record)
        val parsed = ScannedDevice(
            name = name,
            address = address,
            rssi = result.rssi,
            realme = realme,
        )
        val previous = results[parsed.address]
        if (previous != null &&
            previous.name == parsed.name &&
            previous.rssi == parsed.rssi &&
            previous.realme == parsed.realme
        ) {
            return
        }
        results[parsed.address] = parsed
        onResults(sortedResults())
    }

    private fun sortedResults(): List<ScannedDevice> =
        results.values.sortedWith(
            compareByDescending<ScannedDevice> { it.realme }.thenByDescending { it.rssi },
        )

    @SuppressLint("MissingPermission")
    private fun deviceName(device: BluetoothDevice): String? = try {
        device.name
    } catch (e: SecurityException) {
        null
    }

    private fun isReady(bluetoothAdapter: BluetoothAdapter): Boolean = try {
        bluetoothAdapter.isEnabled && bluetoothAdapter.bluetoothLeScanner != null
    } catch (e: SecurityException) {
        false
    }

    private companion object {
        const val TAG = "BleScanner"
    }
}

/** 设备识别：名称关键字 / 厂商广播 / 128 位自定义 UUID。 */
internal object DeviceMatcher {

    fun isHeadphoneName(name: String): Boolean =
        BleConstants.HEADPHONE_NAME_MARKERS.any { name.contains(it, ignoreCase = true) }

    fun isRealme(name: String, record: android.bluetooth.le.ScanRecord?): Boolean {
        if (BleConstants.REALME_NAME_MARKERS.any { name.contains(it, ignoreCase = true) }) return true
        return hasVendorAdvertisement(record)
    }

    fun hasVendorAdvertisement(record: android.bluetooth.le.ScanRecord?): Boolean {
        record ?: return false
        record.serviceUuids?.forEach { parcelUuid ->
            if (BleConstants.VENDOR_ADVERT_SERVICE_UUIDS.any { it == parcelUuid.uuid }) {
                return true
            }
        }
        val manufacturerData = record.manufacturerSpecificData ?: return false
        for (index in 0 until manufacturerData.size()) {
            val payload = manufacturerData.valueAt(index) ?: continue
            if (containsMarker(payload)) return true
        }
        return false
    }

    private fun containsMarker(payload: ByteArray): Boolean {
        if (payload.isEmpty()) return false
        val text = String(payload, Charsets.ISO_8859_1)
        return BleConstants.MANUFACTURER_ASCII_MARKERS.any { text.contains(it, ignoreCase = true) }
    }
}

