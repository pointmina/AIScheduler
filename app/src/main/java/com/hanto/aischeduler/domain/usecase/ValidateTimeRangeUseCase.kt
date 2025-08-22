package com.hanto.aischeduler.domain.usecase

import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.domain.entity.TimeRange
import javax.inject.Inject

/**
 * 시간 범위 검증을 담당하는 Use Case
 *
 * 시간 관련 비즈니스 규칙을 캡슐화
 */
class ValidateTimeRangeUseCase @Inject constructor() {

    companion object {
        private const val MIN_DURATION_HOURS = 1
        private const val MAX_DURATION_HOURS = 16
        private const val RECOMMENDED_MIN_HOURS = 2
        private const val RECOMMENDED_MAX_HOURS = 12
    }

    /**
     * 시간 범위 검증 실행
     * @param timeRange 검증할 시간 범위
     * @throws AppException 검증 실패 시
     */
    operator fun invoke(timeRange: TimeRange) {
        validateTimeFormat(timeRange)
        validateTimeLogic(timeRange)
        validateDuration(timeRange)
        validateBusinessRules(timeRange)
    }

    /**
     * 시간 형식 검증
     */
    private fun validateTimeFormat(timeRange: TimeRange) {
        val startTime = timeRange.startTime
        val endTime = timeRange.endTime

        if (!isValidTimeFormat(startTime)) {
            throw AppException.ValidationException(
                "시작 시간 형식이 올바르지 않습니다: $startTime (HH:MM 형식이어야 합니다)"
            )
        }

        if (!isValidTimeFormat(endTime)) {
            throw AppException.ValidationException(
                "종료 시간 형식이 올바르지 않습니다: $endTime (HH:MM 형식이어야 합니다)"
            )
        }
    }

    /**
     * 시간 논리 검증
     */
    private fun validateTimeLogic(timeRange: TimeRange) {
        val startMinutes = timeToMinutes(timeRange.startTime)
        val endMinutes = timeToMinutes(timeRange.endTime)

        when {
            endMinutes <= startMinutes ->
                throw AppException.TimeRangeException(
                    "종료 시간(${timeRange.endTime})이 시작 시간(${timeRange.startTime})보다 늦어야 합니다"
                )

            startMinutes < 0 || endMinutes > 24 * 60 ->
                throw AppException.TimeRangeException(
                    "시간은 00:00 ~ 23:59 범위 내에 있어야 합니다"
                )
        }
    }

    /**
     * 시간 지속 시간 검증
     */
    private fun validateDuration(timeRange: TimeRange) {
        val totalMinutes = timeRange.totalMinutes()
        val totalHours = totalMinutes / 60f

        when {
            totalMinutes < MIN_DURATION_HOURS * 60 ->
                throw AppException.TimeRangeException(
                    "최소 ${MIN_DURATION_HOURS}시간 이상의 시간 범위가 필요합니다 (현재: ${
                        String.format(
                            "%.1f",
                            totalHours
                        )
                    }시간)"
                )

            totalMinutes > MAX_DURATION_HOURS * 60 ->
                throw AppException.TimeRangeException(
                    "${MAX_DURATION_HOURS}시간을 초과하는 스케줄은 생성할 수 없습니다 (현재: ${
                        String.format(
                            "%.1f",
                            totalHours
                        )
                    }시간)"
                )
        }
    }

    /**
     * 비즈니스 규칙 검증
     */
    private fun validateBusinessRules(timeRange: TimeRange) {
        val startHour = timeToMinutes(timeRange.startTime) / 60
        val endHour = timeToMinutes(timeRange.endTime) / 60

        // 비현실적인 시간대 경고
        when {
            startHour < 5 ->
                throw AppException.TimeRangeException(
                    "오전 5시 이전 시작은 권장되지 않습니다 (건강을 위해 충분한 수면을 취하세요)"
                )

            endHour > 23 ->
                throw AppException.TimeRangeException(
                    "오후 11시 이후 종료는 권장되지 않습니다 (수면 패턴을 고려해주세요)"
                )

            startHour > 12 && endHour < 18 ->
                throw AppException.TimeRangeException(
                    "오후 늦은 시간에 시작해서 저녁 전에 끝나는 스케줄은 너무 짧습니다"
                )
        }
    }

    /**
     * 시간 범위 품질 분석
     */
    fun analyzeTimeRangeQuality(timeRange: TimeRange): TimeRangeAnalysis {
        val totalHours = timeRange.totalHours()
        val startHour = timeToMinutes(timeRange.startTime) / 60
        val endHour = timeToMinutes(timeRange.endTime) / 60

        val quality = calculateQualityScore(totalHours, startHour, endHour)
        val recommendations = generateTimeRecommendations(totalHours, startHour, endHour)
        val timeType = determineTimeType(startHour, endHour)

        return TimeRangeAnalysis(
            quality = quality,
            timeType = timeType,
            productivity = calculateProductivityScore(startHour, endHour),
            recommendations = recommendations,
            optimalBreaks = calculateOptimalBreaks(totalHours.toInt())
        )
    }

    private fun calculateQualityScore(
        totalHours: Float,
        startHour: Int,
        endHour: Int
    ): TimeQuality {
        return when {
            totalHours in RECOMMENDED_MIN_HOURS.toFloat()..RECOMMENDED_MAX_HOURS.toFloat() &&
                    startHour in 7..10 && endHour in 17..20 -> TimeQuality.EXCELLENT

            totalHours in (RECOMMENDED_MIN_HOURS - 1).toFloat()..(RECOMMENDED_MAX_HOURS + 2).toFloat() &&
                    startHour in 6..11 && endHour in 16..21 -> TimeQuality.GOOD

            totalHours >= MIN_DURATION_HOURS && totalHours <= MAX_DURATION_HOURS -> TimeQuality.ACCEPTABLE

            else -> TimeQuality.POOR
        }
    }

    private fun generateTimeRecommendations(
        totalHours: Float,
        startHour: Int,
        endHour: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            totalHours < RECOMMENDED_MIN_HOURS ->
                recommendations.add("${RECOMMENDED_MIN_HOURS}시간 이상으로 설정하면 더 효과적인 스케줄을 만들 수 있습니다")

            totalHours > RECOMMENDED_MAX_HOURS ->
                recommendations.add("${RECOMMENDED_MAX_HOURS}시간 이하로 설정하면 피로를 줄일 수 있습니다")
        }

        when {
            startHour < 7 ->
                recommendations.add("오전 7시 이후 시작하면 생체리듬에 더 좋습니다")

            startHour > 10 ->
                recommendations.add("오전에 시작하면 하루를 더 효율적으로 활용할 수 있습니다")
        }

        when {
            endHour > 20 ->
                recommendations.add("저녁 8시 이전에 마치면 개인 시간을 확보할 수 있습니다")

            endHour < 17 ->
                recommendations.add("오후 5시 이후까지 활용하면 더 많은 작업을 처리할 수 있습니다")
        }

        return recommendations
    }

    private fun determineTimeType(startHour: Int, endHour: Int): TimeType {
        return when {
            endHour <= 12 -> TimeType.MORNING
            startHour >= 18 -> TimeType.EVENING
            startHour <= 9 && endHour >= 17 -> TimeType.FULL_DAY
            else -> TimeType.PARTIAL_DAY
        }
    }

    private fun calculateProductivityScore(startHour: Int, endHour: Int): Float {
        // 생산성이 높은 시간대: 9-11시, 14-16시
        val productiveHours = (9..11).toList() + (14..16).toList()
        val totalHours = endHour - startHour
        val productiveOverlap = productiveHours.count { it in startHour..endHour }

        return if (totalHours > 0) productiveOverlap.toFloat() / totalHours else 0f
    }

    private fun calculateOptimalBreaks(totalHours: Int): Int {
        return when {
            totalHours <= 3 -> 1
            totalHours <= 6 -> 2
            totalHours <= 9 -> 3
            else -> 4
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
            throw AppException.ValidationException("시간 형식 파싱 오류: $time")
        }
    }
}

/**
 * 시간 범위 분석 결과
 */
data class TimeRangeAnalysis(
    val quality: TimeQuality,
    val timeType: TimeType,
    val productivity: Float,
    val recommendations: List<String>,
    val optimalBreaks: Int
)

enum class TimeQuality {
    EXCELLENT,   // 최적
    GOOD,        // 좋음
    ACCEPTABLE,  // 수용 가능
    POOR         // 개선 필요
}

enum class TimeType {
    MORNING,     // 오전
    EVENING,     // 저녁
    FULL_DAY,    // 하루 종일
    PARTIAL_DAY
}