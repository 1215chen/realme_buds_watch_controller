package com.cyhao.realmebuds.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 触觉反馈工具（兼容 Wear OS 与 ColorOS Watch）。
 *
 * Wear OS 上 `LocalHapticFeedback` 可用，但 ColorOS Watch 等非 Wear 系统上可能不生效，
 * 因此统一走系统 Vibrator（API 26+ 用 VibrationEffect，31+ 走 VibratorManager）。
 */
object Haptics {

    /**
     * Wear OS 4+（API 33+）继续使用系统 HapticFeedback，保持原生手感；
     * 仅 ColorOS Watch / 旧系统用 Vibrator 兜底。
     */
    val isSystemHapticsPlatform: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** 轻触：列表选中、开关切换。 */
    fun tick(context: Context) = vibrate(context, durationMs = 12L, amplitude = 110)

    /** 确认：连接成功、降噪切换完成。 */
    fun confirm(context: Context) = vibrate(context, durationMs = 22L, amplitude = 170)

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            return manager?.defaultVibrator
        }
        @Suppress("DEPRECATION")
        return context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
