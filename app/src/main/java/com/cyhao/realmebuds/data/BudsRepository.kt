package com.cyhao.realmebuds.data

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 耳机控制仓库：UI 层只依赖本接口。
 *
 * 实现：
 * - [DemoBudsRepository]：演示模式（无耳机时体验全部界面）。
 * - `com.cyhao.realmebuds.ble.BleBudsRepository`：真实 BLE 实现（realme 私有协议）。
 */
interface BudsRepository {

    /** 当前耳机状态。 */
    val state: StateFlow<BudsState>

    /** 扫描结果列表。 */
    val scanResults: StateFlow<List<ScannedDevice>>

    /** 是否正在扫描。 */
    val scanning: StateFlow<Boolean>

    /** 一次性提示消息（string 资源 id），UI 用页面内浮层展示。 */
    val messages: SharedFlow<Int>

    fun startScan()

    fun stopScan()

    fun connect(device: ScannedDevice)

    fun disconnect()

    fun setAncMode(mode: AncMode)

    fun setAncLevel(level: AncLevel)

    fun setEq(preset: EqPreset)

    /** 设置某只耳朵的某个手势动作（左右耳可以不同）。 */
    fun setGesture(side: EarSide, type: GestureType, action: GestureAction)

    fun setGameMode(enabled: Boolean)

    fun setDualConnection(enabled: Boolean)

    fun setInEarDetection(enabled: Boolean)

    /** 让耳机播放提示音（查找耳机）。 */
    fun findBuds()

    /** 停止查找提示音。 */
    fun stopFindBuds()

    fun factoryReset()

    /** 释放 BLE 资源（Activity 销毁时调用）。 */
    fun close()
}
