package com.cyhao.realmebuds.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.data.ConnectionState
import com.cyhao.realmebuds.data.ScannedDevice
import com.cyhao.realmebuds.ui.components.WearListButton
import com.cyhao.realmebuds.ui.components.WearSectionHeader
import com.cyhao.realmebuds.ui.components.CrownScrollEffect

/** 按系统版本选择运行时权限：Android 12+ 蓝牙权限；Android 11 及以下为定位权限。 */
private val BlePermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

private fun hasBlePermissions(context: Context): Boolean = BlePermissions.all { permission ->
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * 扫描设备：进入即请求蓝牙权限并开始扫描，结果列表点击连接，连接成功后自动返回。
 */
@Composable
fun ScanScreen(vm: BudsViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val view = LocalView.current
    val state by vm.state.collectAsState()
    val demo by vm.demoMode.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val results by vm.scanResults.collectAsState()
    val listState = rememberScalingLazyListState()
    CrownScrollEffect(listState)

    var hasPermission by remember { mutableStateOf(hasBlePermissions(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    var connectingAddress by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = hasBlePermissions(context)
        permissionDenied = !hasPermission
    }

    // 首次进入：演示模式无需权限，直接开始扫描；否则请求权限后扫描
    LaunchedEffect(demo, hasPermission) {
        when {
            demo -> vm.startScan()
            hasPermission -> vm.startScan()
            else -> permissionLauncher.launch(BlePermissions)
        }
    }

    // 离开页面时停止扫描
    DisposableEffect(Unit) {
        onDispose { vm.stopScan() }
    }

    // 扫描期间保持屏幕常亮
    DisposableEffect(scanning) {
        if (scanning) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // 从系统设置返回时刷新权限状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasBlePermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 连接成功自动返回主页
    LaunchedEffect(state.connection) {
        when (state.connection) {
            ConnectionState.Connected -> {
                vm.stopScan()
                navController.popBackStack()
            }

            ConnectionState.Disconnected -> connectingAddress = null
            ConnectionState.Connecting -> Unit
        }
    }

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.scan_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!demo && !hasPermission) {
                item {
                    Text(
                        text = stringResource(R.string.scan_permission_needed),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
                item {
                    WearListButton(
                        iconRes = R.drawable.ic_bluetooth,
                        label = stringResource(R.string.scan_grant_permission),
                        onClick = { permissionLauncher.launch(BlePermissions) },
                    )
                }
                if (permissionDenied) {
                    item {
                        Text(
                            text = stringResource(R.string.scan_permission_denied_hint),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            } else {
                if (scanning) {
                    item { ScanningIndicator() }
                }
                if (state.connection == ConnectionState.Connecting) {
                    item { ConnectingIndicator() }
                }
                if (results.isEmpty() && !scanning) {
                    item {
                        Text(
                            text = stringResource(R.string.scan_empty),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.scan_hint),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
                if (results.isNotEmpty()) {
                    item { WearSectionHeader(stringResource(R.string.scan_section_results)) }
                    items(results, key = { it.address }) { device ->
                        ScanResultRow(
                            device = device,
                            connecting = connectingAddress == device.address,
                            enabled = connectingAddress == null &&
                                state.connection != ConnectionState.Connecting,
                            onClick = {
                                connectingAddress = device.address
                                vm.stopScan()
                                vm.connect(device)
                            },
                        )
                    }
                }
                item {
                    WearListButton(
                        iconRes = R.drawable.ic_refresh,
                        label = stringResource(R.string.scan_rescan),
                        enabled = !scanning && state.connection != ConnectionState.Connecting,
                        onClick = {
                            if (hasPermission || demo) vm.startScan()
                        },
                    )
                }
                if (demo) {
                    item {
                        Text(
                            text = stringResource(R.string.scan_demo_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

/** 扫描中的转圈 + 提示。 */
@Composable
private fun ScanningIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.scan_scanning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

/** 连接中的转圈 + 提示。 */
@Composable
private fun ConnectingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.scan_connecting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 扫描结果行：真我设备用耳机图标 + 品牌标记。 */
@Composable
private fun ScanResultRow(
    device: ScannedDevice,
    connecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    WearListButton(
        iconRes = if (device.realme) R.drawable.ic_buds else R.drawable.ic_bluetooth,
        label = device.name.ifBlank { device.address },
        secondaryLabel = when {
            connecting -> stringResource(R.string.scan_connecting)
            device.realme -> stringResource(R.string.scan_realme_rssi, device.rssi)
            else -> stringResource(R.string.scan_rssi, device.rssi)
        },
        enabled = enabled,
        onClick = onClick,
    )
}
