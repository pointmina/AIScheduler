package com.hanto.aischeduler.domain.entity

data class ScheduleRequest(
    val tasks: List<String>,
    val timeRange: TimeRange,
    val date: String,
    val preferences: SchedulePreferences = SchedulePreferences()
) {
    /**
     * 요청이 유효한지 검증
     */
    fun isValid(): Boolean {
        return tasks.isNotEmpty() &&
                timeRange.isValid() &&
                date.isNotBlank() &&
                isValidDateFormat(date)
    }

    /**
     * 작업당 평균 시간 계산
     */
    fun getAverageTaskDuration(): Int {
        return if (tasks.isNotEmpty()) {
            timeRange.totalMinutes() / tasks.size
        } else 0
    }

    /**
     * 요청 요약 정보
     */
    fun getSummary(): String {
        return "${tasks.size}개 작업, ${timeRange.toDisplayString()}, $date"
    }

    private fun isValidDateFormat(date: String): Boolean {
        return date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    }
}

data class SchedulePreferences(
    val optimizationType: OptimizationType = OptimizationType.BALANCED,
    val breakDuration: Int = 15, // 분
    val maxTaskDuration: Int = 120, // 분 (2시간)
    val allowOvertime: Boolean = false,
    val prioritizeMorning: Boolean = true,
    val includeBreaks: Boolean = true
) {
    /**
     * 설정이 유효한지 검증
     */
    fun isValid(): Boolean {
        return breakDuration in 5..60 &&
                maxTaskDuration in 30..300
    }

    /**
     * 사용자 친화적인 설정 설명
     */
    fun getDescription(): String {
        val type = when (optimizationType) {
            OptimizationType.PRODUCTIVITY -> "생산성 우선"
            OptimizationType.BALANCED -> "균형 잡힌"
            OptimizationType.ENERGY -> "에너지 고려"
            OptimizationType.FLEXIBLE -> "유연한"
        }

        return "$type 스케줄, ${breakDuration}분 휴식, 최대 ${maxTaskDuration}분 작업"
    }
}

enum class OptimizationType {
    PRODUCTIVITY,  // 생산성 우선 - 복잡한 작업을 에너지가 높은 시간에
    BALANCED,      // 균형 잡힌 - 작업과 휴식의 균형
    ENERGY,        // 에너지 레벨 고려 - 개인의 에너지 패턴에 맞춤
    FLEXIBLE       // 유연한 - 변경 가능성을 고려한 여유 있는 스케줄
}