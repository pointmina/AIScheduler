package com.hanto.aischeduler.domain.usecase

import android.util.Log
import com.hanto.aischeduler.data.model.NetworkResult
import com.hanto.aischeduler.domain.entity.Schedule
import com.hanto.aischeduler.domain.entity.ScheduleRequest
import com.hanto.aischeduler.domain.entity.TimeRange
import com.hanto.aischeduler.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * 스케줄 생성의 핵심 Use Case
 *
 * 모든 검증과 최적화를 조합하여 완전한 스케줄을 생성
 * Clean Architecture의 핵심 - 비즈니스 로직의 오케스트레이션
 */
class GenerateScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val validateTasksUseCase: ValidateTasksUseCase,
    private val validateTimeRangeUseCase: ValidateTimeRangeUseCase
) {

    companion object {
        private const val TAG = "GenerateScheduleUseCase"
    }

    /**
     * 스케줄 생성 실행
     * @param request 스케줄 생성 요청
     * @return 생성된 스케줄 또는 에러
     */
    suspend operator fun invoke(request: ScheduleRequest): NetworkResult<Schedule> {
        return try {
            Log.d(TAG, "스케줄 생성 시작: ${request.getSummary()}")

            // 1. 요청 유효성 검증
            validateRequest(request)

            // 2. 작업 목록 검증 및 분석
            val taskAnalysis = analyzeAndValidateTasks(request.tasks)

            // 3. 시간 범위 검증 및 분석
            val timeAnalysis = analyzeAndValidateTimeRange(request.timeRange)

            // 4. 스케줄 생성 가능성 사전 체크
            validateScheduleFeasibility(request, taskAnalysis, timeAnalysis)

            // 5. Repository를 통한 AI 스케줄 생성
            val result = scheduleRepository.generateSchedule(request)

            // 6. 생성 결과 후처리
            processGenerationResult(result, request)

        } catch (e: Exception) {
            Log.e(TAG, "스케줄 생성 실패", e)
            NetworkResult.Error(e)
        }
    }

    /**
     * 요청 기본 유효성 검증
     */
    private fun validateRequest(request: ScheduleRequest) {
        if (!request.isValid()) {
            throw IllegalArgumentException("유효하지 않은 스케줄 요청입니다")
        }

        if (!request.preferences.isValid()) {
            throw IllegalArgumentException("유효하지 않은 스케줄 설정입니다")
        }
    }

    /**
     * 작업 목록 분석 및 검증
     */
    private fun analyzeAndValidateTasks(tasks: List<String>): TaskAnalysisResult {
        // 기본 검증
        validateTasksUseCase(tasks)

        // 상세 분석
        val complexityAnalysis = validateTasksUseCase.analyzeTaskComplexity(tasks)
        val estimatedDuration = validateTasksUseCase.estimateTotalDuration(tasks)

        Log.d(
            TAG,
            "작업 분석 완료: ${complexityAnalysis.getComplexityRatio()}, 예상 소요시간: ${estimatedDuration}분"
        )

        return TaskAnalysisResult(
            complexityAnalysis = complexityAnalysis,
            estimatedDuration = estimatedDuration
        )
    }

    /**
     * 시간 범위 분석 및 검증
     */
    private fun analyzeAndValidateTimeRange(timeRange: TimeRange): TimeAnalysisResult {
        // 기본 검증
        validateTimeRangeUseCase(timeRange)

        // 상세 분석
        val qualityAnalysis = validateTimeRangeUseCase.analyzeTimeRangeQuality(timeRange)

        Log.d(
            TAG,
            "시간 범위 분석 완료: ${qualityAnalysis.quality}, 생산성: ${(qualityAnalysis.productivity * 100).toInt()}%"
        )

        return TimeAnalysisResult(
            qualityAnalysis = qualityAnalysis,
            availableMinutes = timeRange.totalMinutes()
        )
    }

    /**
     * 스케줄 생성 가능성 사전 체크
     */
    private fun validateScheduleFeasibility(
        request: ScheduleRequest,
        taskAnalysis: TaskAnalysisResult,
        timeAnalysis: TimeAnalysisResult
    ) {
        val estimatedDuration = taskAnalysis.estimatedDuration
        val availableMinutes = timeAnalysis.availableMinutes
        val bufferTime = calculateBufferTime(request, timeAnalysis.qualityAnalysis.optimalBreaks)

        val requiredTime = estimatedDuration + bufferTime

        Log.d(TAG, "실행 가능성 체크: 필요시간 ${requiredTime}분, 가용시간 ${availableMinutes}분")

        when {
            requiredTime > availableMinutes * 1.2 -> {
                val excessHours = (requiredTime - availableMinutes) / 60f
                throw IllegalStateException(
                    "시간이 부족합니다. 약 ${String.format("%.1f", excessHours)}시간이 더 필요하거나 " +
                            "작업을 ${taskAnalysis.complexityAnalysis.getTotalTasks() - availableMinutes / 60}개 줄여주세요."
                )
            }

            requiredTime < availableMinutes * 0.3 -> {
                Log.w(TAG, "시간 여유가 너무 많습니다. 추가 작업을 고려해보세요.")
            }
        }

        // 복잡도별 경고
        if (taskAnalysis.complexityAnalysis.complexCount > taskAnalysis.complexityAnalysis.simpleCount + taskAnalysis.complexityAnalysis.mediumCount) {
            Log.w(TAG, "복잡한 작업이 많습니다. 스케줄이 빡빡할 수 있습니다.")
        }
    }

    /**
     * 생성 결과 후처리
     */
    private fun processGenerationResult(
        result: NetworkResult<Schedule>,
        request: ScheduleRequest
    ): NetworkResult<Schedule> {
        return when (result) {
            is NetworkResult.Success -> {
                val schedule = result.data
                Log.d(TAG, "스케줄 생성 성공: ${schedule.getSummary()}")

                // 생성된 스케줄 품질 체크
                val qualityIssues = validateGeneratedScheduleQuality(schedule)

                if (qualityIssues.isNotEmpty()) {
                    Log.w(TAG, "스케줄 품질 이슈: ${qualityIssues.joinToString(", ")}")
                    // 품질 이슈가 있어도 성공으로 반환 (사용자가 판단하도록)
                }

                NetworkResult.Success(schedule)
            }

            is NetworkResult.Error -> {
                Log.e(TAG, "AI 스케줄 생성 실패: ${result.exception.message}")
                result
            }

            is NetworkResult.Loading -> result
        }
    }

    /**
     * 생성된 스케줄 품질 검증
     */
    private fun validateGeneratedScheduleQuality(schedule: Schedule): List<String> {
        val issues = mutableListOf<String>()

        // 충돌 체크
        val conflicts = schedule.getConflictingTasks()
        if (conflicts.isNotEmpty()) {
            issues.add("${conflicts.size}개의 시간 충돌")
        }

        // 완료율 체크 (새 스케줄이므로 0%가 정상)
        // 시간 활용률 체크
        val utilizationRate =
            schedule.getTotalDuration().toFloat() / schedule.timeRange.totalMinutes()
        when {
            utilizationRate < 0.3f -> issues.add("시간 활용률이 낮음 (${(utilizationRate * 100).toInt()}%)")
            utilizationRate > 0.9f -> issues.add("시간 활용률이 너무 높음 (${(utilizationRate * 100).toInt()}%)")
        }

        // 작업 분포 체크
        val durations = schedule.tasks.map { it.getDurationMinutes() }
        if (durations.isNotEmpty()) {
            val maxDuration = durations.maxOrNull() ?: 0
            val minDuration = durations.minOrNull() ?: 0
            if (maxDuration > minDuration * 3) {
                issues.add("작업 시간 편차가 큼")
            }
        }

        return issues
    }

    /**
     * 버퍼 시간 계산 (휴식, 이동시간 등)
     */
    private fun calculateBufferTime(
        request: ScheduleRequest,
        optimalBreaks: Int
    ): Int {
        val breakTime = if (request.preferences.includeBreaks) {
            optimalBreaks * request.preferences.breakDuration
        } else 0

        val transitionTime = (request.tasks.size - 1) * 5 // 작업 간 전환 시간 5분

        return breakTime + transitionTime
    }
}

/**
 * 작업 분석 결과
 */
data class TaskAnalysisResult(
    val complexityAnalysis: TaskComplexityAnalysis,
    val estimatedDuration: Int
)

/**
 * 시간 분석 결과
 */
data class TimeAnalysisResult(
    val qualityAnalysis: TimeRangeAnalysis,
    val availableMinutes: Int
)