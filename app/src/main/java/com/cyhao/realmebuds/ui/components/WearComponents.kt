package com.cyhao.realmebuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BatteryInfo
import com.cyhao.realmebuds.ui.theme.batteryColor

/** 小节标题（列表分组标题，使用官方 ListHeader 默认样式）。 */
@Composable
fun WearSectionHeader(text: String, modifier: Modifier = Modifier) {
    ListHeader(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 列表行按钮：图标 + 主标签 + 副标签（Wear M3 Button 的 icon/secondaryLabel 槽位）。 */
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** 开关行（Wear M3 SwitchButton）。 */
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

/** 连接状态点 + 文案（颜色使用官方角色：primary=已连接，secondary=连接中）。 */
@Composable
fun ConnectionStatusRow(
    connected: Boolean,
    connecting: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when {
        connected -> MaterialTheme.colorScheme.primary
        connecting -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 电量概览：左耳 / 右耳 / 充电盒（部分型号不上报充电盒电量时只显示左右耳）。 */
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
        BatteryColumn(stringResource(R.string.battery_left), battery.left, modifier = Modifier.weight(1f))
        BatteryColumn(stringResource(R.string.battery_right), battery.right, modifier = Modifier.weight(1f))
        if (showCase) {
            BatteryColumn(stringResource(R.string.battery_case), battery.case, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BatteryColumn(label: String, percent: Int?, modifier: Modifier = Modifier) {
    val percentColor = if (percent == null) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        batteryColor(percent)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = if (percent == null) "—" else "$percent%",
            style = MaterialTheme.typography.titleMedium,
            color = percentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        BatteryBar(
            percent = percent,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(3.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 细电量条（轨道与填充均使用官方主题角色）。 */
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
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(batteryColor(percent))
            )
        }
    }
}
