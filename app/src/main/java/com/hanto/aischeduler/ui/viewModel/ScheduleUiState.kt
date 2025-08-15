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
    val isEditMode: Boolean = false // 편집 모드 상태
)