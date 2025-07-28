package com.hanto.aischeduler.data

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


    suspend fun generateSchedule(tasks: List<String>, date: String): List<Task> {
        try {
            val prompt = createSchedulePrompt(tasks, date)
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
                authorization = "Bearer $apiKey}",
                request = request
            )

            if (response.isSuccessful) {
                val scheduleText = response.body()?.choices?.firstOrNull()?.message?.content
                return parseScheduleResponse(scheduleText ?: "", date)
            } else {
                throw Exception("API 호출 실패: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            throw Exception("일정 생성 실패: ${e.message}")
        }
    }

    private fun createSchedulePrompt(tasks: List<String>, date: String): String {
        val tasksText = tasks.joinToString("\n") { "- $it" }
        return """
오늘 날짜: $date
해야 할 일:
$tasksText

위 할 일들을 9시부터 18시 사이에 효율적으로 배치해서 시간표를 만들어주세요.
다음 형식으로만 답변해주세요:

09:00-10:00: 작업명
10:00-11:00: 작업명
11:00-12:00: 작업명
12:00-13:00: 점심시간
13:00-14:00: 작업명

각 작업은 최소 30분, 최대 2시간으로 배치해주세요.
점심시간(12:00-13:00)은 꼭 포함해주세요.
        """.trimIndent()
    }

    private fun parseScheduleResponse(response: String, date: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val lines = response.split("\n").filter { it.contains(":") && it.contains("-") }

        for (line in lines) {
            try {
                // "09:00-10:00: 작업명" 형식 파싱
                val timePart = line.substringBefore(":").trim()
                val taskName = line.substringAfter(":", "").trim()

                if (timePart.contains("-") && taskName.isNotEmpty()) {
                    val times = timePart.split("-")
                    if (times.size == 2) {
                        val startTime = times[0].trim()
                        val endTime = times[1].trim()

                        tasks.add(
                            Task(
                                id = "${date}_${startTime}",
                                title = taskName,
                                startTime = startTime,
                                endTime = endTime,
                                date = date
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // 파싱 실패한 라인은 무시
                continue
            }
        }

        return tasks
    }
}