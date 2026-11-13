package com.cyhao.realmebuds.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.BudsRoutes
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.WearSwitchRow

/**
 * 设备信息：型号/固件/蓝牙地址 + 双设备连接、入耳检测开关 + 恢复出厂设置（二次确认）。
 */
@Composable
fun DeviceInfoScreen(vm: BudsViewModel, navController: NavHostController) {
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    var showResetConfirm by remember { mutableStateOf(false) }
    val canControl = state.connected || state.demo
    val unknown = stringResource(R.string.common_unknown)
    val modelLabel = if (state.model.isBlank()) unknown else state.model
    val firmwareLabel = if (state.firmware.isBlank()) unknown else state.firmware
    val addressLabel = if (state.address.isBlank()) unknown else state.address

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.info_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { WearSectionHeader(stringResource(R.string.info_basic_title)) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    InfoRow(
                        label = stringResource(R.string.info_model),
                        value = modelLabel,
                    )
                    InfoRow(
                        label = stringResource(R.string.info_firmware),
                        value = firmwareLabel,
                    )
                    InfoRow(
                        label = stringResource(R.string.info_address),
                        value = addressLabel,
                    )
                }
            }
            val caps = state.capabilities
            if (caps.dualConnection || caps.inEarDetection) {
                item { WearSectionHeader(stringResource(R.string.info_switch_title)) }
                if (caps.dualConnection) {
                    item {
                        WearSwitchRow(
                            checked = state.dualConnection,
                            onCheckedChange = { vm.setDualConnection(it) },
                            label = stringResource(R.string.info_dual_connection),
                            secondaryLabel = stringResource(R.string.info_dual_connection_desc),
                            enabled = canControl,
                        )
                    }
                }
                if (caps.inEarDetection) {
                    item {
                        WearSwitchRow(
                            checked = state.inEarDetection,
                            onCheckedChange = { vm.setInEarDetection(it) },
                            label = stringResource(R.string.info_in_ear),
                            secondaryLabel = stringResource(R.string.info_in_ear_desc),
                            enabled = canControl,
                        )
                    }
                }
            } else if (canControl) {
                item {
                    Text(
                        text = stringResource(R.string.info_capability_unknown),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            item {
                // 恢复出厂设置：错误色强调，点击后二次确认
                Button(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canControl,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        secondaryContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.info_factory_reset),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = stringResource(R.string.info_factory_reset_desc),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }

        }
    }

    if (showResetConfirm) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showResetConfirm = false },
            title = { Text(text = stringResource(R.string.info_factory_reset_confirm)) },
            text = { Text(text = stringResource(R.string.info_factory_reset_confirm_desc)) },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        showResetConfirm = false
                        vm.factoryReset()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                )
            },
            dismissButton = {
                AlertDialogDefaults.DismissButton(onClick = { showResetConfirm = false })
            },
        )
    }
}

/** 单条信息行：小号标签 + 常规值。 */
@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
