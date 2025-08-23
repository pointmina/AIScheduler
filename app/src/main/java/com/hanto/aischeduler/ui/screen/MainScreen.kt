package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import com.hanto.aischeduler.ui.viewModel.AppScreen
import com.hanto.aischeduler.ui.viewModel.ScheduleUiState
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

    // 앱 시작시 저장된 오늘 스케줄 불러오기
    LaunchedEffect(Unit) {
        viewModel.loadTodaySchedule()
    }

    // 👇 화면 분기 처리 추가
    when (uiState.currentScreen) {
        AppScreen.HOME -> {
            HomeContent(
                uiState = uiState,
                newTask = newTask,
                onTaskTextChange = { newTask = it },
                onAddTask = {
                    if (newTask.isNotBlank()) {
                        viewModel.addTask(newTask)
                        newTask = ""
                    }
                },
                onDeleteTask = { viewModel.removeTask(it) },
                onStartTimeChange = { viewModel.updateStartTime(it) },
                onEndTimeChange = { viewModel.updateEndTime(it) },
                onGenerateSchedule = { viewModel.generateSchedule() },
                onNavigateToSavedSchedules = { viewModel.navigateToSavedSchedules() },
                onClearError = { viewModel.clearError() }
            )
        }

        AppScreen.SCHEDULE_RESULT -> {
            ScheduleResultScreen(
                tasks = uiState.generatedSchedule,
                isEditMode = uiState.isEditMode,
                errorMessage = uiState.errorMessage,
                onBack = { viewModel.navigateToHome() }, // 👈 navigateToHome으로 변경
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
        }

        AppScreen.SAVED_SCHEDULES -> {
            SavedSchedulesScreen(
                savedSchedules = uiState.savedSchedules,
                onBack = { viewModel.navigateToHome() },
                onScheduleClick = { scheduleId ->
                    viewModel.loadSavedSchedule(scheduleId)
                },
                onNewSchedule = { viewModel.navigateToHome() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: ScheduleUiState,
    newTask: String,
    onTaskTextChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onGenerateSchedule: () -> Unit,
    onNavigateToSavedSchedules: () -> Unit,
    onClearError: () -> Unit
) {
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
                actions = {
                    // 저장된 계획 보기 버튼
                    IconButton(onClick = onNavigateToSavedSchedules) {
                        Text(
                            "📋",
                            fontSize = 20.sp
                        )
                    }
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
                    onStartTimeChange = onStartTimeChange,
                    onEndTimeChange = onEndTimeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                )

                // 할 일 입력 카드
                TaskInputCard(
                    taskText = newTask,
                    onTaskTextChange = onTaskTextChange,
                    onAddTask = onAddTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.20f)
                )

                // 할 일 목록 카드
                TaskListCard(
                    tasks = uiState.tasks,
                    onDeleteTask = onDeleteTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                )

                // AI 생성 버튼
                AIGenerateButton(
                    onClick = onGenerateSchedule,
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
                            onDismiss = onClearError,
                            onRetry = if (errorType != ErrorType.INFO) {
                                onGenerateSchedule
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