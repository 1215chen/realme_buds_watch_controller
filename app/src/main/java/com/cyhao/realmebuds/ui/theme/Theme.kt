package com.cyhao.realmebuds.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography

/**
 * 应用主题：直接使用 Material Design 3 官方基线配色与字阶，不做品牌色定制。
 *
 * 说明：Wear Material 3 的默认 [MaterialTheme] 即为深色基线方案（ColorScheme 默认值来自官方
 * ColorTokens）。因此这里只覆盖 typography/shapes 的默认实例，颜色全部走官方角色。
 */
@Composable
fun BudsWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}

/**
 * 电量状态色：只使用 Material Design 3 官方语义色角色，不引入自定义色值。
 * - 正常（>=60%）：primary
 * - 偏低（30%~59%）：secondary
 * - 低电量（<30%）：error
 */
@Composable
fun batteryColor(percent: Int): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        percent >= 60 -> scheme.primary
        percent >= 30 -> scheme.secondary
        else -> scheme.error
    }
}
