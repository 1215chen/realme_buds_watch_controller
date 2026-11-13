package com.cyhao.realmebuds.ble

/**
 * 命令语义置信度。
 *
 * - [CONFIRMED]：有 HCI 抓包/双实现交叉验证，或协议行为已被物理验证；
 * - [LIKELY]：至少一个成熟开源实现（Gadgetbridge/NTeditor/OBudsManager）或 APK 静态证据支持；
 * - [UNVERIFIED]：命名/日志旁证，未确认命令字。
 */
internal enum class CommandConfidence {
    CONFIRMED,
    LIKELY,
    UNVERIFIED,
}

/** 业务功能。 */
internal enum class RealmeFeature {
    Battery,
    Firmware,
    Status,
    AncMode,
    AncLevel,
    Eq,
    TouchConfig,
    MiscConfig,
    GameMode,
    DualConnection,
    InEarDetection,
    FindBuds,
    Subscription,
    Handshake,
    FactoryReset,
}

/**
 * 功能 → 命令字映射表（唯一的「cmd ↔ 功能」映射来源）。
 *
 * 命令字取值来自 realme Link APK 的 `Protocol` 常量表（见 docs/PROTOCOL.md）
 * 与 OPO v1 逆向资料的交集；语义绑定参考：
 * - realme Buds Air 7 Pro HCI 抓包（earFrame）
 * - Gadgetbridge / NTeditor Gadgetbridge OPlus 实现
 * - OnePlus Nord Buds 3 Pro BLE 抓包（HELLO/REGISTER）
 *
 * 注意：APK 分析报告指出「165 个常量 → 业务语义」尚未由反汇编直接绑定；
 * 这里的映射是上述外部抓包/开源实现的交叉结果，按 [CommandConfidence] 标注。
 * 真机抓包后只需改本文件，协议层与仓库层无需变更。
 */
internal object RealmeCommandMap {

    val BATTERY: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Battery,
        query = OpoCommand.BATTERY_REQ,
        queryResponse = OpoCommand.BATTERY_RET,
        confidence = CommandConfidence.CONFIRMED,
        note = "电量：status/count/(side,level)*，side 左1右2盒3，bit7 充电",
    )

    val FIRMWARE: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Firmware,
        query = OpoCommand.FIRMWARE_REQ,
        queryResponse = OpoCommand.FIRMWARE_RET,
        confidence = CommandConfidence.CONFIRMED,
        note = "固件：逗号三元组，type=2 为固件版本",
    )

    val STATUS: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Status,
        query = OpoCommand.STATUS_REQ,
        queryResponse = OpoCommand.STATUS_RET,
        confidence = CommandConfidence.LIKELY,
        note = "佩戴/状态位图；当前 UI 无对应字段，仅解析日志",
    )

    val ANC_MODE: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.AncMode,
        query = OpoCommand.ANC_CONFIG_REQ,
        queryResponse = OpoCommand.ANC_CONFIG_RET,
        set = OpoCommand.ANC_CONFIG_SET,
        setAck = OpoCommand.ANC_CONFIG_ACK,
        confidence = CommandConfidence.CONFIRMED,
        note = "payload 01 01 <mode>；mode 关01/通透02/降噪等级04(轻)08(深)10(中)20(智能)",
    )

    val ANC_LEVEL: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.AncLevel,
        query = OpoCommand.ANC_CONFIG_REQ,
        queryResponse = OpoCommand.ANC_CONFIG_RET,
        set = OpoCommand.ANC_CONFIG_SET,
        setAck = OpoCommand.ANC_CONFIG_ACK,
        confidence = CommandConfidence.LIKELY,
        note = "与降噪共用命令字，子类型 0x0104（DYNAMIC_LEVEL）；部分型号不支持",
    )

    val EQ: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Eq,
        query = OpoCommand.EQUALIZER_REQ,
        queryResponse = OpoCommand.EQUALIZER_RET,
        set = OpoCommand.EQUALIZER_SET,
        setAck = OpoCommand.EQUALIZER_ACK,
        confidence = CommandConfidence.CONFIRMED,
        note = "payload 单字节预设 id；Air 7 Pro 确认 0..3",
    )

    val TOUCH_CONFIG: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.TouchConfig,
        query = OpoCommand.TOUCH_CONFIG_REQ,
        queryResponse = OpoCommand.TOUCH_CONFIG_RET,
        set = OpoCommand.TOUCH_CONFIG_SET,
        setAck = OpoCommand.TOUCH_CONFIG_ACK,
        confidence = CommandConfidence.LIKELY,
        note = "payload 01 <side> <typeLE> <action>；side 左1右2",
    )

    val MISC_CONFIG: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.MiscConfig,
        query = OpoCommand.MISC_CONFIG_REQ,
        queryResponse = OpoCommand.MISC_CONFIG_RET,
        set = OpoCommand.MISC_CONFIG_SET,
        setAck = OpoCommand.MISC_CONFIG_ACK,
        confidence = CommandConfidence.CONFIRMED,
        note = "query payload: count,type...；set payload: type,0/1",
    )

    val GAME_MODE: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.GameMode,
        confidence = CommandConfidence.CONFIRMED,
        note = "MISC 子类型 0x06（复用 [MISC_CONFIG] 命令字）",
    )

    val DUAL_CONNECTION: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.DualConnection,
        confidence = CommandConfidence.LIKELY,
        note = "MISC 子类型 0x11（多点连接，NTeditor Gadgetbridge）",
    )

    val IN_EAR_DETECTION: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.InEarDetection,
        confidence = CommandConfidence.LIKELY,
        note = "MISC 子类型 0x04（入耳检测，NTeditor Gadgetbridge）",
    )

    val FIND_BUDS: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.FindBuds,
        query = null,
        queryResponse = null,
        set = OpoCommand.FIND_DEVICE_REQ,
        setAck = OpoCommand.FIND_DEVICE_ACK,
        confidence = CommandConfidence.LIKELY,
        note = "payload 01 开始 / 00 停止",
    )

    val SUBSCRIPTION: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Subscription,
        set = OpoCommand.SUBSCRIPTION_SET,
        setAck = OpoCommand.SUBSCRIPTION_ACK,
        confidence = CommandConfidence.LIKELY,
        note = "payload 09,type...；事件命令 0x0204",
    )

    val HANDSHAKE: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.Handshake,
        set = OpoCommand.HELLO_REQ,
        confidence = CommandConfidence.LIKELY,
        note = "BLE GATT 形态的 OPO v1：HELLO 0x0100、REGISTER 0x8500；" +
            "realme Air 的 RFCOMM 通道无需应用层握手",
    )

    val FACTORY_RESET: FeatureCommand = FeatureCommand(
        feature = RealmeFeature.FactoryReset,
        confidence = CommandConfidence.UNVERIFIED,
        note = "APK 有 setRestoreData，但 opcode 未确认；高风险操作，未下发",
    )

    /** 全部映射（便于文档/调试/未来配置化）。 */
    val ALL: List<FeatureCommand> = listOf(
        BATTERY, FIRMWARE, STATUS, ANC_MODE, ANC_LEVEL, EQ, TOUCH_CONFIG, MISC_CONFIG,
        GAME_MODE, DUAL_CONNECTION, IN_EAR_DETECTION, FIND_BUDS, SUBSCRIPTION, HANDSHAKE,
        FACTORY_RESET,
    )

    fun of(feature: RealmeFeature): FeatureCommand = when (feature) {
        RealmeFeature.Battery -> BATTERY
        RealmeFeature.Firmware -> FIRMWARE
        RealmeFeature.Status -> STATUS
        RealmeFeature.AncMode -> ANC_MODE
        RealmeFeature.AncLevel -> ANC_LEVEL
        RealmeFeature.Eq -> EQ
        RealmeFeature.TouchConfig -> TOUCH_CONFIG
        RealmeFeature.MiscConfig -> MISC_CONFIG
        RealmeFeature.GameMode -> GAME_MODE
        RealmeFeature.DualConnection -> DUAL_CONNECTION
        RealmeFeature.InEarDetection -> IN_EAR_DETECTION
        RealmeFeature.FindBuds -> FIND_BUDS
        RealmeFeature.Subscription -> SUBSCRIPTION
        RealmeFeature.Handshake -> HANDSHAKE
        RealmeFeature.FactoryReset -> FACTORY_RESET
    }

    /** 查询命令字；无查询能力的条目返回 null。 */
    fun query(feature: RealmeFeature): Int? = of(feature).query

    /** 查询响应命令字。 */
    fun queryResponse(feature: RealmeFeature): Int? = of(feature).queryResponse

    /** 设置命令字；无设置能力的条目返回 null。 */
    fun set(feature: RealmeFeature): Int? = of(feature).set

    /** 设置 ACK 命令字。 */
    fun setAck(feature: RealmeFeature): Int? = of(feature).setAck
}

/** 单个功能对应的命令字与置信度。 */
internal data class FeatureCommand(
    val feature: RealmeFeature,
    val query: Int? = null,
    val queryResponse: Int? = null,
    val set: Int? = null,
    val setAck: Int? = null,
    val confidence: CommandConfidence,
    val note: String = "",
)
