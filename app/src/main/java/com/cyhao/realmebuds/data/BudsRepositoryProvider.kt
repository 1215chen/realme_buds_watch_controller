package com.cyhao.realmebuds.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 轻量本地设置存储：演示模式开关 + 上次连接的耳机（用于自动回连）。
 */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("buds_wear_settings", Context.MODE_PRIVATE)

    var demoMode: Boolean
        get() = prefs.getBoolean(KEY_DEMO_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DEMO_MODE, value).apply()

    /** 上次成功连接的设备；应用重启后据此自动回连。 */
    val savedDevice: ScannedDevice?
        get() {
            val address = prefs.getString(KEY_DEVICE_ADDRESS, null) ?: return null
            return ScannedDevice(
                name = prefs.getString(KEY_DEVICE_NAME, "").orEmpty(),
                address = address,
                rssi = 0,
                realme = prefs.getBoolean(KEY_DEVICE_REALME, false),
            )
        }

    fun saveDevice(device: ScannedDevice) {
        prefs.edit()
            .putString(KEY_DEVICE_ADDRESS, device.address)
            .putString(KEY_DEVICE_NAME, device.name)
            .putBoolean(KEY_DEVICE_REALME, device.realme)
            .apply()
    }

    fun clearDevice() {
        prefs.edit()
            .remove(KEY_DEVICE_ADDRESS)
            .remove(KEY_DEVICE_NAME)
            .remove(KEY_DEVICE_REALME)
            .apply()
    }

    companion object {
        private const val KEY_DEMO_MODE = "demo_mode"
        private const val KEY_DEVICE_ADDRESS = "last_device_address"
        private const val KEY_DEVICE_NAME = "last_device_name"
        private const val KEY_DEVICE_REALME = "last_device_realme"
    }
}

/** 仓库工厂：演示模式走 [DemoBudsRepository]，否则走真实 BLE 实现（并自动回连上次设备）。 */
object BudsRepositoryProvider {
    fun create(context: Context, demoMode: Boolean): BudsRepository =
        if (demoMode) {
            DemoBudsRepository()
        } else {
            val appContext = context.applicationContext
            val savedDevice = AppSettings(appContext).savedDevice
            com.cyhao.realmebuds.ble.BleBudsRepository(appContext, savedDevice)
        }
}
