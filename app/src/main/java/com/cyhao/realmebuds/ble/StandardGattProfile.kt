package com.cyhao.realmebuds.ble

/**
 * 标准 GATT 特征解析（BAS/DIS）。
 *
 * 私有协议不可用时的兜底：至少展示整机电量、型号、固件版本。
 */
internal object StandardGattProfile {

    /** BAS Battery Level：单字节 0..100。 */
    fun parseBatteryLevel(value: ByteArray): Int? {
        if (value.isEmpty()) return null
        val level = value[0].toInt() and 0xFF
        return level.takeIf { it in 0..100 }
    }

    /** DIS 字符串特征：UTF-8/ASCII，通常带 0x00 结尾。 */
    fun parseText(value: ByteArray): String =
        String(value, Charsets.UTF_8).trim('\u0000').trim()
}
