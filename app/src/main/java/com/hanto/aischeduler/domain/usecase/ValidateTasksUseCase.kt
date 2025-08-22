package com.hanto.aischeduler.domain.usecase

import com.hanto.aischeduler.data.model.AppException
import javax.inject.Inject

/**
 * 작업 목록 검증을 담당하는 Use Case
 *
 * 단일 책임 원칙(SRP)에 따라 작업 검증만 담당
 * 비즈니스 로직을 Domain Layer에 집중
 */
class ValidateTasksUseCase @Inject constructor() {

    companion object {
        private const val MAX_TASKS = 20
        private const val MIN_TASKS = 1
        private const val MAX_TASK_LENGTH = 100
        private const val MIN_TASK_LENGTH = 2
    }

    /**
     * 작업 목록 검증 실행
     * @param tasks 검증할 작업 목록
     * @throws AppException.ValidationException 검증 실패 시
     */
    operator fun invoke(tasks: List<String>) {
        validateTaskCount(tasks)
        validateTaskContent(tasks)
        validateTaskUniqueness(tasks)
    }

    /**
     * 작업 개수 검증
     */
    private fun validateTaskCount(tasks: List<String>) {
        when {
            tasks.isEmpty() ->
                throw AppException.ValidationException("작업 목록이 비어있습니다")

            tasks.size < MIN_TASKS ->
                throw AppException.ValidationException("최소 ${MIN_TASKS}개의 작업이 필요합니다")

            tasks.size > MAX_TASKS ->
                throw AppException.ValidationException("작업은 최대 ${MAX_TASKS}개까지 가능합니다")
        }
    }

    /**
     * 작업 내용 검증
     */
    private fun validateTaskContent(tasks: List<String>) {
        tasks.forEachIndexed { index, task ->
            when {
                task.isBlank() ->
                    throw AppException.ValidationException("${index + 1}번째 작업이 비어있습니다")

                task.trim().length < MIN_TASK_LENGTH ->
                    throw AppException.ValidationException("작업 제목은 최소 ${MIN_TASK_LENGTH}자 이상이어야 합니다")

                task.length > MAX_TASK_LENGTH ->
                    throw AppException.ValidationException("작업 제목은 ${MAX_TASK_LENGTH}자를 초과할 수 없습니다 (현재: ${task.length}자)")

                containsInvalidCharacters(task) ->
                    throw AppException.ValidationException("작업 제목에 허용되지 않는 특수문자가 포함되어 있습니다")
            }
        }
    }

    /**
     * 작업 중복 검증
     */
    private fun validateTaskUniqueness(tasks: List<String>) {
        val trimmedTasks = tasks.map { it.trim().lowercase() }
        val duplicates = trimmedTasks
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            val duplicateList = duplicates.joinToString(", ")
            throw AppException.ValidationException("중복된 작업이 있습니다: $duplicateList")
        }
    }

    /**
     * 허용되지 않는 특수문자 검사
     */
    private fun containsInvalidCharacters(task: String): Boolean {
        // 기본적인 특수문자는 허용하되, 시스템에 위험한 문자들만 차단
        val invalidChars = listOf("<", ">", "|", "\"", "*", "?", ":", "\\")
        return invalidChars.any { task.contains(it) }
    }

    /**
     * 작업 목록의 예상 소요시간 계산 (분 단위)
     * 검증과 함께 유용한 정보 제공
     */
    fun estimateTotalDuration(tasks: List<String>): Int {
        return tasks.sumOf { task: String ->
            when {
                task.length < 10 -> 30  // 간단한 작업: 30분
                task.length < 30 -> 60  // 보통 작업: 1시간
                task.length < 50 -> 90  // 복잡한 작업: 1.5시간
                else -> 120             // 매우 복잡한 작업: 2시간
            }.toInt()
        }
    }

    /**
     * 작업의 복잡도 분석
     */
    fun analyzeTaskComplexity(tasks: List<String>): TaskComplexityAnalysis {
        val simple = tasks.count { it.length < 20 }
        val medium = tasks.count { it.length in 20..40 }
        val complex = tasks.count { it.length > 40 }

        return TaskComplexityAnalysis(
            simpleCount = simple,
            mediumCount = medium,
            complexCount = complex,
            averageLength = tasks.map { it.length }.average().toInt(),
            recommendations = generateRecommendations(simple, medium, complex)
        )
    }

    private fun generateRecommendations(simple: Int, medium: Int, complex: Int): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            complex > simple + medium ->
                recommendations.add("복잡한 작업이 많습니다. 작업을 세분화하는 것을 고려해보세요.")

            simple > medium + complex ->
                recommendations.add("간단한 작업들을 묶어서 효율성을 높일 수 있습니다.")

            medium == 0 && (simple > 0 || complex > 0) ->
                recommendations.add("작업의 난이도가 극단적입니다. 중간 단계 작업을 추가해보세요.")
        }

        return recommendations
    }
}

/**
 * 작업 복잡도 분석 결과
 */
data class TaskComplexityAnalysis(
    val simpleCount: Int,
    val mediumCount: Int,
    val complexCount: Int,
    val averageLength: Int,
    val recommendations: List<String>
) {
    fun getTotalTasks(): Int = simpleCount + mediumCount + complexCount

    fun getComplexityRatio(): String {
        val total = getTotalTasks()
        return if (total > 0) {
            "간단:${simpleCount} 보통:${mediumCount} 복잡:${complexCount}"
        } else "작업 없음"
    }
}