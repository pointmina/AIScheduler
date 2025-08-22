package com.hanto.aischeduler.domain.entity

data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val date: String,      // "2024-01-15"
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val category: TaskCategory = TaskCategory.GENERAL
) {
    /**
     * 작업 소요시간 계산 (분 단위)
     */
    fun getDurationMinutes(): Int {
        val startMinutes = timeToMinutes(startTime)
        val endMinutes = timeToMinutes(endTime)
        return endMinutes - startMinutes
    }

    /**
     * 작업이 특정 시간과 겹치는지 확인
     */
    fun overlapsWith(other: Task): Boolean {
        val thisStart = timeToMinutes(startTime)
        val thisEnd = timeToMinutes(endTime)
        val otherStart = timeToMinutes(other.startTime)
        val otherEnd = timeToMinutes(other.endTime)

        return thisStart < otherEnd && thisEnd > otherStart
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}

enum class TaskPriority {
    LOW,        // 낮음
    NORMAL,     // 보통
    HIGH,       // 높음
    URGENT      // 긴급
}

enum class TaskCategory {
    WORK,       // 업무
    PERSONAL,   // 개인
    HEALTH,     // 건강
    LEARNING,   // 학습
    GENERAL     // 일반
}