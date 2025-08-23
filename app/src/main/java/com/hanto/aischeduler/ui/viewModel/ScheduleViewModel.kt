package com.hanto.aischeduler.ui.viewModel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanto.aischeduler.data.database.SavedScheduleDao
import com.hanto.aischeduler.data.database.SavedScheduleEntity
import com.hanto.aischeduler.data.database.SavedTaskEntity
import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.data.model.onError
import com.hanto.aischeduler.data.model.onSuccess
import com.hanto.aischeduler.domain.entity.ScheduleRequest
import com.hanto.aischeduler.domain.entity.TimeRange
import com.hanto.aischeduler.domain.usecase.GenerateScheduleUseCase
import com.hanto.aischeduler.domain.usecase.ValidateTasksUseCase
import com.hanto.aischeduler.domain.usecase.ValidateTimeRangeUseCase
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
    private val generateScheduleUseCase: GenerateScheduleUseCase,
    private val validateTasksUseCase: ValidateTasksUseCase,
    private val validateTimeRangeUseCase: ValidateTimeRangeUseCase,
    private val savedScheduleDao: SavedScheduleDao
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

    /**
     * 현재 스케줄을 데이터베이스에 저장
     */
    fun saveCurrentSchedule(title: String = "오늘의 계획") {
        val currentState = _uiState.value

        if (currentState.generatedSchedule.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "저장할 스케줄이 없습니다")
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "스케줄 저장 시작")

                // 1. 스케줄 엔티티 생성
                val scheduleId = "schedule_${System.currentTimeMillis()}"
                val scheduleEntity = SavedScheduleEntity(
                    id = scheduleId,
                    title = title,
                    date = getTodayDateString(),
                    startTime = currentState.startTime,
                    endTime = currentState.endTime,
                    totalTasks = currentState.generatedSchedule.size,
                    completedTasks = 0
                )

                // 2. 작업 엔티티들 생성
                val taskEntities = currentState.generatedSchedule.mapIndexed { index, task ->
                    SavedTaskEntity(
                        id = "task_${scheduleId}_$index",
                        scheduleId = scheduleId,
                        title = task.title,
                        description = task.description,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        isCompleted = task.isCompleted,
                        sortOrder = index
                    )
                }

                // 3. 데이터베이스에 저장
                savedScheduleDao.insertSchedule(scheduleEntity)
                savedScheduleDao.insertTasks(taskEntities)

                Log.d(TAG, "스케줄 저장 성공: $scheduleId")

                _uiState.update {
                    it.copy(errorMessage = "✅ 계획이 저장되었습니다!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "스케줄 저장 실패", e)
                _uiState.update {
                    it.copy(errorMessage = "저장 중 오류가 발생했습니다: ${e.message}")
                }
            }
        }
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
     *Use Case를 사용한 스케줄 생성
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
            Log.d(TAG, "🚀 Use Case 기반 스케줄 생성 시작")

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                //Domain 엔티티 생성
                val timeRange = TimeRange(currentState.startTime, currentState.endTime)
                val request = ScheduleRequest(
                    tasks = currentState.tasks,
                    timeRange = timeRange,
                    date = getTodayDateString()
                )

                Log.d(TAG, "요청 생성: ${request.getSummary()}")

                //Use Case 실행
                val result = generateScheduleUseCase(request)

                result
                    .onSuccess { schedule ->
                        Log.d(TAG, "스케줄 생성 성공: ${schedule.getSummary()}")

                        // Domain Task를 UI용 Task로 변환
                        val uiTasks = convertToDataTasks(schedule.tasks)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generatedSchedule = uiTasks,
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
     * Domain Task를 Data Task로 변환 (UI 호환성)
     */
    private fun convertToDataTasks(domainTasks: List<Task>): List<Task> {
        return domainTasks.map { domainTask ->
            Task(
                id = domainTask.id,
                title = domainTask.title,
                description = domainTask.description,
                startTime = domainTask.startTime,
                endTime = domainTask.endTime,
                date = domainTask.date,
                isCompleted = domainTask.isCompleted
            )
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

    // 기존 메서드들 유지 (간소화)
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        Log.d(TAG, "reorderTasks 기능은 향후 구현 예정")
    }

    fun splitSchedule() {
        Log.d(TAG, "splitSchedule 기능은 향후 구현 예정")
    }

    fun extendEndTime() {
        Log.d(TAG, "extendEndTime 기능은 향후 구현 예정")
    }

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    /**
     * 작업의 시간을 업데이트 (편집 모드) - 충돌 감지 포함
     */
    fun updateTaskTime(taskId: String, newStartTime: String, newEndTime: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "작업 시간 업데이트 시작: $taskId ($newStartTime-$newEndTime)")

                // 1. 시간 유효성 검증
                if (!isValidTimeFormat(newStartTime) || !isValidTimeFormat(newEndTime)) {
                    _uiState.update {
                        it.copy(errorMessage = "올바르지 않은 시간 형식입니다")
                    }
                    return@launch
                }

                val startMinutes = timeToMinutes(newStartTime)
                val endMinutes = timeToMinutes(newEndTime)

                if (endMinutes <= startMinutes) {
                    _uiState.update {
                        it.copy(errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다")
                    }
                    return@launch
                }

                // 2. 다른 작업들과의 충돌 체크
                val currentTasks = _uiState.value.generatedSchedule
                val conflictingTasks = findConflictingTasks(
                    currentTasks = currentTasks,
                    editingTaskId = taskId,
                    newStartTime = newStartTime,
                    newEndTime = newEndTime
                )

                if (conflictingTasks.isNotEmpty()) {
                    // 충돌 발견 - 자동 조정 시도
                    val adjustedTasks = autoAdjustConflictingTasks(
                        currentTasks = currentTasks,
                        editingTaskId = taskId,
                        newStartTime = newStartTime,
                        newEndTime = newEndTime,
                        conflictingTasks = conflictingTasks
                    )

                    if (adjustedTasks != null) {
                        // 자동 조정 성공
                        _uiState.update {
                            it.copy(
                                generatedSchedule = adjustedTasks,
                                errorMessage = "⚡ 다른 작업들의 시간을 자동으로 조정했습니다"
                            )
                        }

                        // 모든 변경된 작업들을 데이터베이스에 업데이트
                        updateMultipleTasksInDatabase(adjustedTasks.filter {
                            it.id == taskId || conflictingTasks.any { conflict -> conflict.id == it.id }
                        })

                    } else {
                        // 자동 조정 실패 - 사용자에게 알림
                        val conflictNames = conflictingTasks.joinToString(", ") { it.title }
                        _uiState.update {
                            it.copy(errorMessage = "⚠️ 다음 작업과 시간이 겹칩니다: $conflictNames")
                        }
                    }

                } else {
                    // 충돌 없음 - 정상 업데이트
                    val updatedTasks = currentTasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(
                                startTime = newStartTime,
                                endTime = newEndTime
                            )
                        } else {
                            task
                        }
                    }

                    _uiState.update {
                        it.copy(
                            generatedSchedule = updatedTasks,
                            errorMessage = "⏰ 시간이 수정되었습니다"
                        )
                    }

                    // 데이터베이스 업데이트
                    if (taskId.startsWith("task_schedule_")) {
                        savedScheduleDao.updateTaskTime(taskId, newStartTime, newEndTime)

                        val scheduleId = extractScheduleIdFromTaskId(taskId)
                        if (scheduleId.isNotEmpty()) {
                            savedScheduleDao.updateScheduleLastModified(
                                scheduleId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "작업 시간 업데이트 실패", e)
                _uiState.update {
                    it.copy(errorMessage = "시간 수정 중 오류가 발생했습니다")
                }
            }
        }
    }

    // 충돌하는 작업들 찾기
    private fun findConflictingTasks(
        currentTasks: List<Task>,
        editingTaskId: String,
        newStartTime: String,
        newEndTime: String
    ): List<Task> {
        val newStartMinutes = timeToMinutes(newStartTime)
        val newEndMinutes = timeToMinutes(newEndTime)

        return currentTasks.filter { task ->
            if (task.id == editingTaskId) return@filter false

            val taskStartMinutes = timeToMinutes(task.startTime)
            val taskEndMinutes = timeToMinutes(task.endTime)

            // 시간 겹침 체크
            newStartMinutes < taskEndMinutes && newEndMinutes > taskStartMinutes
        }
    }

    // 충돌하는 작업들을 자동으로 조정
    private fun autoAdjustConflictingTasks(
        currentTasks: List<Task>,
        editingTaskId: String,
        newStartTime: String,
        newEndTime: String,
        conflictingTasks: List<Task>
    ): List<Task>? {
        return try {
            val adjustedTasks = currentTasks.toMutableList()
            val newEndMinutes = timeToMinutes(newEndTime)

            // 편집 중인 작업 업데이트
            val editingTaskIndex = adjustedTasks.indexOfFirst { it.id == editingTaskId }
            if (editingTaskIndex >= 0) {
                adjustedTasks[editingTaskIndex] = adjustedTasks[editingTaskIndex].copy(
                    startTime = newStartTime,
                    endTime = newEndTime
                )
            }

            // 충돌하는 작업들을 편집된 작업 이후 시간으로 밀기
            conflictingTasks.sortedBy { timeToMinutes(it.startTime) }.forEach { conflictTask ->
                val taskIndex = adjustedTasks.indexOfFirst { it.id == conflictTask.id }
                if (taskIndex >= 0) {
                    val originalDuration =
                        timeToMinutes(conflictTask.endTime) - timeToMinutes(conflictTask.startTime)
                    val newStartTime = minutesToTime(newEndMinutes)
                    val newEndTime = minutesToTime(newEndMinutes + originalDuration)

                    adjustedTasks[taskIndex] = adjustedTasks[taskIndex].copy(
                        startTime = newStartTime,
                        endTime = newEndTime
                    )
                }
            }

            adjustedTasks.toList()

        } catch (e: Exception) {
            Log.e(TAG, "자동 조정 실패", e)
            null
        }
    }

    // 분을 시간 문자열로 변환
    @SuppressLint("DefaultLocale")
    private fun minutesToTime(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format("%02d:%02d", hour, minute)
    }

    // 여러 작업을 데이터베이스에 업데이트
    private suspend fun updateMultipleTasksInDatabase(tasks: List<Task>) {
        try {
            tasks.forEach { task ->
                if (task.id.startsWith("task_schedule_")) {
                    savedScheduleDao.updateTaskTime(task.id, task.startTime, task.endTime)
                }
            }

            // 스케줄 수정 시간 업데이트
            tasks.firstOrNull()?.let { firstTask ->
                val scheduleId = extractScheduleIdFromTaskId(firstTask.id)
                if (scheduleId.isNotEmpty()) {
                    savedScheduleDao.updateScheduleLastModified(
                        scheduleId,
                        System.currentTimeMillis()
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "데이터베이스 일괄 업데이트 실패", e)
        }
    }


    /**
     * 저장된 오늘 스케줄 불러오기
     */
    fun loadTodaySchedule() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "오늘 스케줄 불러오기 시작")

                val today = getTodayDateString()
                val savedSchedule = savedScheduleDao.getScheduleByDate(today)

                if (savedSchedule != null) {
                    Log.d(TAG, "저장된 스케줄 발견: ${savedSchedule.schedule.title}")

                    // 저장된 데이터를 UI 모델로 변환
                    val uiTasks = savedSchedule.tasks.map { taskEntity ->
                        Task(
                            id = taskEntity.id,
                            title = taskEntity.title,
                            description = taskEntity.description,
                            startTime = taskEntity.startTime,
                            endTime = taskEntity.endTime,
                            date = savedSchedule.schedule.date,
                            isCompleted = taskEntity.isCompleted
                        )
                    }.sortedBy { it.startTime }

                    _uiState.update {
                        it.copy(
                            generatedSchedule = uiTasks,
                            isScheduleGenerated = true,
                            startTime = savedSchedule.schedule.startTime,
                            endTime = savedSchedule.schedule.endTime,
                            errorMessage = "저장된 계획을 불러왔습니다"
                        )
                    }
                } else {
                    Log.d(TAG, "오늘 저장된 스케줄 없음")
                }

            } catch (e: Exception) {
                Log.e(TAG, "스케줄 불러오기 실패", e)
                _uiState.update {
                    it.copy(errorMessage = "저장된 계획을 불러올 수 없습니다")
                }
            }
        }
    }

    /**
     * 작업 완료 상태 업데이트
     */
    fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                // 1. 데이터베이스 업데이트
                savedScheduleDao.updateTaskCompletion(taskId, isCompleted)

                // 2. UI 상태 업데이트
                val updatedTasks = _uiState.value.generatedSchedule.map { task ->
                    if (task.id == taskId) {
                        task.copy(isCompleted = isCompleted)
                    } else {
                        task
                    }
                }

                _uiState.update {
                    it.copy(generatedSchedule = updatedTasks)
                }

                // 3. 스케줄의 완료 카운트 업데이트
                if (taskId.contains("task_schedule_")) {
                    val scheduleId = taskId.substringAfter("task_").substringBefore("_")
                    if (scheduleId.isNotEmpty()) {
                        savedScheduleDao.updateScheduleCompletionCount("schedule_$scheduleId")
                    }
                }

                Log.d(TAG, "작업 완료 상태 업데이트: $taskId -> $isCompleted")

            } catch (e: Exception) {
                Log.e(TAG, "작업 상태 업데이트 실패", e)
            }
        }
    }

    private fun isValidTimeFormat(time: String): Boolean {
        return time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
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

    private fun extractScheduleIdFromTaskId(taskId: String): String {
        return try {
            if (taskId.startsWith("task_schedule_")) {
                val parts = taskId.split("_")
                if (parts.size >= 3) {
                    "schedule_${parts[2]}"
                } else ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

}