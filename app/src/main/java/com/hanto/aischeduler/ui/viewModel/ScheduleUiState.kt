package com.hanto.aischeduler.ui.viewModel

import com.hanto.aischeduler.data.model.Task

data class ScheduleUiState(
    val tasks: List<String> = emptyList(),
    val generatedSchedule: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isScheduleGenerated: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val isEditMode: Boolean = false,
    val currentScreen: AppScreen = AppScreen.HOME,
    val savedSchedules: List<SavedScheduleItem> = emptyList()
)

// 앱 화면 열거형
enum class AppScreen {
    HOME,           // 메인 화면 (할 일 입력)
    SCHEDULE_RESULT,// 스케줄 결과 화면
    SAVED_SCHEDULES // 저장된 계획 목록 화면
}

// 저장된 스케줄 아이템
data class SavedScheduleItem(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val createdAt: Long
) {
    val completionRate: Float
        get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val isCompleted: Boolean
        get() = completedTasks == totalTasks && totalTasks > 0
}