package com.hanto.aischeduler.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanto.aischeduler.data.repository.ScheduleRepository
import com.hanto.aischeduler.data.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ScheduleUiState(
    val tasks: List<String> = emptyList(),
    val generatedSchedule: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isScheduleGenerated: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "18:00"
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    fun addTask(task: String) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.add(task)
        _uiState.value = _uiState.value.copy(
            tasks = currentTasks,
            errorMessage = null
        )
    }

    fun removeTask(task: String) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.remove(task)
        _uiState.value = _uiState.value.copy(tasks = currentTasks)
    }

    fun updateStartTime(time: String) {
        _uiState.value = _uiState.value.copy(startTime = time)
    }

    fun updateEndTime(time: String) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun generateSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val schedule = scheduleRepository.generateSchedule(
                    tasks = _uiState.value.tasks,
                    date = getTodayDateString(),
                    startTime = _uiState.value.startTime,
                    endTime = _uiState.value.endTime
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedSchedule = schedule,
                    isScheduleGenerated = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "일정 생성 실패: ${e.message}"
                )
            }
        }
    }

    fun clearSchedule() {
        _uiState.value = _uiState.value.copy(
            generatedSchedule = emptyList(),
            tasks = emptyList(),
            isScheduleGenerated = false,
            errorMessage = null
        )
    }

    fun backToInput() {
        _uiState.value = _uiState.value.copy(
            isScheduleGenerated = false,
            errorMessage = null
        )
    }

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}