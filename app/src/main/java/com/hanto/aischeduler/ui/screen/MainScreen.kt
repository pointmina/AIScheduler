package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hanto.aischeduler.ui.components.AIGenerateButton
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.components.TaskInputCard
import com.hanto.aischeduler.ui.components.TaskListCard
import com.hanto.aischeduler.ui.components.TimeSettingCard
import com.hanto.aischeduler.ui.theme.AISchedulerTheme
import com.hanto.aischeduler.ui.theme.AppColors
import com.hanto.aischeduler.ui.viewModel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // ViewModel 상태 구독
    val uiState by viewModel.uiState.collectAsState()
    var newTask by remember { mutableStateOf("") }

    // 스케줄이 생성되면 결과 화면으로 이동
    if (uiState.isScheduleGenerated) {
        ScheduleResultScreen(
            tasks = uiState.generatedSchedule,
            onBack = { viewModel.backToInput() },
            onSave = {
                // TODO: 저장 기능
            },
            onSetAlarm = {
                // TODO: 알림 설정 기능
            }
        )
        return
    }

    SolidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 상단 앱바
            TopAppBar(
                title = {
                    Text(
                        "오늘의 할일!",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // 고정 비율 레이아웃 (스크롤 제거)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 날짜 표시
                Text(
                    text = "📅 ${getCurrentDate()}",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // 시간 설정 카드
                TimeSettingCard(
                    startTime = uiState.startTime,
                    endTime = uiState.endTime,
                    onStartTimeChange = { viewModel.updateStartTime(it) },
                    onEndTimeChange = { viewModel.updateEndTime(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                )

                // 할 일 입력 카드
                TaskInputCard(
                    taskText = newTask,
                    onTaskTextChange = { newTask = it },
                    onAddTask = {
                        if (newTask.isNotBlank()) {
                            viewModel.addTask(newTask)
                            newTask = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.20f)
                )

                // 할 일 목록 카드
                TaskListCard(
                    tasks = uiState.tasks,
                    onDeleteTask = { taskToDelete ->
                        viewModel.removeTask(taskToDelete)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                )

                // AI 생성 버튼
                AIGenerateButton(
                    onClick = {
                        viewModel.generateSchedule()
                    },
                    isLoading = uiState.isLoading,
                    enabled = uiState.tasks.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.1f)
                )

                // 에러 메시지 및 안내
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 에러 메시지
                    uiState.errorMessage?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.Warning.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "⚠️ $error",
                                modifier = Modifier.padding(12.dp),
                                color = AppColors.Warning,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 할 일이 없을 때 안내
                    if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.Warning.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "⚠️ 할 일을 하나 이상 추가해주세요",
                                modifier = Modifier.padding(12.dp),
                                color = AppColors.Warning,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat(" M월 d일 EEEE", Locale.KOREAN)
    return formatter.format(Date())
}

// Preview는 ViewModel 없이 기본 화면만 표시
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    AISchedulerTheme {
        SolidBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📅 ${getCurrentDate()}",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TimeSettingCard(
                    startTime = "09:00",
                    endTime = "18:00",
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    modifier = Modifier.weight(0.15f)
                )
                TaskInputCard(
                    taskText = "",
                    onTaskTextChange = {},
                    onAddTask = {},
                    modifier = Modifier.weight(0.15f)
                )
                TaskListCard(
                    tasks = listOf("프로젝트 완성하기", "점심 약속", "운동하기"),
                    onDeleteTask = {},
                    modifier = Modifier.weight(0.35f)
                )
                AIGenerateButton(
                    onClick = {},
                    isLoading = false,
                    enabled = true,
                    modifier = Modifier.weight(0.1f)
                )
                Spacer(modifier = Modifier.weight(0.2f))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenMobilePreview() {
    AISchedulerTheme {
        MainScreenPreview()
    }
}