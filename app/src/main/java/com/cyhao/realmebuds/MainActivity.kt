package com.cyhao.realmebuds

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyhao.realmebuds.data.BudsViewModel
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
}
