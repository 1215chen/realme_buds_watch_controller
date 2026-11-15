package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
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
import com.cyhao.realmebuds.data.labelRes
import com.cyhao.realmebuds.ui.components.BatteryRings
import com.cyhao.realmebuds.ui.components.ConnectionStatusRow
import com.cyhao.realmebuds.ui.components.WearNavRow
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.WearSwitchRow
import com.cyhao.realmebuds.ui.theme.ConnectedGreen
import kotlinx.coroutines.delay

/** 「查找耳机」反馈时长（毫秒）。 */
private const val FindBudsFeedbackMillis = 3_000L

/**
 * 主页：设备卡（电量圆环）+ 降噪快捷切换 + EQ/游戏模式/按键设置/设备信息 + 更多。
 * 设计来源：用户提供的 3 页 Wear OS 设计稿；配色为 Material Design 3 蓝色系。
 */
@Composable
fun HomeScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    var finding by remember { mutableStateOf(false) }

    LaunchedEffect(finding) {
        if (finding) {
            delay(FindBudsFeedbackMillis)
            finding = false
        }
    }

    val connected = state.connected
    val connecting = state.connection == ConnectionState.Connecting
    val canControl = connected || state.demo
    val caps = state.capabilities
    val firmwareLabel = if (state.firmware.isBlank()) stringResource(R.string.common_unknown) else state.firmware

    ScreenScaffold(scrollState = listState) { padding ->
        // TimeText 由 AppScaffold 绘制在顶部，这里额外留出顶部间距避免与标题重叠
        val listPadding = PaddingValues(
            start = 10.dp,
            top = padding.calculateTopPadding() + 20.dp,
            end = 10.dp,
            bottom = padding.calculateBottomPadding(),
        )
        ScalingLazyColumn(state = listState, contentPadding = listPadding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                DeviceCard(
                    state = state,
                    connected = connected,
                    connecting = connecting,
                    showCase = caps.caseBattery,
                )
            }

            if (!connected) {
                item {
                    Button(
                        onClick = { navController.navigate(BudsRoutes.SCAN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        enabled = !connecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        label = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.home_connect_prompt), maxLines = 1)
                        },
                    )
                }
            } else {
                item {
                    AncSegmentedSelector(
                        current = state.ancMode,
                        enabled = canControl,
                        onSelect = { mode -> vm.setAncMode(mode) },
                    )
                }
                item {
                    WearNavRow(
                        iconRes = R.drawable.ic_tune,
                        label = stringResource(R.string.home_eq_mode),
                        secondaryLabel = stringResource(state.eqPreset.labelRes),
                        enabled = canControl,
                        onClick = { navController.navigate(BudsRoutes.EQUALIZER) },
                    )
                }
                item {
                    WearSwitchRow(
                        checked = state.gameMode,
                        onCheckedChange = { vm.setGameMode(it) },
                        label = stringResource(R.string.game_mode_title),
                        enabled = canControl,
                    )
                }
                item {
                    WearNavRow(
                        iconRes = R.drawable.ic_grid,
                        label = stringResource(R.string.home_key_settings),
                        enabled = canControl,
                        onClick = { navController.navigate(BudsRoutes.GESTURES) },
                    )
                }
                item {
                    WearNavRow(
                        iconRes = R.drawable.ic_more,
                        label = stringResource(R.string.info_title),
                        secondaryLabel = firmwareLabel,
                        onClick = { navController.navigate(BudsRoutes.DEVICE_INFO) },
                    )
                }
                if (caps.findBuds) {
                    item {
                        WearNavRow(
                            iconRes = R.drawable.ic_find,
                            label = stringResource(
                                if (finding) R.string.find_buds_stop else R.string.find_buds_title
                            ),
                            secondaryLabel = if (finding) {
                                stringResource(R.string.find_buds_ringing)
                            } else {
                                stringResource(R.string.find_buds_desc)
                            },
                            showChevron = false,
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
                item { WearSectionHeader(stringResource(R.string.home_section_more)) }
                if (caps.dualConnection) {
                    item {
                        WearSwitchRow(
                            checked = state.dualConnection,
                            onCheckedChange = { vm.setDualConnection(it) },
                            label = stringResource(R.string.info_dual_connection),
                            enabled = canControl,
                        )
                    }
                }
                item {
                    WearNavRow(
                        iconRes = R.drawable.ic_bluetooth_off,
                        label = stringResource(R.string.home_disconnect),
                        showChevron = false,
                        onClick = { vm.disconnect() },
                    )
                }
            }

            item {
                WearNavRow(
                    iconRes = R.drawable.ic_menu,
                    label = stringResource(R.string.home_settings),
                    secondaryLabel = "v" + AppInfo.VERSION,
                    onClick = { navController.navigate(BudsRoutes.ABOUT) },
                )
            }
        }
    }
}

/** 设备卡：耳机图标 + 名称/未连接 + 连接状态 + L/R/Case 电量圆环。 */
@Composable
private fun DeviceCard(
    state: BudsState,
    connected: Boolean,
    connecting: Boolean,
    showCase: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_buds),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                if (state.name.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            ConnectionStatusRow(connected = connected, connecting = connecting)
            Spacer(Modifier.height(14.dp))
            BatteryRings(battery = state.battery, showCase = showCase)
        }
    }
}

/**
 * 降噪快捷切换：跟随点击平滑滑动的选中指示块（弹簧动画 + 轻微拉伸），
 * 不做真正的毛玻璃/背景模糊，保持简洁。
 */
@Composable
private fun AncSegmentedSelector(
    current: AncMode,
    enabled: Boolean,
    onSelect: (AncMode) -> Unit,
) {
    val options = listOf(AncMode.NoiseCancellation, AncMode.Off, AncMode.Transparency)
    val selectedIndex = options.indexOf(current).coerceAtLeast(0)
    val anim = remember { Animatable(selectedIndex.toFloat()) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(selectedIndex) {
        anim.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.60f, stiffness = 500f),
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        val segmentWidth = maxWidth / options.size
        // 滑动的选中块：弹簧回弹 + 运动中轻微横向拉伸
        Box(
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .graphicsLayer {
                    translationX = anim.value * segmentWidth.toPx()
                    val v = (anim.velocity / 9000f).coerceIn(-1f, 1f)
                    scaleX = 1f + v * 0.08f
                    scaleY = 1f - abs(v) * 0.04f
                }
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(modifier = Modifier.matchParentSize()) {
            options.forEachIndexed { index, mode ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "ancTextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = enabled) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(mode)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}