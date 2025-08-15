package com.hanto.aischeduler.data.repository

import android.annotation.SuppressLint
import android.util.Log
import com.hanto.aischeduler.data.api.GroqApiService
import com.hanto.aischeduler.data.model.GroqMessage
import com.hanto.aischeduler.data.model.GroqRequest
import com.hanto.aischeduler.data.model.Task
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val groqApiService: GroqApiService
) {
    private val apiKey = "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"

    suspend fun generateSchedule(
        tasks: List<String>,
        date: String,
        startTime: String = "09:00",
        endTime: String = "18:00"
    ): List<Task> {
        try {
            val prompt = createSchedulePrompt(
                tasks = tasks,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
            val request = GroqRequest(
                messages = listOf(
                    GroqMessage(
                        role = "system",
                        content = getSystemPrompt(startTime, endTime)
                    ),
                    GroqMessage(
                        role = "user",
                        content = prompt
                    )
                )
            )

            val response = groqApiService.generateSchedule(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val scheduleText = response.body()?.choices?.firstOrNull()?.message?.content
                Log.d("ScheduleRepository", "AI Response: $scheduleText")

                return if (scheduleText.isNullOrBlank()) {
                    createDefaultSchedule(tasks, date, startTime, endTime)
                } else {
                    val parsedTasks = parseScheduleResponse(scheduleText, date)
                    parsedTasks.ifEmpty {
                        Log.d("ScheduleRepository", "Parsing failed, creating default schedule")
                        createDefaultSchedule(tasks, date, startTime, endTime)
                    }
                }
            } else {
                Log.e("ScheduleRepository", "API Error: ${response.errorBody()?.string()}")
                throw Exception("API 호출 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error: ${e.message}")
            return createDefaultSchedule(tasks, date, startTime, endTime)
        }
    }

    // 기본 스케줄 생성
    @SuppressLint("DefaultLocale")
    private fun createDefaultSchedule(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): List<Task> {
        val scheduleList = mutableListOf<Task>()

        // 시작/종료 시간 파싱
        val startTimeParts = startTime.split(":")
        var currentHour = startTimeParts[0].toInt()
        var currentMinute = startTimeParts[1].toInt()

        val endTimeParts = endTime.split(":")
        val endHour = endTimeParts[0].toInt()
        val endMinute = endTimeParts[1].toInt()
        val totalEndMinutes = endHour * 60 + endMinute

        // 시간대별 설정
        val isEveningTime = currentHour >= 18
        val isMorningTime = currentHour < 12

        tasks.forEachIndexed { index, task ->
            val currentTotalMinutes = currentHour * 60 + currentMinute

            // 종료 시간을 넘으면 중단
            if (currentTotalMinutes >= totalEndMinutes) return@forEachIndexed

            val startT = String.format("%02d:%02d", currentHour, currentMinute)

            // 작업 시간 할당 (시간대별 조정)
            val taskDuration = when {
                isEveningTime -> 60 // 저녁: 1시간씩
                isMorningTime -> 90 // 아침: 1.5시간씩
                else -> 90 // 기본: 1.5시간씩
            }

            currentMinute += taskDuration
            currentHour += currentMinute / 60
            currentMinute %= 60

            // 종료 시간을 넘지 않도록 조정
            val newTotalMinutes = currentHour * 60 + currentMinute
            if (newTotalMinutes > totalEndMinutes) {
                currentHour = endHour
                currentMinute = endMinute
            }

            val endT = String.format("%02d:%02d", currentHour, currentMinute)

            scheduleList.add(
                Task(
                    id = "${date}_${index}",
                    title = task,
                    description = "",
                    startTime = startT,
                    endTime = endT,
                    date = date
                )
            )
        }

        // 특별 이벤트 추가 (시간대별)
        addSpecialEvents(scheduleList, date, startTime, endTime, isEveningTime)

        return scheduleList.sortedBy { it.startTime }
    }


    // 특별 이벤트 추가 (식사시간 등)
    private fun addSpecialEvents(
        scheduleList: MutableList<Task>,
        date: String,
        startTime: String,
        endTime: String,
        isEveningTime: Boolean
    ) {
        val startHour = startTime.split(":")[0].toInt()
        val endHour = endTime.split(":")[0].toInt()

        when {
            // 점심시간 (12-13시 사이에 시작하는 경우)
            startHour <= 12 && endHour >= 13 && !isEveningTime -> {
                scheduleList.add(
                    Task(
                        id = "${date}_lunch",
                        title = "점심시간",
                        startTime = "12:00",
                        endTime = "13:00",
                        date = date
                    )
                )
            }
            // 저녁식사 (18-19시 사이)
            isEveningTime && startHour <= 18 && endHour >= 19 -> {
                scheduleList.add(
                    Task(
                        id = "${date}_dinner",
                        title = "저녁식사",
                        description = "맛있는 저녁 드세요",
                        startTime = "18:00",
                        endTime = "19:00",
                        date = date
                    )
                )
            }
        }
    }

    // 시스템 프롬프트
    private fun getSystemPrompt(
        startTime: String,
        endTime: String
    ): String {

        return """
You are a "Professional Schedule Planning AI".
Current time range: $startTime ~ $endTime

Goal:
- Arrange all given tasks efficiently to maximize productivity.

Constraints:
1. All tasks must start after "$startTime" and finish before "$endTime".
2. **All given tasks must be scheduled within the time range** (shorten or merge tasks if needed).
3. Output format: "HH:MM-HH:MM: Task Name"
4. Do not split tasks into multiple time slots.
5. Do not place any breaks unless they are explicitly listed as tasks.
6. Only include the provided tasks in the schedule. **Do not create or add any new tasks**.
7. Final answer must be written in Korean.

Output format:
HH:MM-HH:MM: [Exact Task Name]
...
"""
    }

    private fun createSchedulePrompt(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): String {
        val tasksText = tasks.joinToString("\n") { "- $it" }

        return """
Date: $date
Time range: $startTime ~ $endTime

Task list:
$tasksText

Important:
- Include only the tasks listed above in the schedule. Do not create any additional tasks.
- The output must be entirely in Korean.

Rules for scheduling:
1. **All tasks must be scheduled within the time range** (shorten durations if needed).
2. Each time block must have a clear start and end time.
3. Do not split tasks into multiple slots.
4. No duplicate task entries.
5. Do not add breaks unless they are given as tasks.
6. Only use the exact task names provided.

Output format:
HH:MM-HH:MM: [Exact Task Name]
"""
    }


    private fun parseScheduleResponse(response: String, date: String): List<Task> {
        val tasks = mutableListOf<Task>()
        Log.d("ScheduleRepository", "Parsing response: $response")

        // 줄바꿈과 콤마 둘 다 처리
        val allLines = response.split("\n")
        val scheduleItems = mutableListOf<String>()

        // 각 줄을 콤마로 다시 분리
        allLines.forEach { line ->
            if (line.trim().isNotEmpty()) {
                scheduleItems.addAll(line.split(","))
            }
        }

        // 시간 패턴이 있는 항목만 필터링 (정규식 개선)
        val validItems = scheduleItems.filter { item ->
            val trimmedItem = item.trim()
            val cleanItem = trimmedItem.replace("*", "").trim()
            cleanItem.matches(Regex(".*\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}:.*"))
        }

        Log.d("ScheduleRepository", "Valid schedule items: $validItems")

        for ((index, item) in validItems.withIndex()) {
            try {
                val trimmedItem = item.trim().replace("*", "").trim() // ** 제거

                // 정규식으로 시간 패턴 추출: HH:MM-HH:MM: 작업명
                val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2}):\\s*(.+)")
                val matchResult = timePattern.find(trimmedItem)

                if (matchResult != null) {
                    val startTime = matchResult.groupValues[1]
                    val endTime = matchResult.groupValues[2]
                    val fullTaskName = matchResult.groupValues[3].trim()

                    // 번호와 괄호 안의 추가 정보 제거하고 작업명만 추출
                    var taskName = fullTaskName

                    // "1. " "2. " 등 번호 제거
                    taskName = taskName.replace(Regex("^\\d+\\.\\s*"), "")

                    // 괄호 안의 정보 제거
                    taskName = taskName.split("(")[0].trim()

                    Log.d("ScheduleRepository", "파싱 성공: $startTime-$endTime, 작업: $taskName")

                    tasks.add(
                        Task(
                            id = "${date}_${index}",
                            title = taskName,
                            description = "",
                            startTime = startTime,
                            endTime = endTime,
                            date = date
                        )
                    )
                    Log.d("ScheduleRepository", "Task 추가: $startTime-$endTime: $taskName")
                } else {
                    Log.d("ScheduleRepository", "시간 패턴 매칭 실패: $trimmedItem")
                }
            } catch (e: Exception) {
                Log.e("ScheduleRepository", "Error parsing item '$item': ${e.message}")
                continue
            }
        }

        Log.d("ScheduleRepository", "최종 파싱 결과: ${tasks.size}개 tasks")
        return tasks
    }
}