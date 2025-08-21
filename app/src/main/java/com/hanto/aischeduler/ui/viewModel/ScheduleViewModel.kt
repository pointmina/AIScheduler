// 업데이트된 ScheduleViewModel.kt
package com.hanto.aischeduler.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.data.model.onError
import com.hanto.aischeduler.data.model.onSuccess
import com.hanto.aischeduler.data.repository.ScheduleRepository
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

    companion object {
        private const val TAG = "ScheduleViewModel"
    }

    fun addTask(task: String) {
        if (task.isBlank()) {
            Log.w(TAG, "빈 작업 추가 시도")
            return
        }

        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.add(task.trim())

        _uiState.update {
            it.copy(
                tasks = currentTasks,
                errorMessage = null
            )
        }

        Log.d(TAG, "작업 추가됨: $task (총 ${currentTasks.size}개)")
    }

    fun removeTask(task: String) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val removed = currentTasks.remove(task)

        if (removed) {
            _uiState.update { it.copy(tasks = currentTasks) }
            Log.d(TAG, "작업 제거됨: $task (남은 ${currentTasks.size}개)")
        }
    }

    fun updateStartTime(time: String) {
        _uiState.update { it.copy(startTime = time, errorMessage = null) }
        Log.d(TAG, "시작 시간 변경: $time")
    }

    fun updateEndTime(time: String) {
        _uiState.update { it.copy(endTime = time, errorMessage = null) }
        Log.d(TAG, "종료 시간 변경: $time")
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
        Log.d(TAG, "편집 모드 토글: ${_uiState.value.isEditMode}")
    }

    /**
     * 스케줄 생성 (개선된 에러 핸들링)
     */
    fun generateSchedule() {
        val currentState = _uiState.value

        // 기본 검증
        if (currentState.tasks.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "할 일을 하나 이상 추가해주세요")
            }
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "스케줄 생성 시작 - ${currentState.tasks.size}개 작업")

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val result = scheduleRepository.generateSchedule(
                    tasks = currentState.tasks,
                    date = getTodayDateString(),
                    startTime = currentState.startTime,
                    endTime = currentState.endTime
                )

                result
                    .onSuccess { schedule ->
                        Log.d(TAG, "스케줄 생성 성공: ${schedule.size}개 항목")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generatedSchedule = schedule,
                                isScheduleGenerated = true,
                                errorMessage = null
                            )
                        }
                    }
                    .onError { exception ->
                        Log.e(TAG, "스케줄 생성 실패", exception)
                        val userMessage = if (exception is AppException) {
                            exception.getUserMessage()
                        } else {
                            "일정 생성 중 오류가 발생했습니다"
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = userMessage
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "예상치 못한 오류", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "예상치 못한 오류가 발생했습니다"
                    )
                }
            }
        }
    }

    /**
     * 에러 메시지 제거
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
        Log.d(TAG, "에러 메시지 제거됨")
    }

    /**
     * 입력 화면으로 돌아가기
     */
    fun backToInput() {
        _uiState.update {
            it.copy(
                isScheduleGenerated = false,
                isEditMode = false,
                errorMessage = null
            )
        }
        Log.d(TAG, "입력 화면으로 복귀")
    }

    /**
     * 스케줄 초기화
     */
    fun clearSchedule() {
        _uiState.update {
            it.copy(
                generatedSchedule = emptyList(),
                tasks = emptyList(),
                isScheduleGenerated = false,
                errorMessage = null,
                isEditMode = false
            )
        }
        Log.d(TAG, "스케줄 초기화됨")
    }

    // 기존 메서드들 유지 (reorderTasks, updateTaskTime 등)
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()

        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= currentTasks.size || toIndex >= currentTasks.size
        ) {
            Log.w(TAG, "잘못된 인덱스로 재정렬 시도: $fromIndex -> $toIndex")
            return
        }

        val actualTasks = currentTasks.filter {
            !it.title.contains("휴식") &&
                    !it.title.contains("점심") &&
                    !it.title.contains("저녁식사") &&
                    !it.title.contains("커피")
        }.toMutableList()

        if (fromIndex >= actualTasks.size || toIndex >= actualTasks.size) {
            Log.w(TAG, "실제 작업 범위를 벗어난 재정렬 시도")
            return
        }

        val movedTask = actualTasks.removeAt(fromIndex)
        actualTasks.add(toIndex, movedTask)

        val reorderedSchedule = recalculateScheduleTimes(
            tasks = actualTasks.map { it.title },
            date = _uiState.value.generatedSchedule.firstOrNull()?.date ?: "",
            startTime = _uiState.value.startTime,
            endTime = _uiState.value.endTime
        )

        _uiState.update { it.copy(generatedSchedule = reorderedSchedule) }
        Log.d(TAG, "작업 순서 변경: $fromIndex -> $toIndex")
    }

    fun updateTaskTime(taskId: String, newStartTime: String, newEndTime: String) {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()
        val taskIndex = currentTasks.indexOfFirst { it.id == taskId }

        if (taskIndex == -1) {
            Log.w(TAG, "존재하지 않는 작업 ID: $taskId")
            return
        }

        currentTasks[taskIndex] = currentTasks[taskIndex].copy(
            startTime = newStartTime,
            endTime = newEndTime
        )

        val redistributedTasks = redistributeAfterTimeChange(currentTasks, taskIndex)
        _uiState.update { it.copy(generatedSchedule = redistributedTasks) }

        Log.d(TAG, "작업 시간 변경: $taskId ($newStartTime-$newEndTime)")
    }

    fun splitSchedule() {
        Log.d(TAG, "스케줄 분할 시작")
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()
        val fixedTasks = getFixedTasks(currentTasks)

        val splitTasks = mutableListOf<Task>()

        currentTasks.forEach { task ->
            if (isFixedTask(task)) {
                splitTasks.add(task)
            } else {
                val conflictingFixed = findConflictingFixedTasks(task, fixedTasks)
                if (conflictingFixed.isEmpty()) {
                    splitTasks.add(task)
                } else {
                    val splitResult = splitTaskAroundFixed(task, conflictingFixed)
                    splitTasks.addAll(splitResult)
                }
            }
        }

        _uiState.update {
            it.copy(
                generatedSchedule = splitTasks.sortedBy { task -> task.startTime },
                errorMessage = null
            )
        }
        Log.d(TAG, "스케줄 분할 완료: ${splitTasks.size}개 항목")
    }

    fun extendEndTime() {
        val currentTasks = _uiState.value.generatedSchedule
        if (currentTasks.isEmpty()) return

        val lastTask = currentTasks.maxByOrNull { timeToMinutes(it.endTime) }
        lastTask?.let { task ->
            val newEndTime = minutesToTime(timeToMinutes(task.endTime) + 30)
            _uiState.update {
                it.copy(
                    endTime = newEndTime,
                    errorMessage = null
                )
            }
            Log.d(TAG, "종료 시간 연장: $newEndTime")
        }
    }

    // 유틸리티 메서드들
    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return try {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            hour * 60 + minute
        } catch (e: Exception) {
            0
        }
    }

    private fun minutesToTime(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format("%02d:%02d", hour, minute)
    }

    private fun isFixedTask(task: Task): Boolean {
        return task.title.contains("점심") || task.title.contains("저녁식사")
    }

    private fun getFixedTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { isFixedTask(it) }
    }

    private fun findConflictingFixedTasks(task: Task, fixedTasks: List<Task>): List<Task> {
        val taskStart = timeToMinutes(task.startTime)
        val taskEnd = timeToMinutes(task.endTime)

        return fixedTasks.filter { fixedTask ->
            val fixedStart = timeToMinutes(fixedTask.startTime)
            val fixedEnd = timeToMinutes(fixedTask.endTime)

            // 시간 겹침 확인
            taskStart < fixedEnd && taskEnd > fixedStart
        }
    }

    private fun splitTaskAroundFixed(task: Task, conflictingFixed: List<Task>): List<Task> {
        val result = mutableListOf<Task>()
        val taskStart = timeToMinutes(task.startTime)
        val taskEnd = timeToMinutes(task.endTime)

        val sortedFixed = conflictingFixed.sortedBy { timeToMinutes(it.startTime) }
        val timeSlots = mutableListOf<Pair<Int, Int>>()
        var currentStart = taskStart

        sortedFixed.forEach { fixedTask ->
            val fixedStart = timeToMinutes(fixedTask.startTime)
            val fixedEnd = timeToMinutes(fixedTask.endTime)

            if (currentStart < fixedStart) {
                val slotEnd = minOf(fixedStart, taskEnd)
                if (slotEnd > currentStart) {
                    timeSlots.add(Pair(currentStart, slotEnd))
                }
            }
            currentStart = maxOf(currentStart, fixedEnd)
        }

        if (currentStart < taskEnd) {
            timeSlots.add(Pair(currentStart, taskEnd))
        }

        timeSlots.forEachIndexed { index, (start, end) ->
            val partTitle = if (timeSlots.size > 1) {
                "${task.title} (${index + 1}부)"
            } else {
                task.title
            }

            result.add(
                task.copy(
                    id = "${task.id}_part${index + 1}",
                    title = partTitle,
                    startTime = minutesToTime(start),
                    endTime = minutesToTime(end),
                    description = if (timeSlots.size > 1) "${task.description} (분할됨)" else task.description
                )
            )
        }

        return result
    }

    private fun redistributeAfterTimeChange(
        tasks: MutableList<Task>,
        changedTaskIndex: Int
    ): List<Task> {
        val sortedTasks = tasks.sortedBy { it.startTime }.toMutableList()
        val changedTaskNewIndex = sortedTasks.indexOfFirst { it.id == tasks[changedTaskIndex].id }

        for (i in changedTaskNewIndex + 1 until sortedTasks.size) {
            val prevTask = sortedTasks[i - 1]
            val currentTask = sortedTasks[i]

            if (isFixedTask(currentTask)) continue

            if (timeToMinutes(prevTask.endTime) > timeToMinutes(currentTask.startTime)) {
                val taskDuration =
                    timeToMinutes(currentTask.endTime) - timeToMinutes(currentTask.startTime)
                val newStartTime = prevTask.endTime
                val newEndTime = minutesToTime(timeToMinutes(newStartTime) + taskDuration)

                sortedTasks[i] = currentTask.copy(
                    startTime = newStartTime,
                    endTime = newEndTime,
                    description = "${currentTask.description} (자동 조정됨)"
                )

                val endTimeLimit = timeToMinutes(_uiState.value.endTime)
                if (timeToMinutes(newEndTime) > endTimeLimit) {
                    _uiState.update {
                        it.copy(errorMessage = "⚠️ 일부 작업이 설정된 종료시간을 초과합니다.")
                    }
                    break
                }
            }
        }

        return sortedTasks
    }

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
        val taskDuration = if (isEveningTime) 60 else 90

        tasks.forEachIndexed { index, task ->
            val currentTotalMinutes = currentHour * 60 + currentMinute
            if (currentTotalMinutes >= totalEndMinutes) return@forEachIndexed

            val startT = String.format("%02d:%02d", currentHour, currentMinute)

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
}