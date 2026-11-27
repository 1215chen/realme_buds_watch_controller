package com.cyhao.realmebuds.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BatteryInfo
import com.cyhao.realmebuds.ui.theme.ConnectedGreen
import com.cyhao.realmebuds.ui.theme.batteryColor

/** 小节标题（居中）。 */
@Composable
fun WearSectionHeader(text: String, modifier: Modifier = Modifier) {
    ListHeader(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 设计风格列表行：左侧图标 + 主标签(+副标签)，右侧可选箭头。 */
@Composable
fun WearNavRow(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** 设计风格开关行。 */
@Composable
fun WearToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

/** 兼容旧页面的开关行（图标 + 主/副标签）。 */
@Composable
fun WearSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = secondaryLabel?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

/** 兼容旧页面的按钮行。 */
@Composable
fun WearListButton(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = secondaryLabel?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

/** 连接状态点 + 文案。 */
@Composable
fun ConnectionStatusRow(
    connected: Boolean,
    connecting: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when {
        connected -> ConnectedGreen
        connecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val text = when {
        connected -> stringResource(R.string.status_connected)
        connecting -> stringResource(R.string.status_connecting)
        else -> stringResource(R.string.status_disconnected)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/**
 * 圆形电量环：外圈轨道 + 进度弧，中间显示 L/R/Case 与百分比。
 * 未上报时显示「-」。
 */
@Composable
fun BatteryRing(
    label: String,
    percent: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    strokeWidth: Dp = 4.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.onSecondaryContainer.copy(alpha = 0.30f)
    val progressColor = percent?.let { batteryColor(it) } ?: scheme.onSecondaryContainer.copy(alpha = 0.5f)
    val fraction = (percent ?: 0).coerceIn(0, 100) / 100f
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val d = this.size.minDimension - stroke
            val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(d, d),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSecondaryContainer,
                maxLines = 1,
            )
            Text(
                text = if (percent == null) "-%" else "$percent%",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

/** 电量行：L / R / Case 三个等大圆环（每个占 1/3 宽度，避免被挤压导致大小不一）。 */
@Composable
fun BatteryRings(battery: BatteryInfo, modifier: Modifier = Modifier, showCase: Boolean = true) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BatteryRing(label = "L", percent = battery.left, size = 54.dp)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BatteryRing(label = "R", percent = battery.right, size = 54.dp)
        }
        if (showCase) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BatteryRing(label = "Case", percent = battery.case, size = 54.dp)
            }
        }
    }
}

/** 兼容旧页面的细电量条。 */
@Composable
fun BatteryBar(
    percent: Int?,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val fraction = (percent ?: 0).coerceIn(0, 100) / 100f
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (percent != null && percent > 0) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(batteryColor(percent))
            )
        }
    }
}

/** 兼容旧页面的电量概览（文字版）。 */
@Composable
fun BatterySummary(
    battery: BatteryInfo,
    modifier: Modifier = Modifier,
    showCase: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryRing(label = "L", percent = battery.left)
        BatteryRing(label = "R", percent = battery.right)
        if (showCase) BatteryRing(label = "Case", percent = battery.case)
    }
}
