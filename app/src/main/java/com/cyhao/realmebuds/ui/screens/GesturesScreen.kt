package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.data.EarSide
import com.cyhao.realmebuds.data.GestureAction
import com.cyhao.realmebuds.data.GestureType
import com.cyhao.realmebuds.data.labelRes
import com.cyhao.realmebuds.ui.components.WearListButton
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.CrownScrollEffect

/** 正在编辑的手势：哪只耳 + 哪种手势。 */
private data class GestureEditTarget(val side: EarSide, val type: GestureType)

/**
 * 触控手势：左右耳分别配置双击 / 三击 / 长按（两只耳可以不同），点击弹出动作选择列表。
 */
@Composable
fun GesturesScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    CrownScrollEffect(listState)
    var editing by remember { mutableStateOf<GestureEditTarget?>(null) }
    val canControl = state.connected || state.demo

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.gesture_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            EarSide.entries.forEach { side ->
                item { WearSectionHeader(stringResource(side.labelRes)) }
                items(GestureType.entries) { type ->
                    WearListButton(
                        iconRes = R.drawable.ic_gesture,
                        label = stringResource(type.labelRes),
                        secondaryLabel = stringResource(state.gesture(side, type).labelRes),
                        enabled = canControl,
                        onClick = { editing = GestureEditTarget(side, type) },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.gesture_hint),
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

    val target = editing
    if (target != null) {
        val currentAction = state.gesture(target.side, target.type)
        AlertDialog(
            visible = true,
            onDismissRequest = { editing = null },
            title = {
                Text(
                    text = stringResource(
                        R.string.gesture_edit_title,
                        stringResource(target.side.labelRes),
                        stringResource(target.type.labelRes),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            content = {
                items(GestureAction.entries) { action ->
                    TextToggleButton(
                        checked = currentAction == action,
                        onCheckedChange = { checked ->
                            if (checked) {
                                vm.setGesture(target.side, target.type, action)
                                editing = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(action.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                item {
                    Button(
                        onClick = { editing = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = stringResource(R.string.common_cancel), maxLines = 1) },
                    )
                }
            },
        )
    }
}
