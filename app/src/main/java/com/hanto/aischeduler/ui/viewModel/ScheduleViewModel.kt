package com.hanto.aischeduler.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.data.model.onError
import com.hanto.aischeduler.data.model.onSuccess
import com.hanto.aischeduler.domain.entity.ScheduleRequest
import com.hanto.aischeduler.domain.entity.Task
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
    private val validateTimeRangeUseCase: ValidateTimeRangeUseCase
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
     *작업 목록 미리 검증
     */
    fun validateTasks(tasks: List<String>): String? {
        return try {
            validateTasksUseCase(tasks)

            // 복잡도 분석 결과도 제공
            val analysis = validateTasksUseCase.analyzeTaskComplexity(tasks)
            if (analysis.recommendations.isNotEmpty()) {
                "💡 ${analysis.recommendations.first()}"
            } else null

        } catch (e: AppException) {
            e.getUserMessage()
        } catch (e: Exception) {
            "작업 검증 중 오류가 발생했습니다"
        }
    }

    /**
     *시간 범위 미리 검증
     */
    fun validateTimeRange(startTime: String, endTime: String): String? {
        return try {
            val timeRange = TimeRange(startTime, endTime)
            validateTimeRangeUseCase(timeRange)

            // 품질 분석 결과도 제공
            val analysis = validateTimeRangeUseCase.analyzeTimeRangeQuality(timeRange)
            when (analysis.quality) {
                com.hanto.aischeduler.domain.usecase.TimeQuality.POOR ->
                    "시간 설정을 개선하면 더 좋은 스케줄을 만들 수 있습니다"

                com.hanto.aischeduler.domain.usecase.TimeQuality.ACCEPTABLE ->
                    "✅ 적절한 시간 설정입니다"

                com.hanto.aischeduler.domain.usecase.TimeQuality.GOOD ->
                    "👍 좋은 시간 설정입니다"

                com.hanto.aischeduler.domain.usecase.TimeQuality.EXCELLENT ->
                    "🌟 최적의 시간 설정입니다!"
            }

        } catch (e: AppException) {
            e.getUserMessage()
        } catch (e: Exception) {
            "시간 검증 중 오류가 발생했습니다"
        }
    }

    /**
     * Domain Task를 Data Task로 변환 (UI 호환성)
     */
    private fun convertToDataTasks(domainTasks: List<Task>): List<com.hanto.aischeduler.data.model.Task> {
        return domainTasks.map { domainTask ->
            com.hanto.aischeduler.data.model.Task(
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

    fun updateTaskTime(taskId: String, newStartTime: String, newEndTime: String) {
        Log.d(TAG, "updateTaskTime 기능은 향후 구현 예정")
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
}