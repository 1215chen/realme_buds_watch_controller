package com.cyhao.realmebuds.ble

import java.util.concurrent.atomic.AtomicInteger

/**
 * realme / OPPO 系耳机控制通道的两种串流帧方言。
 *
 * 逆向资料（realme Link APK 静态分析 + 公开的 HCI 抓包逆向）显示同一套 16 位命令字
 * 被两种 SDK 使用，帧头不同：
 *
 * - [OPO]：`AA | LL | 00 | 00 | CC CC | SS | PP PP | payload`（9 字节头，little-endian）
 *   证据：realme Buds Air 7 Pro HCI 抓包 + Gadgetbridge/NTeditor 的 OPlus 实现。
 * - [TL]：`CC CC | SS | PP PP | payload`（5 字节头，big-endian；`CC` bit15=响应标志）
 *   证据：com.realme.iot.headset.tl.protocol.packet.Packet（官方客户端静态分析）+ docs/PROTOCOL.md。
 *
 * 两种方言共用命令字（0x0106 电量、0x0105 固件、0x010C/0x0404 降噪、0x0406 EQ…），
 * 但没有可靠的型号→方言映射，因此运行时做「发送探测 + 收包自适应」。
 * TODO(真机)：确定具体型号使用的方言后，可把 [FrameDialect] 固定到配置里。
 */
internal enum class FrameDialect {
    /** OPO v1 / OPlus 帧（realme Air 系列 RFCOMM、部分 OPPO/OnePlus BLE GATT）。 */
    OPO,

    /** Telink SDK 帧（realme Link 新版 RUS/VENDOR_TL 型号）。 */
    TL,
}

/** 一条已解码的协议帧。 */
internal data class ProtocolFrame(
    val command: Int,
    val seq: Int,
    val payload: ByteArray,
    val dialect: FrameDialect,
) {
    /** 帧中的响应标志位（bit15）。 */
    val isResponse: Boolean get() = (command and FRAME_RESPONSE_FLAG) != 0

    /** 去掉方向位后的基命令。 */
    val baseCommand: Int get() = command and 0x7FFF

    companion object {
        const val FRAME_RESPONSE_FLAG: Int = 0x8000
    }
}

/** 帧编码器：维护自增序号，按方言组包。 */
internal class FrameEncoder {

    private val sequence = AtomicInteger(0)

    fun encode(dialect: FrameDialect, command: Int, payload: ByteArray = EMPTY): ByteArray =
        when (dialect) {
            FrameDialect.OPO -> encodeOpo(command, payload)
            FrameDialect.TL -> encodeTl(command, payload)
        }

    /** 供协议层构造探测帧时使用。 */
    fun encodeOpo(command: Int, payload: ByteArray = EMPTY): ByteArray {
        // 帧：AA LL 00 00 CC CC SS PP PP payload；LL = 整帧长度 - 2 = 7 + payload.size
        val frame = ByteArray(OPO_HEADER_SIZE + payload.size)
        frame[0] = OPO_PREAMBLE
        frame[1] = (frame.size - 2).toByte()
        frame[2] = 0
        frame[3] = 0
        frame[4] = (command and 0xFF).toByte()
        frame[5] = ((command ushr 8) and 0xFF).toByte()
        frame[6] = nextSeq()
        frame[7] = (payload.size and 0xFF).toByte()
        frame[8] = ((payload.size ushr 8) and 0xFF).toByte()
        payload.copyInto(frame, OPO_HEADER_SIZE)
        return frame
    }

    fun encodeTl(command: Int, payload: ByteArray = EMPTY): ByteArray {
        // 帧：CC CC SS LL LL payload（big-endian，LL = payload 长度）
        val frame = ByteArray(TL_HEADER_SIZE + payload.size)
        frame[0] = ((command ushr 8) and 0xFF).toByte()
        frame[1] = (command and 0xFF).toByte()
        frame[2] = nextSeq()
        frame[3] = ((payload.size ushr 8) and 0xFF).toByte()
        frame[4] = (payload.size and 0xFF).toByte()
        payload.copyInto(frame, TL_HEADER_SIZE)
        return frame
    }

    fun nextSeq(): Byte = (sequence.getAndIncrement() and 0xFF).toByte()

    companion object {
        val EMPTY: ByteArray = ByteArray(0)

        val OPO_PREAMBLE: Byte = 0xAA.toByte()
        const val OPO_HEADER_SIZE: Int = 9
        const val TL_HEADER_SIZE: Int = 5

        /** TL 帧首字节允许的命令高位（0x01..0x05 / 0x0F 及其响应变体）。 */
        private val TL_FIRST_BYTES: Set<Int> = buildSet {
            for (value in 0x01..0x05) add(value)
            add(0x0F)
            for (value in 0x81..0x85) add(value)
            add(0x8F)
        }

        fun looksLikeTlHeader(firstByte: Int): Boolean = firstByte in TL_FIRST_BYTES
    }
}

/**
 * 流式帧组装器：自动识别 OPO / TL 方言，处理粘包与半包。
 *
 * 只在仓库的单线程 Handler 上调用，无需额外同步。
 */
internal class FrameAssembler {

    private var buffer: ByteArray = ByteArray(0)

    fun reset() {
        buffer = ByteArray(0)
    }

    fun feed(chunk: ByteArray, onFrame: (ProtocolFrame) -> Unit) {
        if (chunk.isEmpty()) return
        val merged = ByteArray(buffer.size + chunk.size)
        buffer.copyInto(merged)
        chunk.copyInto(merged, buffer.size)

        var start = 0
        loop@ while (start < merged.size) {
            val first = merged[start].toInt() and 0xFF
            if (first == (FrameEncoder.OPO_PREAMBLE.toInt() and 0xFF)) {
                if (merged.size - start < 2) break@loop
                val total = (merged[start + 1].toInt() and 0xFF) + 2
                if (total < FrameEncoder.OPO_HEADER_SIZE) {
                    start++
                    continue@loop
                }
                if (merged.size - start < total) break@loop
                val frame = parseOpo(merged, start)
                if (frame == null) {
                    start++
                    continue@loop
                }
                onFrame(frame)
                start += total
            } else if (FrameEncoder.looksLikeTlHeader(first)) {
                if (merged.size - start < FrameEncoder.TL_HEADER_SIZE) break@loop
                val payloadLength = ((merged[start + 3].toInt() and 0xFF) shl 8) or
                    (merged[start + 4].toInt() and 0xFF)
                val total = FrameEncoder.TL_HEADER_SIZE + payloadLength
                if (merged.size - start < total) break@loop
                val command = ((merged[start].toInt() and 0xFF) shl 8) or
                    (merged[start + 1].toInt() and 0xFF)
                val seq = merged[start + 2].toInt() and 0xFF
                onFrame(
                    ProtocolFrame(
                        command = command,
                        seq = seq,
                        payload = merged.copyOfRange(
                            start + FrameEncoder.TL_HEADER_SIZE,
                            start + total,
                        ),
                        dialect = FrameDialect.TL,
                    )
                )
                start += total
            } else {
                start++
            }
        }
        buffer = if (start >= merged.size) {
            ByteArray(0)
        } else {
            merged.copyOfRange(start, merged.size)
        }
    }

    private fun parseOpo(source: ByteArray, offset: Int): ProtocolFrame? {
        val payloadLength = (source[offset + 7].toInt() and 0xFF) or
            ((source[offset + 8].toInt() and 0xFF) shl 8)
        val payloadStart = offset + FrameEncoder.OPO_HEADER_SIZE
        if (payloadStart + payloadLength > source.size) return null
        val command = (source[offset + 4].toInt() and 0xFF) or
            ((source[offset + 5].toInt() and 0xFF) shl 8)
        return ProtocolFrame(
            command = command,
            seq = source[offset + 6].toInt() and 0xFF,
            payload = source.copyOfRange(payloadStart, payloadStart + payloadLength),
            dialect = FrameDialect.OPO,
        )
    }
}
