package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.AncLevel
import com.cyhao.realmebuds.data.AncMode
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.data.labelRes
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.CrownScrollEffect

/**
 * 降噪控制：关闭 / 通透 / 降噪 单选；选中降噪时追加 4 档强度。
 * 所有变更立即写回耳机（失败提示由仓库层 Toast 统一处理）。
 */
@Composable
fun NoiseControlScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    CrownScrollEffect(listState)
    val canControl = state.connected || state.demo

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.noise_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { NoiseSummaryCard(mode = state.ancMode, level = state.ancLevel) }
            if (!canControl) {
                item {
                    Text(
                        text = stringResource(R.string.common_need_connection),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
            item { WearSectionHeader(stringResource(R.string.noise_mode_title)) }
            items(AncMode.entries) { mode ->
                TextToggleButton(
                    checked = state.ancMode == mode,
                    onCheckedChange = { checked -> if (checked) vm.setAncMode(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canControl,
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (state.ancMode == AncMode.NoiseCancellation) {
                item { WearSectionHeader(stringResource(R.string.noise_level_title)) }
                items(AncLevel.entries) { level ->
                    TextToggleButton(
                        checked = state.ancLevel == level,
                        onCheckedChange = { checked -> if (checked) vm.setAncLevel(level) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canControl,
                    ) {
                        Text(
                            text = stringResource(level.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.noise_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** 当前降噪模式概览卡。 */
@Composable
private fun NoiseSummaryCard(mode: AncMode, level: AncLevel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_noise),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (mode == AncMode.NoiseCancellation) {
                        stringResource(
                            R.string.home_anc_summary,
                            stringResource(mode.labelRes),
                            stringResource(level.labelRes),
                        )
                    } else {
                        stringResource(mode.labelRes)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(noiseDescRes(mode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

/** 降噪模式说明文案。 */
private fun noiseDescRes(mode: AncMode): Int = when (mode) {
    AncMode.Off -> R.string.noise_desc_off
    AncMode.Transparency -> R.string.noise_desc_transparency
    AncMode.NoiseCancellation -> R.string.noise_desc_cancellation
}
