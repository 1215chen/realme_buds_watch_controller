package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.AppInfo
import com.cyhao.realmebuds.BudsRoutes
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.AncMode
import com.cyhao.realmebuds.data.BudsState
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.data.ConnectionState
import com.cyhao.realmebuds.data.defaultGestures
import com.cyhao.realmebuds.data.labelRes
import com.cyhao.realmebuds.ui.components.BatterySummary
import com.cyhao.realmebuds.ui.components.ConnectionStatusRow
import com.cyhao.realmebuds.ui.components.WearListButton
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.WearSwitchRow
import kotlinx.coroutines.delay

/** 「查找耳机」反馈时长（毫秒）。 */
private const val FindBudsFeedbackMillis = 3_000L

/**
 * 主页：设备状态卡 + 功能入口。
 * 未连接时首行提供「扫描并连接」；连接中显示加载态。
 */
@Composable
fun HomeScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    var finding by remember { mutableStateOf(false) }

    // 查找耳机的短暂反馈：3 秒后恢复常态
    LaunchedEffect(finding) {
        if (finding) {
            delay(FindBudsFeedbackMillis)
            finding = false
        }
    }

    val canControl = state.connected || state.demo
    val firmwareLabel = if (state.firmware.isBlank()) {
        stringResource(R.string.common_unknown)
    } else {
        state.firmware
    }

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { HomeDeviceCard(state = state) }
            if (state.demo) {
                item {
                    Text(
                        text = stringResource(R.string.msg_demo_enabled),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primaryDim,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
            if (!state.connected) {
                item {
                    WearListButton(
                        iconRes = R.drawable.ic_search,
                        label = stringResource(R.string.home_connect_prompt),
                        secondaryLabel = if (state.connection == ConnectionState.Connecting) {
                            stringResource(R.string.status_connecting)
                        } else {
                            stringResource(R.string.scan_hint)
                        },
                        enabled = state.connection != ConnectionState.Connecting,
                        onClick = { navController.navigate(BudsRoutes.SCAN) },
                    )
                }
            }
            item { WearSectionHeader(stringResource(R.string.home_section_controls)) }
            item {
                WearListButton(
                    iconRes = R.drawable.ic_noise,
                    label = stringResource(R.string.noise_title),
                    secondaryLabel = ancSummary(state),
                    enabled = canControl,
                    onClick = { navController.navigate(BudsRoutes.NOISE) },
                )
            }
            item {
                WearListButton(
                    iconRes = R.drawable.ic_equalizer,
                    label = stringResource(R.string.eq_title),
                    secondaryLabel = stringResource(state.eqPreset.labelRes),
                    enabled = canControl,
                    onClick = { navController.navigate(BudsRoutes.EQUALIZER) },
                )
            }
            item {
                WearListButton(
                    iconRes = R.drawable.ic_gesture,
                    label = stringResource(R.string.gesture_title),
                    secondaryLabel = if (state.gestures == defaultGestures()) {
                        stringResource(R.string.home_gesture_default)
                    } else {
                        stringResource(R.string.home_gesture_customized)
                    },
                    enabled = canControl,
                    onClick = { navController.navigate(BudsRoutes.GESTURES) },
                )
            }
            item {
                WearSwitchRow(
                    checked = state.gameMode,
                    onCheckedChange = { vm.setGameMode(it) },
                    label = stringResource(R.string.game_mode_title),
                    secondaryLabel = stringResource(R.string.game_mode_desc),
                    enabled = canControl,
                )
            }
            if (state.capabilities.findBuds) {
                item {
                    WearListButton(
                        iconRes = R.drawable.ic_find,
                        label = stringResource(
                            if (finding) R.string.find_buds_stop else R.string.find_buds_title
                        ),
                        secondaryLabel = if (finding) {
                            stringResource(R.string.find_buds_ringing)
                        } else {
                            stringResource(R.string.find_buds_desc)
                        },
                        enabled = canControl,
                        onClick = {
                            if (finding) {
                                finding = false
                                vm.stopFindBuds()
                            } else {
                                finding = true
                                vm.findBuds()
                            }
                        },
                    )
                }
            }
            item {
                WearListButton(
                    iconRes = R.drawable.ic_info,
                    label = stringResource(R.string.info_title),
                    secondaryLabel = firmwareLabel,
                    onClick = { navController.navigate(BudsRoutes.DEVICE_INFO) },
                )
            }
            item { WearSectionHeader(stringResource(R.string.home_section_more)) }
            item {
                WearListButton(
                    iconRes = R.drawable.ic_settings,
                    label = stringResource(R.string.home_settings),
                    secondaryLabel = "v" + AppInfo.VERSION,
                    onClick = { navController.navigate(BudsRoutes.ABOUT) },
                )
            }
            if (state.connected) {
                item {
                    WearListButton(
                        iconRes = R.drawable.ic_bluetooth,
                        label = stringResource(R.string.home_disconnect),
                        onClick = { vm.disconnect() },
                    )
                }
            }
        }
    }
}

/** 设备状态卡：名称 + 型号 + 连接状态点 + 电量/加载态。 */
@Composable
private fun HomeDeviceCard(state: BudsState) {
    val nameLabel = if (state.name.isBlank()) {
        stringResource(R.string.status_disconnected)
    } else {
        state.name
    }
    val modelLabel = when {
        state.model.isNotBlank() -> state.model
        state.connected -> stringResource(R.string.common_unknown)
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_buds),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nameLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (modelLabel.isNotBlank()) {
                    Text(
                        text = modelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ConnectionStatusRow(
            connected = state.connected,
            connecting = state.connection == ConnectionState.Connecting,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            state.connected -> {
                Spacer(Modifier.height(10.dp))
                BatterySummary(battery = state.battery, showCase = state.capabilities.caseBattery)
                if (state.battery.charging) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.battery_charging),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            state.connection == ConnectionState.Connecting -> {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.status_connecting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            else -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.scan_hint),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** 降噪副标签：关闭/通透，或「降噪 · 等级」。 */
@Composable
private fun ancSummary(state: BudsState): String {
    val mode = stringResource(state.ancMode.labelRes)
    return if (state.ancMode == AncMode.NoiseCancellation) {
        stringResource(R.string.home_anc_summary, mode, stringResource(state.ancLevel.labelRes))
    } else {
        mode
    }
}
