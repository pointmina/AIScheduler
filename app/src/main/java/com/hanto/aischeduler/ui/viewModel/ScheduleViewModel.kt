package com.hanto.aischeduler.ui.viewModel

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

    // 개별 작업 시간 수정 (충돌 해결 포함)
    fun updateTaskTime(taskId: String, newStartTime: String, newEndTime: String) {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()
        val taskIndex = currentTasks.indexOfFirst { it.id == taskId }

        if (taskIndex == -1) return

        // 시간 변경
        currentTasks[taskIndex] = currentTasks[taskIndex].copy(
            startTime = newStartTime,
            endTime = newEndTime
        )

        // 충돌 해결 및 재배치
        val redistributedTasks = redistributeAfterTimeChange(currentTasks, taskIndex)

        _uiState.update {
            it.copy(generatedSchedule = redistributedTasks)
        }
    }

    // 시간 변경 후 자동 재배치 (O(N) 알고리즘)
    private fun redistributeAfterTimeChange(
        tasks: MutableList<Task>,
        changedTaskIndex: Int
    ): List<Task> {
        // 시간순으로 정렬
        val sortedTasks = tasks.sortedBy { it.startTime }.toMutableList()
        val changedTaskNewIndex = sortedTasks.indexOfFirst { it.id == tasks[changedTaskIndex].id }

        // 변경된 Task 이후부터 순차적으로 검사 및 조정
        for (i in changedTaskNewIndex + 1 until sortedTasks.size) {
            val prevTask = sortedTasks[i - 1]
            val currentTask = sortedTasks[i]

            // 고정 Task(점심, 휴식) 보호
            if (isFixedTask(currentTask)) {
                continue
            }

            // 이전 Task와 겹치는지 확인
            if (timeToMinutes(prevTask.endTime) > timeToMinutes(currentTask.startTime)) {
                // 겹치면 현재 Task를 뒤로 밀기
                val taskDuration = timeToMinutes(currentTask.endTime) - timeToMinutes(currentTask.startTime)
                val newStartTime = prevTask.endTime
                val newEndTime = minutesToTime(timeToMinutes(newStartTime) + taskDuration)

                sortedTasks[i] = currentTask.copy(
                    startTime = newStartTime,
                    endTime = newEndTime,
                    description = "${currentTask.description} (자동 조정됨)"
                )

                // 전체 종료시간 초과 검사
                val endTimeLimit = timeToMinutes(_uiState.value.endTime)
                if (timeToMinutes(newEndTime) > endTimeLimit) {
                    // 종료시간 초과 시 사용자에게 알림
                    _uiState.update {
                        it.copy(errorMessage = "⚠️ 일부 작업이 설정된 종료시간을 초과합니다. 시간을 조정하거나 작업을 줄여주세요.")
                    }
                    break
                }
            }
        }

        return sortedTasks
    }

    // 고정 Task 여부 확인 (점심시간, 휴식시간 등)
    private fun isFixedTask(task: Task): Boolean {
        return task.title.contains("점심") ||
                task.title.contains("휴식") ||
                task.title.contains("커피") ||
                task.title.contains("저녁식사")
    }

    // 시간을 분으로 변환
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

    // 분을 시간으로 변환
    private fun minutesToTime(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format("%02d:%02d", hour, minute)
    }

    // 스케줄 압축 (각 작업 시간을 비례적으로 축소)
    fun compressSchedule() {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()
        val nonFixedTasks = currentTasks.filter { !isFixedTask(it) }

        if (nonFixedTasks.isEmpty()) return

        val startTimeMinutes = timeToMinutes(_uiState.value.startTime)
        val endTimeMinutes = timeToMinutes(_uiState.value.endTime)
        val availableMinutes = endTimeMinutes - startTimeMinutes

        // 고정 Task들의 시간 계산
        val fixedTasks = currentTasks.filter { isFixedTask(it) }
        val fixedTasksTime = fixedTasks.sumOf {
            timeToMinutes(it.endTime) - timeToMinutes(it.startTime)
        }

        // 일반 작업들이 사용할 수 있는 시간
        val availableForTasks = availableMinutes - fixedTasksTime

        // 각 작업의 새로운 시간 계산
        val taskDuration = availableForTasks / nonFixedTasks.size
        var currentTime = startTimeMinutes

        val compressedTasks = mutableListOf<Task>()

        currentTasks.sortedBy { it.startTime }.forEach { task ->
            if (isFixedTask(task)) {
                // 고정 Task는 그대로 유지
                compressedTasks.add(task)
                currentTime = timeToMinutes(task.endTime)
            } else {
                // 일반 Task는 압축된 시간으로 조정
                val startTime = minutesToTime(currentTime)
                val endTime = minutesToTime(currentTime + taskDuration)

                compressedTasks.add(
                    task.copy(
                        startTime = startTime,
                        endTime = endTime,
                        description = "${task.description} (압축됨)"
                    )
                )
                currentTime += taskDuration
            }
        }

        _uiState.update {
            it.copy(
                generatedSchedule = compressedTasks.sortedBy { task -> task.startTime },
                errorMessage = null
            )
        }
    }

    // 종료시간 자동 연장
    fun extendEndTime() {
        val currentTasks = _uiState.value.generatedSchedule
        if (currentTasks.isEmpty()) return

        // 마지막 Task의 종료시간 찾기
        val lastTask = currentTasks.maxByOrNull { timeToMinutes(it.endTime) }
        lastTask?.let { task ->
            val newEndTime = minutesToTime(timeToMinutes(task.endTime) + 30) // 30분 여유

            _uiState.update {
                it.copy(
                    endTime = newEndTime,
                    errorMessage = null
                )
            }
        }
    }

    // 충돌 상황 감지 및 해결 옵션 제공
    fun checkAndResolveConflicts() {
        val currentTasks = _uiState.value.generatedSchedule
        val endTimeLimit = timeToMinutes(_uiState.value.endTime)

        val overflowTask = currentTasks.find {
            timeToMinutes(it.endTime) > endTimeLimit
        }

        if (overflowTask != null) {
            val overflowMinutes = timeToMinutes(overflowTask.endTime) - endTimeLimit
            _uiState.update {
                it.copy(
                    errorMessage = "⚠️ '${overflowTask.title}' 작업이 ${overflowMinutes}분 초과합니다. 해결 방법을 선택해주세요."
                )
            }
        }
    }

    // 스마트 휴식시간 조정 (충돌 해결용)
    fun adjustBreakTimes() {
        val currentTasks = _uiState.value.generatedSchedule.toMutableList()
        val breakTasks = currentTasks.filter {
            it.title.contains("휴식") || it.title.contains("커피")
        }

        // 휴식시간을 절반으로 줄이기
        val adjustedTasks = currentTasks.map { task ->
            if (breakTasks.contains(task)) {
                val currentDuration = timeToMinutes(task.endTime) - timeToMinutes(task.startTime)
                val newDuration = maxOf(5, currentDuration / 2) // 최소 5분

                task.copy(
                    endTime = minutesToTime(timeToMinutes(task.startTime) + newDuration),
                    description = "${task.description} (단축됨)"
                )
            } else {
                task
            }
        }

        _uiState.update {
            it.copy(generatedSchedule = adjustedTasks)
        }
    }

    // 에러 메시지 지우기
    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    // 시간 재계산 함수
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