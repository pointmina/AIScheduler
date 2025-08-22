package com.hanto.aischeduler.domain.entity

data class ScheduleStatistics(
    val totalSchedules: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val totalHours: Float,
    val averageEfficiency: Float,
    val mostProductiveHour: Int, // 0-23
    val categoryDistribution: Map<TaskCategory, Int>,
    val priorityDistribution: Map<TaskPriority, Int>,
    val weeklyCompletion: List<Float>, // 7일간의 완료율
    val trends: StatisticsTrends
) {
    /**
     * 전체 완료율 계산
     */
    fun getOverallCompletionRate(): Float {
        return if (totalTasks > 0) {
            completedTasks.toFloat() / totalTasks
        } else 0f
    }

    /**
     * 평균 일일 작업 수
     */
    fun getAverageDailyTasks(): Float {
        return if (totalSchedules > 0) {
            totalTasks.toFloat() / totalSchedules
        } else 0f
    }

    /**
     * 통계 요약 정보
     */
    fun getSummary(): String {
        val completionRate = (getOverallCompletionRate() * 100).toInt()
        return "총 ${totalSchedules}개 스케줄, 완료율 ${completionRate}%, 평균 효율성 ${(averageEfficiency * 100).toInt()}%"
    }
}

data class StatisticsTrends(
    val completionTrend: TrendDirection, // 완료율 추세
    val efficiencyTrend: TrendDirection, // 효율성 추세
    val workloadTrend: TrendDirection,   // 작업량 추세
    val suggestions: List<String> = emptyList() // 개선 제안
)

enum class TrendDirection {
    IMPROVING,    // 개선됨
    STABLE,       // 안정적
    DECLINING     // 하락함
}