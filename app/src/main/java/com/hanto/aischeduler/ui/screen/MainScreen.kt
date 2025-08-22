package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hanto.aischeduler.ui.components.AIGenerateButton
import com.hanto.aischeduler.ui.components.ErrorDisplay
import com.hanto.aischeduler.ui.components.ErrorType
import com.hanto.aischeduler.ui.components.LoadingIndicator
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.components.TaskInputCard
import com.hanto.aischeduler.ui.components.TaskListCard
import com.hanto.aischeduler.ui.components.TimeSettingCard
import com.hanto.aischeduler.ui.viewModel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    var newTask by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadTodaySchedule()
    }

    // 스케줄이 생성되면 결과 화면으로 이동
    if (uiState.isScheduleGenerated) {
        ScheduleResultScreen(
            tasks = uiState.generatedSchedule,
            isEditMode = uiState.isEditMode,
            errorMessage = uiState.errorMessage,
            onBack = { viewModel.backToInput() },
            onToggleEditMode = { viewModel.toggleEditMode() },
            onReorderTasks = { fromIndex, toIndex ->
                viewModel.reorderTasks(fromIndex, toIndex)
            },
            onUpdateTaskTime = { taskId, startTime, endTime ->
                viewModel.updateTaskTime(taskId, startTime, endTime)
            },
            onSplitSchedule = { viewModel.splitSchedule() },
            onExtendEndTime = { viewModel.extendEndTime() },
            onClearError = { viewModel.clearError() },
            onSave = {
                viewModel.saveCurrentSchedule("오늘의 계획")
            },
            onSetAlarm = {
                // TODO: 알림 설정 기능
            },
            onTaskCompletionToggle = { taskId, isCompleted ->
                viewModel.updateTaskCompletion(taskId, isCompleted)
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
                    // 로딩 상태
                    if (uiState.isLoading) {
                        LoadingIndicator(
                            message = "AI가 최적의 스케줄을 만들고 있어요..."
                        )
                    }

                    // 에러 메시지
                    uiState.errorMessage?.let { error ->
                        val errorType = when {
                            error.contains("인터넷") || error.contains("네트워크") -> ErrorType.WARNING
                            error.contains("API") || error.contains("서버") -> ErrorType.ERROR
                            else -> ErrorType.INFO
                        }

                        ErrorDisplay(
                            message = error,
                            type = errorType,
                            onDismiss = { viewModel.clearError() },
                            onRetry = if (errorType != ErrorType.INFO) {
                                { viewModel.generateSchedule() }
                            } else null
                        )
                    }

                    // 안내 메시지
                    if (uiState.tasks.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                        ErrorDisplay(
                            message = "할 일을 추가하고 AI 스케줄을 생성해보세요!",
                            type = ErrorType.INFO
                        )
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