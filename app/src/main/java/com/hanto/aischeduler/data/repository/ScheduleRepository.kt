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
                        content = "당신은 일정 관리 전문가입니다. 사용자의 할 일 목록을 받아서 하루 일정을 시간대별로 효율적으로 배치해주세요."
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

                // 응답이 짧을 때 기본 스케줄 생성
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
            // 에러 발생시 기본 스케줄 반환
            return createDefaultSchedule(tasks, date, startTime, endTime)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun createDefaultSchedule(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): List<Task> {
        val scheduleList = mutableListOf<Task>()
        var currentHour = 9
        var currentMinute = 0

        tasks.forEachIndexed { index, task ->
            val startT = String.format("%02d:%02d", currentHour, currentMinute)

            // 각 작업에 1.5시간 할당
            currentMinute += 90
            currentHour += currentMinute / 60
            currentMinute %= 60

            val endT = String.format("%02d:%02d", currentHour, currentMinute)

            scheduleList.add(
                Task(
                    id = "${date}_${index}",
                    title = task,
                    description = "AI가 추천한 시간",
                    startTime = startT,
                    endTime = endT,
                    date = date
                )
            )

            // 점심시간 추가 (12시쯤)
            if (currentHour >= 12 && scheduleList.none { it.title.contains("점심") }) {
                scheduleList.add(
                    Task(
                        id = "${date}_lunch",
                        title = "점심시간",
                        description = "맛있는 점심 드세요",
                        startTime = "12:00",
                        endTime = "13:00",
                        date = date
                    )
                )
                currentHour = 13
                currentMinute = 0
            }
        }

        return scheduleList
    }

    private fun createSchedulePrompt(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): String {
        val tasksText = tasks.joinToString("\n") { "- $it" }
        return """
오늘 날짜: $date
해야 할 일:
$tasksText

"위 할 일들을 ${startTime}부터 $endTime 사이에 효율적으로 배치해서"
반드시 다음 형식으로 여러 줄에 걸쳐 답변해주세요:

09:00-10:30: 첫 번째 작업
10:30-11:00: 휴식
11:00-12:00: 두 번째 작업
12:00-13:00: 점심시간
13:00-15:00: 세 번째 작업

각 작업은 최소 30분, 최대 2시간으로 배치해주세요.
점심시간(12:00-13:00)은 꼭 포함해주세요.
        """.trimIndent()
    }

    private fun parseScheduleResponse(response: String, date: String): List<Task> {
        val tasks = mutableListOf<Task>()
        Log.d("ScheduleRepository", "Parsing response: $response")

        val lines = response.split("\n").filter {
            it.contains(":") && it.contains("-") && it.trim().isNotEmpty()
        }

        Log.d("ScheduleRepository", "Filtered lines: $lines")

        for ((index, line) in lines.withIndex()) {
            try {
                // "09:00-10:00: 작업명" 형식 파싱
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val timePart = parts[0].trim()
                    val taskName = parts[1].trim()

                    Log.d(
                        "ScheduleRepository",
                        "Parsing line: timePart='$timePart', taskName='$taskName'"
                    )

                    if (timePart.contains("-") && taskName.isNotEmpty()) {
                        val times = timePart.split("-")
                        if (times.size == 2) {
                            val startTime = times[0].trim()
                            val endTime = times[1].trim()

                            tasks.add(
                                Task(
                                    id = "${date}_${index}",
                                    title = taskName,
                                    description = "AI가 생성한 일정",
                                    startTime = startTime,
                                    endTime = endTime,
                                    date = date
                                )
                            )
                            Log.d(
                                "ScheduleRepository",
                                "Added task: $startTime-$endTime: $taskName"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScheduleRepository", "Error parsing line '$line': ${e.message}")
                continue
            }
        }

        Log.d("ScheduleRepository", "Parsed ${tasks.size} tasks")
        return tasks
    }
}