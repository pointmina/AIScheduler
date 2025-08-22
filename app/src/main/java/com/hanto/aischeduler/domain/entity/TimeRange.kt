package com.hanto.aischeduler.domain.entity

data class TimeRange(
    val startTime: String, // "09:00"
    val endTime: String    // "18:00"
) {
    /**
     * 전체 시간 범위의 분 단위 계산
     */
    fun totalMinutes(): Int {
        return timeToMinutes(endTime) - timeToMinutes(startTime)
    }

    /**
     * 전체 시간 범위의 시간 단위 계산 (소수점 포함)
     */
    fun totalHours(): Float {
        return totalMinutes() / 60f
    }

    /**
     * 시간 범위가 유효한지 검증
     */
    fun isValid(): Boolean {
        return try {
            val start = timeToMinutes(startTime)
            val end = timeToMinutes(endTime)
            end > start && start >= 0 && end <= 24 * 60
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 특정 시간이 이 범위 안에 있는지 확인
     */
    fun contains(time: String): Boolean {
        return try {
            val timeMinutes = timeToMinutes(time)
            val start = timeToMinutes(startTime)
            val end = timeToMinutes(endTime)
            timeMinutes >= start && timeMinutes <= end
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 사용자 친화적인 문자열 반환
     */
    fun toDisplayString(): String {
        return "$startTime ~ $endTime (${totalHours()}시간)"
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        require(parts.size == 2) { "Invalid time format: $time" }

        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        require(hour in 0..23) { "Invalid hour: $hour" }
        require(minute in 0..59) { "Invalid minute: $minute" }

        return hour * 60 + minute
    }

    companion object {
        /**
         * 기본 시간 범위들
         */
        val WORK_DAY = TimeRange("09:00", "18:00")
        val FULL_DAY = TimeRange("09:00", "22:00")
        val EVENING = TimeRange("19:00", "22:00")
        val MORNING = TimeRange("06:00", "12:00")

        /**
         * 문자열에서 TimeRange 생성
         */
        fun fromString(startTime: String, endTime: String): TimeRange {
            val timeRange = TimeRange(startTime, endTime)
            require(timeRange.isValid()) { "Invalid time range: $startTime - $endTime" }
            return timeRange
        }
    }
}