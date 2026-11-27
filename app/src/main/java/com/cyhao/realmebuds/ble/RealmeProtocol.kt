package com.cyhao.realmebuds.ble

import com.cyhao.realmebuds.data.AncLevel
import com.cyhao.realmebuds.data.AncMode
import com.cyhao.realmebuds.data.BatteryInfo
import com.cyhao.realmebuds.data.EqPreset
import com.cyhao.realmebuds.data.EarSide
import com.cyhao.realmebuds.data.GestureAction
import com.cyhao.realmebuds.data.GestureType
import com.cyhao.realmebuds.data.protocolId

/**
 * realme / OPPO（BBK OPO v1）16 位命令字表。
 *
 * 命令字与载荷编码来源：
 * - 公开逆向资料 earFrame（realme Buds Air 7 Pro HCI 抓包，ANC/电量/EQ/游戏模式均已物理验证）；
 * - Gadgetbridge 及其 NTeditor fork（OPPO/realme OPlus 实现，含多点连接/入耳检测）；
 * - `docs/PROTOCOL.md`（realme Link APK：命令常量与传输方式）。
 *
 * 未确认/依赖型号的能力集中标注 TODO，避免把推测当事实。
 */
internal object OpoCommand {

    // 查询类（请求 → 响应）
    const val BATTERY_REQ: Int = 0x0106
    const val BATTERY_RET: Int = 0x8106
    const val STATUS_REQ: Int = 0x0109
    const val STATUS_RET: Int = 0x8109
    const val FIRMWARE_REQ: Int = 0x0105
    const val FIRMWARE_RET: Int = 0x8105
    const val TOUCH_CONFIG_REQ: Int = 0x0108
    const val TOUCH_CONFIG_RET: Int = 0x8108
    const val ANC_CONFIG_REQ: Int = 0x010C
    const val ANC_CONFIG_RET: Int = 0x810C
    const val MISC_CONFIG_REQ: Int = 0x010D
    const val MISC_CONFIG_RET: Int = 0x810D
    const val EQUALIZER_REQ: Int = 0x010F
    const val EQUALIZER_RET: Int = 0x810F

    // 设置类（请求 → ACK）
    const val TOUCH_CONFIG_SET: Int = 0x0401
    const val TOUCH_CONFIG_ACK: Int = 0x8401
    const val FIND_DEVICE_REQ: Int = 0x0400
    const val FIND_DEVICE_ACK: Int = 0x8400
    const val MISC_CONFIG_SET: Int = 0x0403
    const val MISC_CONFIG_ACK: Int = 0x8403
    const val ANC_CONFIG_SET: Int = 0x0404
    const val ANC_CONFIG_ACK: Int = 0x8404
    const val EQUALIZER_SET: Int = 0x0406
    const val EQUALIZER_ACK: Int = 0x8406

    // 订阅（事件推送）
    const val SUBSCRIPTION_SET: Int = 0x0205
    const val SUBSCRIPTION_ACK: Int = 0x8205
    const val SUBSCRIPTION_RET: Int = 0x0204

    // BLE GATT 形态的 OPO v1 会话建立（HELLO / REGISTER）
    const val HELLO_REQ: Int = 0x0100
    const val REGISTER_REQ: Int = 0x8500

    /** 设备对请求/设置的响应命令字：最高位置 1。 */
    fun ackFor(command: Int): Int = command or ProtocolFrame.FRAME_RESPONSE_FLAG
}

/** ANC 配置子类型（payload 前两字节，little-endian）。 */
internal object OpoAncType {
    const val MODE: Int = 0x0101
    const val DYNAMIC_LEVEL: Int = 0x0104
}

/** 降噪模式/等级取值（realme 系；OPPO/OnePlus 部分机型映射不同，见 docs）。 */
internal object OpoAncValue {
    const val OFF: Int = 0x01
    const val TRANSPARENCY: Int = 0x02
    const val LEVEL_LOW: Int = 0x04
    const val LEVEL_HIGH: Int = 0x08
    const val LEVEL_NORMAL: Int = 0x10
    const val LEVEL_DYNAMIC: Int = 0x20
}

/** MISC 配置子类型。 */
internal object OpoMiscType {
    const val IN_EAR_DETECTION: Int = 0x04
    const val GAME_MODE: Int = 0x06
    const val MULTIPOINT: Int = 0x11
}

/** 通知订阅类型（订阅事件帧 payload[0]）。 */
internal object OpoSubscriptionType {
    const val BATTERY: Int = 0x01
    const val STATUS: Int = 0x02
    const val ANC_MODE: Int = 0x03
    const val GAME_MODE: Int = 0x05
    const val MULTIPOINT: Int = 0x06
}

/** 触控配置子类型（payload 第 3、4 字节，little-endian）。 */
internal object OpoTouchType {
    const val DOUBLE_TAP: Int = 0x0201
    const val TRIPLE_TAP: Int = 0x0301
    const val LONG_PRESS: Int = 0x0401
}

/** 触控动作取值（realme 系）。ToggleAnc 暂无确认值。 */
internal object OpoTouchAction {
    const val NONE: Int = 0x00
    const val PLAY_PAUSE: Int = 0x01
    const val VOICE_ASSISTANT: Int = 0x04
    const val PREVIOUS: Int = 0x05
    const val NEXT: Int = 0x06
    const val VOLUME_UP: Int = 0x0B
    const val VOLUME_DOWN: Int = 0x0C
}

/** 一条待发送命令（未编码，发送时按当前方言组包）。 */
internal data class ProtocolCommand(
    val command: Int,
    val payload: ByteArray = FrameEncoder.EMPTY,
    val expectedAck: Int = OpoCommand.ackFor(command),
)

/** 协议层解析出的事件，由仓库映射到 [com.cyhao.realmebuds.data.BudsState]。 */
internal sealed interface ProtocolEvent {
    data class Battery(val info: BatteryInfo) : ProtocolEvent
    data class Anc(val mode: AncMode, val level: AncLevel?) : ProtocolEvent
    data class Eq(val presetId: Int) : ProtocolEvent
    data class GameMode(val enabled: Boolean) : ProtocolEvent
    data class DualConnection(val enabled: Boolean) : ProtocolEvent
    data class InEar(val enabled: Boolean) : ProtocolEvent
    data class Gestures(val gestures: Map<EarSide, Map<GestureType, GestureAction>>) : ProtocolEvent
    data class Firmware(val version: String) : ProtocolEvent
}

/**
 * realme 协议层：命令构造 + 通知/响应解析。纯函数，不持有 Android 对象。
 */
internal class RealmeProtocol(private val token: ByteArray = byteArrayOf()) {

    // ---------------------------------------------------------------- 查询命令
    fun batteryRequest(): ProtocolCommand = queryCommand(RealmeFeature.Battery)

    fun firmwareRequest(): ProtocolCommand = queryCommand(RealmeFeature.Firmware)

    fun statusRequest(): ProtocolCommand = queryCommand(RealmeFeature.Status)

    fun eqRequest(): ProtocolCommand = queryCommand(RealmeFeature.Eq)

    fun ancModeRequest(): ProtocolCommand =
        queryCommand(RealmeFeature.AncMode, u16Le(OpoAncType.MODE))

    /** 读取降噪等级（仅部分型号支持；不支持时设备会忽略）。 */
    fun ancLevelRequest(): ProtocolCommand =
        queryCommand(RealmeFeature.AncLevel, u16Le(OpoAncType.DYNAMIC_LEVEL))

    fun touchConfigRequest(): ProtocolCommand =
        queryCommand(RealmeFeature.TouchConfig, byteArrayOf(0x02, 0x03, 0x01))

    /** 一次性读取游戏模式/双设备/入耳检测开关。 */
    fun miscConfigRequest(
        types: List<Int> = listOf(
            OpoMiscType.GAME_MODE,
            OpoMiscType.MULTIPOINT,
            OpoMiscType.IN_EAR_DETECTION,
        ),
    ): ProtocolCommand {
        val payload = ByteArray(1 + types.size)
        payload[0] = types.size.toByte()
        types.forEachIndexed { index, type -> payload[index + 1] = type.toByte() }
        return queryCommand(RealmeFeature.MiscConfig, payload)
    }

    /** 订阅电量/状态/降噪/游戏模式/双设备推送。 */
    fun subscriptionRequest(
        types: List<Int> = listOf(
            OpoSubscriptionType.BATTERY,
            OpoSubscriptionType.STATUS,
            OpoSubscriptionType.ANC_MODE,
            OpoSubscriptionType.GAME_MODE,
            OpoSubscriptionType.MULTIPOINT,
        ),
    ): ProtocolCommand {
        val payload = ByteArray(1 + types.size)
        payload[0] = 0x09
        types.forEachIndexed { index, type -> payload[index + 1] = type.toByte() }
        return setCommand(RealmeFeature.Subscription, payload)
    }

    // ---------------------------------------------------------------- 设置命令
    /**
     * 设置降噪。
     *
     * realme：payload = `01 01 <mode>`；mode = OFF 0x01 / TRANSPARENCY 0x02 /
     * 降噪等级 0x04(轻) 0x08(深) 0x10(中) 0x20(智能)。
     */
    fun ancSet(mode: AncMode, level: AncLevel): ProtocolCommand {
        val value = when (mode) {
            AncMode.Off -> OpoAncValue.OFF
            AncMode.Transparency -> OpoAncValue.TRANSPARENCY
            AncMode.NoiseCancellation -> ancLevelCode(level)
        }
        return setCommand(
            RealmeFeature.AncMode,
            byteArrayOf(0x01, 0x01, value.toByte()),
        )
    }

    fun setEq(preset: EqPreset): ProtocolCommand =
        setCommand(RealmeFeature.Eq, byteArrayOf(preset.protocolId.toByte()))

    /**
     * 设置触控手势：左右耳分别下发。
     *
     * payload = `01 <side> <typeLE> <action>`；side 左 0x01/右 0x02。
     * 返回 null 表示该动作在协议层没有确认取值（如 ToggleAnc）。
     */
    fun setGesture(side: EarSide, type: GestureType, action: GestureAction): ProtocolCommand? {
        val actionCode = actionCode(action) ?: return null
        val typeCode = touchTypeCode(type)
        return setCommand(
            RealmeFeature.TouchConfig,
            byteArrayOf(
                0x01,
                touchSideCode(side).toByte(),
                (typeCode and 0xFF).toByte(),
                ((typeCode ushr 8) and 0xFF).toByte(),
                actionCode.toByte(),
            ),
        )
    }

    fun setGameMode(enabled: Boolean): ProtocolCommand = miscSet(OpoMiscType.GAME_MODE, enabled)

    fun setDualConnection(enabled: Boolean): ProtocolCommand =
        miscSet(OpoMiscType.MULTIPOINT, enabled)

    fun setInEarDetection(enabled: Boolean): ProtocolCommand =
        miscSet(OpoMiscType.IN_EAR_DETECTION, enabled)

    fun findBuds(start: Boolean = true): ProtocolCommand =
        setCommand(
            RealmeFeature.FindBuds,
            byteArrayOf(if (start) 0x01 else 0x00),
        )

    // --------------------------------------------------- BLE GATT 会话建立（OPO）
    fun hello(): ProtocolCommand = setCommand(RealmeFeature.Handshake)

    /**
     * REGISTER 是 HELLO 之后的第二条会话命令（0x8500），与 HELLO 不在同一映射条目，
     * 因此直接取 OpoCommand 常量（仅 BLE GATT 形态使用）。
     */
    fun register(): ProtocolCommand {
        val payload = ByteArray(1 + token.size)
        payload[0] = 0x00
        token.copyInto(payload, 1)
        return ProtocolCommand(OpoCommand.REGISTER_REQ, payload)
    }

    private fun miscSet(type: Int, enabled: Boolean): ProtocolCommand =
        setCommand(
            RealmeFeature.MiscConfig,
            byteArrayOf(type.toByte(), if (enabled) 0x01 else 0x00),
        )

    // ------------------------------------------------- 命令构造工具（映射表驱动）
    private fun queryCommand(
        feature: RealmeFeature,
        payload: ByteArray = FrameEncoder.EMPTY,
    ): ProtocolCommand {
        val command = requireNotNull(RealmeCommandMap.query(feature)) {
            "feature $feature has no query command"
        }
        return ProtocolCommand(command, payload)
    }

    private fun setCommand(
        feature: RealmeFeature,
        payload: ByteArray = FrameEncoder.EMPTY,
    ): ProtocolCommand {
        val command = requireNotNull(RealmeCommandMap.set(feature)) {
            "feature $feature has no set command"
        }
        val ack = RealmeCommandMap.setAck(feature) ?: OpoCommand.ackFor(command)
        return ProtocolCommand(command, payload, expectedAck = ack)
    }

    // ---------------------------------------------------------------- 解析入口
    fun parse(frame: ProtocolFrame): List<ProtocolEvent> = when (frame.command) {
        OpoCommand.BATTERY_RET -> parseBattery(frame.payload)
            ?.let { listOf(ProtocolEvent.Battery(it)) }
            ?: emptyList()

        OpoCommand.FIRMWARE_RET -> parseFirmware(frame.payload)
            ?.let { listOf(ProtocolEvent.Firmware(it)) }
            ?: emptyList()

        OpoCommand.EQUALIZER_RET -> parseEq(frame.payload)
            ?.let { listOf(ProtocolEvent.Eq(it)) }
            ?: emptyList()

        OpoCommand.ANC_CONFIG_RET -> parseAnc(frame.payload)

        OpoCommand.MISC_CONFIG_RET -> parseMisc(frame.payload)

        OpoCommand.TOUCH_CONFIG_RET -> parseTouch(frame.payload)

        OpoCommand.SUBSCRIPTION_RET -> parseSubscription(frame.payload)

        // 其余（ACK/未知）不产生状态事件；ACK 由仓库的命令队列按命令字匹配。
        else -> emptyList()
    }

    // ---------------------------------------------------------------- 电量解析
    /**
     * 电量：`status/type, count, (side, level)*`。
     * side 左 1/右 2/盒 3；level 低 7 位为百分比，bit7 为充电标志；
     * side 0xFF 或 case 0 表示该部位不可用。
     */
    private fun parseBattery(payload: ByteArray): BatteryInfo? {
        if (payload.size < 2) return null
        val count = payload[1].toInt() and 0xFF
        var left: Int? = null
        var right: Int? = null
        var case: Int? = null
        var charging = false

        var index = 2
        var parsed = 0
        val max = if (count == 0) (payload.size - 2) / 2 else count
        while (index + 1 < payload.size && parsed < max) {
            val side = payload[index].toInt() and 0xFF
            val raw = payload[index + 1].toInt() and 0xFF
            val level = raw and 0x7F
            val isCharging = (raw and 0x80) != 0
            when (side) {
                0x01 -> if (level in 0..100) {
                    left = level
                    if (isCharging) charging = true
                }

                0x02 -> if (level in 0..100) {
                    right = level
                    if (isCharging) charging = true
                }

                0x03 -> if (level in 1..100) {
                    case = level
                    if (isCharging) charging = true
                }

                else -> Unit // 0xFF：该部位不可用（如单耳取出）
            }
            index += 2
            parsed++
        }
        if (left == null && right == null && case == null) return null
        return BatteryInfo(left = left, right = right, case = case, charging = charging)
    }

    // ---------------------------------------------------------------- 固件解析
    /**
     * 固件：`status, "1,2,<ver>,2,2,<ver>,..."`（逗号三元组，type=2 表示固件）。
     * realme 的版本字符串自带点号时会重复多次，取第一个即可。
     */
    private fun parseFirmware(payload: ByteArray): String? {
        if (payload.size < 3) return null
        val raw = String(payload, 2, payload.size - 2, Charsets.UTF_8)
            .trim('\u0000')
            .trim()
        if (raw.isBlank()) return null

        val parts = raw.split(",")
        if (parts.size % 3 != 0) return raw

        val versions = arrayOfNulls<String>(3)
        var i = 0
        while (i + 2 < parts.size) {
            val part = parts[i].trim()
            val type = parts[i + 1].trim()
            val version = parts[i + 2].trim()
            if (type == "2" && version.isNotBlank()) {
                when (part) {
                    "1" -> versions[0] = version
                    "2" -> versions[1] = version
                    "3" -> versions[2] = version
                }
            }
            i += 3
        }
        val collected = mutableListOf<String>()
        for (version in versions) {
            if (version.isNullOrBlank()) continue
            collected.add(version)
            if (version.contains('.')) break
        }
        return if (collected.isEmpty()) raw else collected.joinToString(".")
    }

    // -------------------------------------------------------------- 降噪解析
    private fun parseAnc(payload: ByteArray): List<ProtocolEvent> {
        if (payload.isEmpty()) return emptyList()
        val value = when {
            payload.size >= 4 -> {
                val type = (payload[1].toInt() and 0xFF) or ((payload[2].toInt() and 0xFF) shl 8)
                if (type == OpoAncType.MODE || type == OpoAncType.DYNAMIC_LEVEL) {
                    payload[3].toInt() and 0xFF
                } else {
                    payload[payload.size - 1].toInt() and 0xFF
                }
            }

            payload.size == 3 && (payload[1].toInt() and 0xFF) == 0x01 ->
                payload[2].toInt() and 0xFF

            else -> payload[payload.size - 1].toInt() and 0xFF
        }
        return when (value) {
            OpoAncValue.OFF -> listOf(ProtocolEvent.Anc(AncMode.Off, null))
            OpoAncValue.TRANSPARENCY -> listOf(ProtocolEvent.Anc(AncMode.Transparency, null))
            OpoAncValue.LEVEL_LOW -> ancEvent(AncLevel.Light)
            OpoAncValue.LEVEL_HIGH -> ancEvent(AncLevel.Deep)
            OpoAncValue.LEVEL_NORMAL -> ancEvent(AncLevel.Medium)
            OpoAncValue.LEVEL_DYNAMIC -> ancEvent(AncLevel.Smart)
            else -> emptyList()
        }
    }

    private fun ancEvent(level: AncLevel): List<ProtocolEvent> =
        listOf(ProtocolEvent.Anc(AncMode.NoiseCancellation, level))

    // ---------------------------------------------------------------- EQ 解析
    private fun parseEq(payload: ByteArray): Int? {
        if (payload.size < 2) return null
        return payload[1].toInt() and 0xFF
    }

    // ------------------------------------------------------------ MISC 开关解析
    private fun parseMisc(payload: ByteArray): List<ProtocolEvent> {
        if (payload.size < 2) return emptyList()
        val events = mutableListOf<ProtocolEvent>()
        if ((payload[0].toInt() and 0xFF) == 0x00) {
            val count = payload[1].toInt() and 0xFF
            var index = 2
            repeat(count) {
                if (index + 1 < payload.size) {
                    miscEvent(payload[index].toInt() and 0xFF, payload[index + 1].toInt() and 0xFF)
                        ?.let(events::add)
                    index += 2
                }
            }
        } else {
            // 部分固件/订阅事件直接回 [miscType, value]
            miscEvent(payload[0].toInt() and 0xFF, payload[1].toInt() and 0xFF)
                ?.let(events::add)
        }
        return events
    }

    private fun miscEvent(type: Int, rawValue: Int): ProtocolEvent? = when (type) {
        OpoMiscType.GAME_MODE -> ProtocolEvent.GameMode(rawValue == 0x01)
        OpoMiscType.MULTIPOINT -> ProtocolEvent.DualConnection(rawValue == 0x01)
        OpoMiscType.IN_EAR_DETECTION -> ProtocolEvent.InEar(rawValue == 0x01)
        else -> null
    }

    // ------------------------------------------------------------ 触控配置解析
    /** 触控：`status, (side, typeLE, action)*`，每 4 字节一组。 */
    private fun parseTouch(payload: ByteArray): List<ProtocolEvent> {
        if (payload.size < 6) return emptyList()
        val gestures = linkedMapOf<EarSide, MutableMap<GestureType, GestureAction>>()
        var index = 2
        while (index + 3 < payload.size) {
            val side = touchSideOf(payload[index].toInt() and 0xFF)
            val typeCode = (payload[index + 1].toInt() and 0xFF) or
                ((payload[index + 2].toInt() and 0xFF) shl 8)
            val actionCode = payload[index + 3].toInt() and 0xFF
            val type = gestureTypeOf(typeCode)
            val action = actionOf(actionCode)
            if (side != null && type != null && action != null) {
                gestures.getOrPut(side) { linkedMapOf() }[type] = action
            }
            index += 4
        }
        return if (gestures.isEmpty()) emptyList() else listOf(ProtocolEvent.Gestures(gestures))
    }

    // ------------------------------------------------------------ 订阅事件解析
    private fun parseSubscription(payload: ByteArray): List<ProtocolEvent> {
        if (payload.isEmpty()) return emptyList()
        return when (payload[0].toInt() and 0xFF) {
            OpoSubscriptionType.BATTERY -> parseBattery(payload)
                ?.let { listOf(ProtocolEvent.Battery(it)) }
                ?: emptyList()

            OpoSubscriptionType.ANC_MODE -> parseAnc(payload)

            OpoSubscriptionType.GAME_MODE ->
                if (payload.size >= 2) {
                    listOf(ProtocolEvent.GameMode((payload[1].toInt() and 0xFF) == 0x01))
                } else {
                    emptyList()
                }

            // STATUS（佩戴状态）、MULTIPOINT（设备列表）当前 UI 无对应字段，先忽略。
            else -> emptyList()
        }
    }

    // ---------------------------------------------------------------- 映射工具
    private fun ancLevelCode(level: AncLevel): Int = when (level) {
        AncLevel.Smart -> OpoAncValue.LEVEL_DYNAMIC
        AncLevel.Deep -> OpoAncValue.LEVEL_HIGH
        AncLevel.Medium -> OpoAncValue.LEVEL_NORMAL
        AncLevel.Light -> OpoAncValue.LEVEL_LOW
    }

    private fun touchTypeCode(type: GestureType): Int = when (type) {
        GestureType.DoubleTap -> OpoTouchType.DOUBLE_TAP
        GestureType.TripleTap -> OpoTouchType.TRIPLE_TAP
        GestureType.LongPress -> OpoTouchType.LONG_PRESS
    }

    /** 协议动作码；`GestureAction.ToggleAnc` 在 realme 系无确认取值（TODO 真机确认）。 */
    private fun actionCode(action: GestureAction): Int? = when (action) {
        GestureAction.PlayPause -> OpoTouchAction.PLAY_PAUSE
        GestureAction.Previous -> OpoTouchAction.PREVIOUS
        GestureAction.Next -> OpoTouchAction.NEXT
        GestureAction.VolumeUp -> OpoTouchAction.VOLUME_UP
        GestureAction.VolumeDown -> OpoTouchAction.VOLUME_DOWN
        GestureAction.VoiceAssistant -> OpoTouchAction.VOICE_ASSISTANT
        GestureAction.None -> OpoTouchAction.NONE
        GestureAction.ToggleAnc -> null
    }

    private fun gestureTypeOf(code: Int): GestureType? = when (code) {
        OpoTouchType.DOUBLE_TAP -> GestureType.DoubleTap
        OpoTouchType.TRIPLE_TAP -> GestureType.TripleTap
        OpoTouchType.LONG_PRESS -> GestureType.LongPress
        else -> null
    }

    private fun actionOf(code: Int): GestureAction? = when (code) {
        OpoTouchAction.NONE -> GestureAction.None
        OpoTouchAction.PLAY_PAUSE -> GestureAction.PlayPause
        OpoTouchAction.VOICE_ASSISTANT, 0x03 -> GestureAction.VoiceAssistant
        OpoTouchAction.PREVIOUS -> GestureAction.Previous
        OpoTouchAction.NEXT -> GestureAction.Next
        OpoTouchAction.VOLUME_UP -> GestureAction.VolumeUp
        OpoTouchAction.VOLUME_DOWN -> GestureAction.VolumeDown
        else -> null
    }

    private fun u16Le(value: Int): ByteArray =
        byteArrayOf((value and 0xFF).toByte(), ((value ushr 8) and 0xFF).toByte())

    private companion object {
        val TOUCH_SIDES = intArrayOf(0x01, 0x02)

    /** 左右耳 → 协议 side 字节（左 0x01 / 右 0x02）。 */
    fun touchSideCode(side: EarSide): Int = if (side == EarSide.Left) 0x01 else 0x02

    /** 协议 side 字节 → 左右耳。 */
    fun touchSideOf(code: Int): EarSide? = when (code) {
        0x01 -> EarSide.Left
        0x02 -> EarSide.Right
        else -> null
    }
    }
}
