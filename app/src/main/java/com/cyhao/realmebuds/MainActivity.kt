package com.cyhao.realmebuds

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyhao.realmebuds.data.BudsViewModel
import com.cyhao.realmebuds.ui.components.CrownInput
import com.cyhao.realmebuds.ui.theme.BudsWearTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: BudsViewModel = viewModel(factory = BudsViewModel.factory(applicationContext))

            BudsWearTheme {
                BudsWearApp(vm)
            }
        }
    }

    /**
     * 表冠按键适配：部分手表（ColorOS Watch 等）把旋转表冠上报为 DPAD/PAGE 按键，
     * 统一转成列表滚动步数交给页面。
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Wear OS 4+（API 33+）的旋转表冠由系统以 RotaryScrollEvent 派发，列表已原生支持；
        // 这里的按键兜底只给 ColorOS Watch / 旧系统用，避免影响 Wear OS 体验。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
                -> {
                    CrownInput.emit(-1)
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_NAVIGATE_NEXT,
                -> {
                    CrownInput.emit(1)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 表冠滚动适配：部分系统把旋转表冠上报为 ACTION_SCROLL 通用事件。
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.dispatchGenericMotionEvent(event)
        }
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val scroll = if (vScroll != 0f) vScroll else event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (scroll != 0f) {
                CrownInput.fromScrollAxis(scroll)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
