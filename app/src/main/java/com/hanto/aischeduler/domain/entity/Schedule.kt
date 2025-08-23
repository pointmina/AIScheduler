package com.hanto.aischeduler.domain.entity

import android.annotation.SuppressLint
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.data.model.TaskCategory
import com.hanto.aischeduler.data.model.TaskPriority

data class Schedule(
    val id: String,
    val date: String,
    val tasks: List<Task>,
    val timeRange: TimeRange,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val metadata: ScheduleMetadata = ScheduleMetadata()
) {
    /**
     * 스케줄의 완료율 계산
     */
    fun getCompletionRate(): Float {
        return if (tasks.isNotEmpty()) {
            tasks.count { it.isCompleted }.toFloat() / tasks.size
        } else 0f
    }

    /**
     * 총 스케줄 시간 계산 (분)
     */
    fun getTotalDuration(): Int {
        return tasks.sumOf { it.getDurationMinutes() }
    }

    /**
     * 완료된 작업들만 반환
     */
    fun getCompletedTasks(): List<Task> {
        return tasks.filter { it.isCompleted }
    }

    /**
     * 미완료 작업들만 반환
     */
    fun getPendingTasks(): List<Task> {
        return tasks.filter { !it.isCompleted }
    }

    /**
     * 시간 충돌이 있는 작업들 찾기
     */
    fun getConflictingTasks(): List<Pair<Task, Task>> {
        val conflicts = mutableListOf<Pair<Task, Task>>()
        val sortedTasks = tasks.sortedBy { it.startTime }

        for (i in 0 until sortedTasks.size - 1) {
            for (j in i + 1 until sortedTasks.size) {
                if (sortedTasks[i].overlapsWith(sortedTasks[j])) {
                    conflicts.add(Pair(sortedTasks[i], sortedTasks[j]))
                }
            }
        }

        return conflicts
    }

    /**
     * 카테고리별 작업 분포
     */
    fun getTasksByCategory(): Map<TaskCategory, List<Task>> {
        return tasks.groupBy { it.category }
    }

    /**
     * 우선순위별 작업 분포
     */
    fun getTasksByPriority(): Map<TaskPriority, List<Task>> {
        return tasks.groupBy { it.priority }
    }

    /**
     * 스케줄이 유효한지 검증
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                date.isNotBlank() &&
                timeRange.isValid() &&
                tasks.isNotEmpty() &&
                getConflictingTasks().isEmpty()
    }

    /**
     * 스케줄 효율성 점수 계산 (0-100)
     */
    fun getEfficiencyScore(): Int {
        val completionScore = getCompletionRate() * 40 // 완료율 40점
        val utilizationScore =
            (getTotalDuration().toFloat() / timeRange.totalMinutes()) * 30 // 시간 활용률 30점
        val balanceScore = calculateBalanceScore() * 20 // 균형성 20점
        val conflictPenalty = getConflictingTasks().size * 5 // 충돌 감점 10점

        return (completionScore + utilizationScore + balanceScore - conflictPenalty)
            .coerceIn(0f, 100f).toInt()
    }

    /**
     * 스케줄 요약 정보
     */
    @SuppressLint("DefaultLocale")
    fun getSummary(): String {
        val completionRate = (getCompletionRate() * 100).toInt()
        val totalHours = getTotalDuration() / 60f
        return "$date: ${tasks.size}개 작업, ${
            String.format(
                "%.1f",
                totalHours
            )
        }시간, 완료율 $completionRate%"
    }

    private fun calculateBalanceScore(): Float {

        if (tasks.isEmpty()) return 0f

        val durations = tasks.map { it.getDurationMinutes() }
        val average = durations.average()
        val variance = durations.map { (it - average) * (it - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)

        return (100 - standardDeviation.coerceAtMost(100.0)).toFloat() / 100f
    }
}

data class ScheduleMetadata(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val totalDuration: Int = 0, // 분
    val efficiency: Float = 0f, // 0.0 - 1.0
    val conflicts: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
) {
    fun getSummary(): String {
        val efficiencyPercent = (efficiency * 100).toInt()
        return "작업 $completedTasks/$totalTasks 완료, 효율성 $efficiencyPercent%, 충돌 ${conflicts.size}개"
    }

    fun isValid(): Boolean {
        return totalTasks >= 0 &&
                completedTasks >= 0 &&
                completedTasks <= totalTasks &&
                totalDuration >= 0 &&
                efficiency in 0f..1f
    }
}