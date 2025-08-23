package com.hanto.aischeduler.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.hanto.aischeduler.ui.screen.MainScreen
import com.hanto.aischeduler.ui.theme.AISchedulerTheme
import com.hanto.aischeduler.ui.viewModel.AppScreen
import com.hanto.aischeduler.ui.viewModel.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AISchedulerTheme {
                val viewModel: ScheduleViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                var lastBackPressTime by remember { mutableLongStateOf(0L) }

                // 뒤로가기 처리
                BackHandler {
                    when (uiState.currentScreen) {
                        AppScreen.HOME -> {
                            val currentTime = System.currentTimeMillis()

                            if (currentTime - lastBackPressTime < 2000) {
                                // 2초 내에 두 번 누르면 앱 종료
                                finish()
                            } else {
                                lastBackPressTime = currentTime
                                Toast.makeText(
                                    this@MainActivity,
                                    "한 번 더 누르면 앱이 종료됩니다",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        AppScreen.SCHEDULE_RESULT -> {
                            // 스케줄 결과 화면 → 홈으로
                            viewModel.navigateToHome()
                        }

                        AppScreen.SAVED_SCHEDULES -> {
                            // 저장된 계획 목록 → 홈으로
                            viewModel.navigateToHome()
                        }
                    }
                }

                MainScreen()
            }
        }
    }
}