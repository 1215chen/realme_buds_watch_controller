package com.cyhao.realmebuds.ble

import java.util.UUID

/**
 * BLE 层集中常量。
 *
 * 说明：协议来源与置信度见 `docs/BLE_IMPL.md` 与 `docs/PROTOCOL.md`。
 * 这里只放「常量」，不参与业务逻辑，便于后续按分析报告/真机抓包替换。
 */
internal object BleConstants {

    // ---------------------------------------------------------------- 标准 GATT
    /** BAS 电量服务 0x180F。 */
    val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

    /** BAS 电量特征 0x2A19。 */
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    /** DIS 设备信息服务 0x180A。 */
    val SERVICE_DEVICE_INFO: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

    /** DIS 厂商名 0x2A29。 */
    val CHAR_MANUFACTURER_NAME: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")

    /** DIS 型号 0x2A24。 */
    val CHAR_MODEL_NUMBER: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

    /** DIS 固件版本 0x2A26。 */
    val CHAR_FIRMWARE_REVISION: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

    /** CCCD 描述符 0x2902。 */
    val DESCRIPTOR_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ------------------------------------------------- realme / OPPO OPO v1 私有通道
    /**
     * OPO/realme 私有服务。
     *
     * - 在部分 OPPO/OnePlus 机型上以 BLE GATT 服务形式出现（写 + 通知特征见下）；
     * - 在 realme Air 系列上同一 UUID 位于 BR/EDR SDP（经典蓝牙 RFCOMM 通道），
     *   因此它既可能是 GATT 服务，也可能是 SPP 的 service UUID。
     */
    val OPO_SERVICE: UUID = UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5")

    /** OPO 写特征（BLE 机型；必须 use Write Without Response）。 */
    val OPO_WRITE_CHARACTERISTIC: UUID = UUID.fromString("0100079a-d102-11e1-9b23-00025b00a5a5")

    /** OPO 通知特征（BLE 机型）。 */
    val OPO_NOTIFY_CHARACTERISTIC: UUID = UUID.fromString("0200079a-d102-11e1-9b23-00025b00a5a5")

    /** 经典串口服务 SPP（兜底 RFCOMM UUID）。 */
    val SPP_SERVICE: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    /** Google Fast Pair 服务 0xFE2C（仅用于识别 realme 系设备广播）。 */
    val FAST_PAIR_SERVICE: UUID = UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")

    /** FE2C 遥测/固件特征；部分 OPPO 系耳机也会在该特征上回包，需要一并订阅。 */
    val FE2C_TELEMETRY_CHARACTERISTIC: UUID =
        UUID.fromString("fe2c123a-8366-4814-8eb0-01de32100bea")

    /**
     * 广播中出现即视为 realme/OPPO 系。
     *
     * 来源：对 realme Link / OPO v1 的公开逆向资料（earFrame、OBudsManager）。
     * 注意：前两个是服务 UUID，后两个是部分 realme 耳机广播里的自定义 128 位 UUID。
     */
    val VENDOR_ADVERT_SERVICE_UUIDS: List<UUID> = listOf(
        OPO_SERVICE,
        FAST_PAIR_SERVICE,
        UUID.fromString("99999999-9999-9999-9999-999999999999"),
        UUID.fromString("df21fe2c-2515-4fdb-8886-f12c4d67927c"),
    )

    /** 名称关键字（忽略大小写）；命中即视为「耳机/真我系设备」。 */
    val HEADPHONE_NAME_MARKERS: List<String> = listOf(
        "realme", "oppo", "oneplus", "enco", "buds",
    )

    /** 名称中出现即标记为 realme 品牌系（含 OPPO/OnePlus/enco 等同族协议设备）。 */
    val REALME_NAME_MARKERS: List<String> = listOf(
        "realme", "oppo", "oneplus", "enco",
    )

    /** BLE 广播厂商数据里出现这些 ASCII 片段也视为待选耳机。 */
    val MANUFACTURER_ASCII_MARKERS: List<String> = listOf(
        "realme", "oppo", "oneplus", "enco", "buds",
    )

    // ------------------------------------------------------------------- 时序参数
    /** 扫描自动停止时间（SPEC：15 秒）。 */
    const val SCAN_TIMEOUT_MS: Long = 15_000L

    /** 连接整体超时；超时后尝试 RFCOMM 兜底或报失败。 */
    const val CONNECT_TIMEOUT_MS: Long = 12_000L

    /** RFCOMM 单次 connect() 最长等待（超过后主动关闭 socket）。 */
    const val RFCOMM_CONNECT_TIMEOUT_MS: Long = 10_000L

    /** MTU 请求目标。 */
    const val MTU_TARGET: Int = 247

    /** GATT 服务发现后给描述符写/标准读取留出的窗口。 */
    const val GATT_SETUP_DELAY_MS: Long = 400L

    /** 描述符写完成后的就绪兜底延时。 */
    const val GATT_READY_FALLBACK_MS: Long = 1_200L

    /** 单条命令等待 ACK/响应的超时。 */
    const val COMMAND_TIMEOUT_MS: Long = 2_500L

    /** 非 silent 命令的额外重试次数。 */
    const val COMMAND_MAX_RETRIES: Int = 1

    /** 连续写特征之间的最小间隔（realme 配置 SPP 命令间隔约 10ms）。 */
    const val WRITE_INTERVAL_MS: Long = 15L

    /** 非用户主动断开时的自动重连次数。 */
    const val MAX_RECONNECT_ATTEMPTS: Int = 20

    /** 自动重连延迟。 */
    const val RECONNECT_DELAY_MS: Long = 2_000L

    /** 单次回连最大退避间隔（毫秒）。 */
    const val MAX_RECONNECT_DELAY_MS: Long = 30_000L

    /** 应用启动后自动回连上次设备的延迟（毫秒）。 */
    const val AUTO_CONNECT_DELAY_MS: Long = 800L

    // ------------------------------------------- OPO v1 over BLE GATT 的鉴权握手
    /**
     * BLE GATT 形态的 OPO v1 需要先 HELLO、再 REGISTER 才会接受控制命令
     * （来源：OnePlus Nord Buds 3 Pro 抓包/逆向资料）。realme Air 系列走 RFCOMM
     * 时不需要应用层握手，因此该流程只在 GATT 私有通道上执行。
     *
     * TODO(真机)：token 可能随型号变化（默认 B5 50 A0 69），确认后可放入配置。
     */
    const val BLE_HANDSHAKE_ENABLED: Boolean = true
    const val BLE_HANDSHAKE_HELLO_DELAY_MS: Long = 2_000L
    const val BLE_HANDSHAKE_REGISTER_DELAY_MS: Long = 1_500L
    val BLE_HANDSHAKE_TOKEN: ByteArray = byteArrayOf(0xB5.toByte(), 0x50, 0xA0.toByte(), 0x69)
}
