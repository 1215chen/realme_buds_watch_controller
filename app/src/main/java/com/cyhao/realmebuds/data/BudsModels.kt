package com.cyhao.realmebuds.data

import com.cyhao.realmebuds.R

/** 耳机连接状态。 */
enum class ConnectionState { Disconnected, Connecting, Connected }

/** 左右耳。手势、查找等命令需要按耳区分。 */
enum class EarSide { Left, Right }

/** 降噪主模式（对应 realme Link 的「降噪控制」）。 */
enum class AncMode { Off, Transparency, NoiseCancellation }

/** 降噪强度（智能/深度/中等/轻度）。 */
enum class AncLevel { Smart, Deep, Medium, Light }

/** 音效预设。 */
/**
 * 音效预设：采用 realme 通用音效档位（协议 id 即 ordinal，0..5）。
 * 不同型号支持的档位数量不同，UI 一次展示全部，耳机不支持的档位会被拒绝并提示失败。
 */
enum class EqPreset { VocalPure, Original, BassPure, BassDeep, Bright, Dynamic }

/** 可配置的触控手势类型。 */
enum class GestureType { DoubleTap, TripleTap, LongPress }

/** 触控手势可映射的动作。 */
enum class GestureAction { PlayPause, Previous, Next, VolumeUp, VolumeDown, VoiceAssistant, ToggleAnc, None }

/** 电量信息，单位百分比；null 表示该部位未知（如取出单耳）。 */
data class BatteryInfo(
    val left: Int? = null,
    val right: Int? = null,
    val case: Int? = null,
    val charging: Boolean = false,
)

/** 扫描到的 BLE 设备。 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val realme: Boolean = false,
)

/**
 * 型号能力（用于隐藏耳机不支持的开关）。
 *
 * 真实耳机在连接初期通常无法得知型号能力，因此 [known] 为 false、各能力默认 false，
 * 由协议通知（如入耳事件、多点连接事件）在运行中逐步确认后置为 true；演示模式给出完整能力。
 */
data class DeviceCapabilities(
    val known: Boolean = false,
    val inEarDetection: Boolean = false,
    val dualConnection: Boolean = false,
    val ancLevel: Boolean = false,
    val gameMode: Boolean = true,
    val findBuds: Boolean = true,
    /** 充电盒是否上报电量（T200 只上报 L/R）。 */
    val caseBattery: Boolean = true,
)

/** 耳机完整状态快照；UI 只读。 */
data class BudsState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val name: String = "",
    val model: String = "",
    val firmware: String = "",
    val address: String = "",
    val battery: BatteryInfo = BatteryInfo(),
    val ancMode: AncMode = AncMode.Off,
    val ancLevel: AncLevel = AncLevel.Smart,
    val eqPreset: EqPreset = EqPreset.Original,
    /** 手势按左右耳分别保存（两只耳可以不同）。 */
    val gestures: Map<EarSide, Map<GestureType, GestureAction>> = defaultGestures(),
    val gameMode: Boolean = false,
    val dualConnection: Boolean = false,
    val inEarDetection: Boolean = true,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val isRealme: Boolean = false,
    val demo: Boolean = false,
) {
    val connected: Boolean get() = connection == ConnectionState.Connected

    /** 取某只耳朵某个手势的动作。 */
    fun gesture(side: EarSide, type: GestureType): GestureAction =
        gestures[side]?.get(type) ?: GestureAction.None
}

fun defaultGestures(): Map<EarSide, Map<GestureType, GestureAction>> = mapOf(
    EarSide.Left to mapOf(
        GestureType.DoubleTap to GestureAction.PlayPause,
        GestureType.TripleTap to GestureAction.Previous,
        GestureType.LongPress to GestureAction.ToggleAnc,
    ),
    EarSide.Right to mapOf(
        GestureType.DoubleTap to GestureAction.PlayPause,
        GestureType.TripleTap to GestureAction.Next,
        GestureType.LongPress to GestureAction.ToggleAnc,
    ),
)

// ---- UI 文案映射（资源 id 集中在 data 层，便于协议层与 UI 层共用） ----

/** 预设对应的协议 id（realme 通用编号，等于 ordinal）。 */
val EqPreset.protocolId: Int
    get() = ordinal

/** 按协议 id 解析预设（所有型号通用）。 */
fun eqPresetFromId(id: Int): EqPreset? = EqPreset.entries.getOrNull(id)

val EarSide.labelRes: Int
    get() = when (this) {
        EarSide.Left -> R.string.gesture_left_ear
        EarSide.Right -> R.string.gesture_right_ear
    }

val AncMode.labelRes: Int
    get() = when (this) {
        AncMode.Off -> R.string.noise_off
        AncMode.Transparency -> R.string.noise_transparency
        AncMode.NoiseCancellation -> R.string.noise_cancellation
    }

val AncLevel.labelRes: Int
    get() = when (this) {
        AncLevel.Smart -> R.string.noise_level_smart
        AncLevel.Deep -> R.string.noise_level_deep
        AncLevel.Medium -> R.string.noise_level_medium
        AncLevel.Light -> R.string.noise_level_light
    }

val EqPreset.labelRes: Int
    get() = when (this) {
        EqPreset.VocalPure -> R.string.eq_vocal_pure
        EqPreset.Original -> R.string.eq_original
        EqPreset.BassPure -> R.string.eq_bass_pure
        EqPreset.BassDeep -> R.string.eq_bass_deep
        EqPreset.Bright -> R.string.eq_bright
        EqPreset.Dynamic -> R.string.eq_dynamic
    }

val GestureType.labelRes: Int
    get() = when (this) {
        GestureType.DoubleTap -> R.string.gesture_double
        GestureType.TripleTap -> R.string.gesture_triple
        GestureType.LongPress -> R.string.gesture_long
    }

val GestureAction.labelRes: Int
    get() = when (this) {
        GestureAction.PlayPause -> R.string.gesture_action_play_pause
        GestureAction.Previous -> R.string.gesture_action_prev
        GestureAction.Next -> R.string.gesture_action_next
        GestureAction.VolumeUp -> R.string.gesture_action_volume_up
        GestureAction.VolumeDown -> R.string.gesture_action_volume_down
        GestureAction.VoiceAssistant -> R.string.gesture_action_assistant
        GestureAction.ToggleAnc -> R.string.gesture_action_toggle_anc
        GestureAction.None -> R.string.gesture_action_none
    }
