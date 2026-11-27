package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.data.EqPreset
import com.cyhao.realmebuds.data.labelRes
import com.cyhao.realmebuds.ui.components.CrownScrollEffect

/**
 * 音效：6 个预设单选 + 频响示意图 + 说明。
 */
@Composable
fun EqualizerScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    CrownScrollEffect(listState)
    val canControl = state.connected || state.demo

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.eq_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { EqualizerSummaryCard(preset = state.eqPreset) }
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
            items(EqPreset.entries) { preset ->
                TextToggleButton(
                    checked = state.eqPreset == preset,
                    onCheckedChange = { checked -> if (checked) vm.setEq(preset) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canControl,
                ) {
                    Text(
                        text = stringResource(preset.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.eq_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** 当前预设概览：预设名 + 频响柱状示意图。 */
@Composable
private fun EqualizerSummaryCard(preset: EqPreset) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_equalizer),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(preset.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.eq_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        EqualizerBars(
            preset = preset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(40.dp),
        )
    }
}

/** 频响柱状图：5 根竖向柱，按预设高度不同。 */
@Composable
private fun EqualizerBars(preset: EqPreset, modifier: Modifier = Modifier) {
    val levels = remember(preset) { presetLevels(preset) }
    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val barCount = levels.size
        if (barCount == 0 || size.width <= 0f) return@Canvas
        val gap = size.width * 0.05f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        levels.forEachIndexed { index, level ->
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = radius,
            )
            val barHeight = size.height * level.coerceIn(0.12f, 1f)
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

/** 各预设的 5 段频响高度（0f-1f），仅用于 UI 示意。 */
private fun presetLevels(preset: EqPreset): List<Float> = when (preset) {
    EqPreset.VocalPure -> listOf(0.35f, 0.58f, 0.85f, 0.60f, 0.36f)
    EqPreset.Original -> listOf(0.50f, 0.50f, 0.50f, 0.50f, 0.50f)
    EqPreset.BassPure -> listOf(0.85f, 0.70f, 0.52f, 0.40f, 0.34f)
    EqPreset.BassDeep -> listOf(0.95f, 0.82f, 0.58f, 0.36f, 0.28f)
    EqPreset.Bright -> listOf(0.32f, 0.40f, 0.55f, 0.75f, 0.90f)
    EqPreset.Dynamic -> listOf(0.72f, 0.42f, 0.65f, 0.42f, 0.78f)
}
