package com.hanto.aischeduler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hanto.aischeduler.ui.screen.MainScreen
import com.hanto.aischeduler.ui.theme.AISchedulerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AISchedulerTheme {
                MainScreen()
            }
        }
    }
}