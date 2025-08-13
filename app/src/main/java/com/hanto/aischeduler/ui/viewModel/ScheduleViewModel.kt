package com.hanto.aischeduler.ui.viewModel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanto.aischeduler.data.repository.ScheduleRepository
import com.hanto.aischeduler.data.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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

    // 휴식시간 설정 함수들 추가
    fun toggleBreaks() {
        _uiState.update {
            it.copy(includeBreaks = !it.includeBreaks)
        }
    }

    fun updateBreakDuration(duration: Int) {
        _uiState.update {
            it.copy(breakDuration = duration)
        }
    }

    // 편집 모드 토글
    fun toggleEditMode() {
        _uiState.update {
            it.copy(isEditMode = !it.isEditMode)
        }
    }

    // 작업 순서 변경 (시간 재계산 포함)
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()

        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= currentTasks.size || toIndex >= currentTasks.size) {
            return
        }

        // 실제 작업들만 필터링 (휴식시간, 식사시간 제외)
        val actualTasks = currentTasks.filter {
            !it.title.contains("휴식") &&
                    !it.title.contains("점심") &&
                    !it.title.contains("저녁식사") &&
                    !it.title.contains("커피")
        }.toMutableList()

        if (fromIndex >= actualTasks.size || toIndex >= actualTasks.size) {
            return
        }

        // 순서 변경
        val movedTask = actualTasks.removeAt(fromIndex)
        actualTasks.add(toIndex, movedTask)

        // 시간 재계산해서 새로운 스케줄 생성
        val reorderedSchedule = recalculateScheduleTimes(
            tasks = actualTasks.map { it.title },
            date = _uiState.value.generatedSchedule.firstOrNull()?.date ?: "",
            startTime = _uiState.value.startTime,
            endTime = _uiState.value.endTime
        )

        _uiState.update {
            it.copy(generatedSchedule = reorderedSchedule)
        }
    }

    // 개별 작업 시간 수정
    fun updateTaskTime(taskId: String, newStartTime: String, newEndTime: String) {
        val updatedTasks = _uiState.value.generatedSchedule.map { task ->
            if (task.id == taskId) {
                task.copy(startTime = newStartTime, endTime = newEndTime)
            } else {
                task
            }
        }

        _uiState.update {
            it.copy(generatedSchedule = updatedTasks)
        }
    }

    // 시간 재계산 함수
    @SuppressLint("DefaultLocale")
    private fun recalculateScheduleTimes(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): List<Task> {
        val scheduleList = mutableListOf<Task>()

        val startTimeParts = startTime.split(":")
        var currentHour = startTimeParts[0].toInt()
        var currentMinute = startTimeParts[1].toInt()

        val endTimeParts = endTime.split(":")
        val endHour = endTimeParts[0].toInt()
        val endMinute = endTimeParts[1].toInt()
        val totalEndMinutes = endHour * 60 + endMinute

        val isEveningTime = currentHour >= 18

        tasks.forEachIndexed { index, task ->
            val currentTotalMinutes = currentHour * 60 + currentMinute
            if (currentTotalMinutes >= totalEndMinutes) return@forEachIndexed

            val startT = String.format("%02d:%02d", currentHour, currentMinute)

            // 작업 시간 할당
            val taskDuration = when {
                isEveningTime -> 60 // 저녁: 1시간씩
                else -> 90 // 기본: 1.5시간씩
            }

            currentMinute += taskDuration
            currentHour += currentMinute / 60
            currentMinute %= 60

            val newTotalMinutes = currentHour * 60 + currentMinute
            if (newTotalMinutes > totalEndMinutes) {
                currentHour = endHour
                currentMinute = endMinute
            }

            val endT = String.format("%02d:%02d", currentHour, currentMinute)

            scheduleList.add(
                Task(
                    id = "${date}_reorder_${index}",
                    title = task,
                    description = "순서 변경됨",
                    startTime = startT,
                    endTime = endT,
                    date = date
                )
            )

            // 휴식시간 추가
            if (index < tasks.size - 1) {
                val restDuration = if (isEveningTime) 15 else 30
                val restStartTime = String.format("%02d:%02d", currentHour, currentMinute)

                currentMinute += restDuration
                currentHour += currentMinute / 60
                currentMinute %= 60

                val restEndTime = String.format("%02d:%02d", currentHour, currentMinute)

                scheduleList.add(
                    Task(
                        id = "${date}_rest_reorder_${index}",
                        title = if (isEveningTime) "간단한 휴식" else "커피 타임",
                        description = "재충전 시간",
                        startTime = restStartTime,
                        endTime = restEndTime,
                        date = date
                    )
                )
            }
        }

        return scheduleList.sortedBy { it.startTime }
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
                    endTime = _uiState.value.endTime,
                    includeBreaks = _uiState.value.includeBreaks, // 휴식시간 옵션 전달
                    breakDuration = _uiState.value.breakDuration // 휴식시간 길이 전달
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
            isEditMode = false,
            errorMessage = null
        )
    }

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}