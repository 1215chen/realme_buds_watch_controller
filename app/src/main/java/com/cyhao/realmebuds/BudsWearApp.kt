package com.cyhao.realmebuds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.ui.screens.AboutScreen
import com.cyhao.realmebuds.ui.screens.DeviceInfoScreen
import com.cyhao.realmebuds.ui.screens.EqualizerScreen
import com.cyhao.realmebuds.ui.screens.GesturesScreen
import com.cyhao.realmebuds.ui.screens.HomeScreen
import com.cyhao.realmebuds.ui.screens.NoiseControlScreen
import com.cyhao.realmebuds.ui.screens.ScanScreen

/** 页面路由常量。 */
object BudsRoutes {
    const val HOME = "home"
    const val NOISE = "noise"
    const val EQUALIZER = "equalizer"
    const val GESTURES = "gestures"
    const val DEVICE_INFO = "info"
    const val SCAN = "scan"
    const val ABOUT = "about"
}

@Composable
fun BudsWearApp(vm: BudsViewModel) {
    val navController = rememberSwipeDismissableNavController()
    val context = LocalContext.current

    // 页面内提示浮层（Wear OS 上 Toast 不可靠，统一用底部浮层）
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        vm.messages.collect { resId -> message = context.resources.getString(resId) }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2_500)
            message = null
        }
    }

    AppScaffold {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = BudsRoutes.HOME,
        ) {
            composable(BudsRoutes.HOME) { HomeScreen(vm, navController) }
            composable(BudsRoutes.NOISE) { NoiseControlScreen(vm, navController) }
            composable(BudsRoutes.EQUALIZER) { EqualizerScreen(vm, navController) }
            composable(BudsRoutes.GESTURES) { GesturesScreen(vm, navController) }
            composable(BudsRoutes.DEVICE_INFO) { DeviceInfoScreen(vm, navController) }
            composable(BudsRoutes.SCAN) { ScanScreen(vm, navController) }
            composable(BudsRoutes.ABOUT) { AboutScreen(vm, navController) }
        }

        val current = message
        if (current != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = current,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
