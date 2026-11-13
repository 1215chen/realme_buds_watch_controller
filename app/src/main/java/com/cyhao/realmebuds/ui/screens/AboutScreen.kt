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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.cyhao.realmebuds.AppInfo
import com.cyhao.realmebuds.R
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.ui.components.WearSwitchRow

/**
 * 关于：应用名/版本、当前设备型号、演示模式开关、免责声明。
 */
@Composable
fun AboutScreen(vm: BudsViewModel, navController: NavHostController) {
    val demo by vm.demoMode.collectAsState()
    val state by vm.state.collectAsState()
    val listState = rememberScalingLazyListState()
    val watchModel = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Wear OS"

    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(state = listState, contentPadding = padding) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.about_version) + " " + AppInfo.VERSION,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.about_device_model, watchModel),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.name.isBlank() && state.model.isBlank()) {
                            stringResource(R.string.about_earbud_none)
                        } else {
                            stringResource(
                                R.string.about_earbud_model,
                                state.model.ifBlank { state.name },
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                WearSwitchRow(
                    checked = demo,
                    onCheckedChange = { vm.setDemoMode(it) },
                    label = stringResource(R.string.about_demo_mode),
                    secondaryLabel = stringResource(R.string.about_demo_desc),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.about_disclaimer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                )
            }
        }
    }
}
