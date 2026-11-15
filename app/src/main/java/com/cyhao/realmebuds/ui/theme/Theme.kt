package com.cyhao.realmebuds.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography

/**
 * 应用主题：Material Design 3 蓝色系（dark blue baseline）。
 * 不使用任何品牌定制色，所有颜色来自蓝色调性板的官方语义角色。
 */

// 语义状态色（非品牌色）：连接成功 / 低电量提示
val ConnectedGreen = Color(0xFF6DD58C)

/**
 * M3 蓝色系 dark ColorScheme（基于蓝色 tonal palette 的语义角色）。
 */
private val BlueColorScheme = ColorScheme(
    primary = Color(0xFFA8C7FA),
    primaryDim = Color(0xFF7FA9E8),
    primaryContainer = Color(0xFF0842A0),
    onPrimary = Color(0xFF062E6F),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBFC6DC),
    secondaryDim = Color(0xFF8E96AA),
    secondaryContainer = Color(0xFF3F4759),
    onSecondary = Color(0xFF293042),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFF9FCAFF),
    tertiaryDim = Color(0xFF6E9BD1),
    tertiaryContainer = Color(0xFF0842A0),
    onTertiary = Color(0xFF003257),
    onTertiaryContainer = Color(0xFFD1E4FF),
    surfaceContainerLow = Color(0xFF1A222C),
    surfaceContainer = Color(0xFF222B37),
    surfaceContainerHigh = Color(0xFF2C3847),
    onSurface = Color(0xFFE1E7F0),
    onSurfaceVariant = Color(0xFFC0CADC),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44464F),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE1E7F0),
    error = Color(0xFFF2B8B5),
    errorDim = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C1D18),
    onError = Color(0xFF601410),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun BudsWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlueColorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}

/**
 * 电量状态色：只使用 M3 官方语义色角色。
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
