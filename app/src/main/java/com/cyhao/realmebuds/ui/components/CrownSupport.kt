package com.cyhao.realmebuds.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlin.math.abs

/**
 * 表冠输入总线。
 *
 * 不同系统对旋转表冠的上报方式不同：
 * - Wear OS：RotaryScrollEvent（ScalingLazyColumn 已内置支持）
 * - ColorOS Watch / 部分方形表：ACTION_SCROLL 的通用 MotionEvent
 * - 另一些机型：DPAD_UP/DOWN、PAGE_UP/DOWN 按键事件
 *
 * 这里统一转成「列表滚动步数」事件，由各页面的 [CrownScrollEffect] 消费。
 */
object CrownInput {
    private val _events = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Int> = _events.asSharedFlow()

    fun emit(steps: Int) {
        if (steps != 0) _events.tryEmit(steps.coerceIn(-1, 1))
    }

    /** 由 MotionEvent 的滚动轴值换算滚动步数。 */
    fun fromScrollAxis(value: Float) {
        if (abs(value) < 0.01f) return
        emit(if (value > 0f) 1 else -1)
    }
}

/**
 * 让页面列表响应表冠输入（按键 / 滚动事件两种来源）。
 * 放在 ScreenScaffold 之前调用一次即可。
 */
@Composable
fun CrownScrollEffect(state: ScalingLazyListState) {
    LaunchedEffect(state) {
        CrownInput.events.collect { steps ->
            val info = state.layoutInfo
            if (info.totalItemsCount <= 0) return@collect
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return@collect
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val current = items.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) }?.index ?: 0
            val target = (current + steps).coerceIn(0, (info.totalItemsCount - 1).coerceAtLeast(0))
            if (target != current) {
                state.animateScrollToItem(target)
            }
        }
    }
}
