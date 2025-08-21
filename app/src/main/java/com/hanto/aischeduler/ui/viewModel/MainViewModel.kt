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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    val tasks: List<String> = emptyList(),
    val generatedSchedule: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MainViewModel"
    }

    fun addTask(task: String) {
        if (task.isBlank()) {
            Log.w(TAG, "빈 작업 추가 시도")
            return
        }

        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.add(task.trim())

        _uiState.value = _uiState.value.copy(
            tasks = currentTasks,
            errorMessage = null
        )

        Log.d(TAG, "작업 추가됨: $task (총 ${currentTasks.size}개)")
    }

    fun removeTask(task: String) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val removed = currentTasks.remove(task)

        if (removed) {
            _uiState.value = _uiState.value.copy(tasks = currentTasks)
            Log.d(TAG, "작업 제거됨: $task (남은 ${currentTasks.size}개)")
        }
    }

    /**
     * 스케줄 생성 (NetworkResult 처리)
     */
    fun generateSchedule() {
        val currentState = _uiState.value

        // 기본 검증
        if (currentState.tasks.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "할 일을 하나 이상 추가해주세요"
            )
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "스케줄 생성 시작 - ${currentState.tasks.size}개 작업")

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val result = scheduleRepository.generateSchedule(
                    tasks = currentState.tasks,
                    date = getTodayDateString()
                )

                // NetworkResult 처리
                result
                    .onSuccess { schedule ->
                        Log.d(TAG, "스케줄 생성 성공: ${schedule.size}개 항목")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            generatedSchedule = schedule,
                            errorMessage = null
                        )
                    }
                    .onError { exception ->
                        Log.e(TAG, "스케줄 생성 실패", exception)
                        val userMessage = if (exception is AppException) {
                            exception.getUserMessage()
                        } else {
                            "일정 생성 중 오류가 발생했습니다"
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = userMessage
                        )
                    }

            } catch (e: Exception) {
                Log.e(TAG, "예상치 못한 오류", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "예상치 못한 오류가 발생했습니다"
                )
            }
        }
    }

    /**
     * 에러 메시지 제거
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        Log.d(TAG, "에러 메시지 제거됨")
    }

    /**
     * 스케줄 초기화
     */
    fun clearSchedule() {
        _uiState.value = _uiState.value.copy(
            generatedSchedule = emptyList(),
            tasks = emptyList(),
            errorMessage = null
        )
        Log.d(TAG, "스케줄 초기화됨")
    }

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}