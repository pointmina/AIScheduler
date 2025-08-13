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

    //기본 스케줄 생성 - 시간대 파라미터 적용
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

        // 시간대별 휴식시간 설정
        val isEveningTime = currentHour >= 18
        val isMorningTime = currentHour < 12

        tasks.forEachIndexed { index, task ->
            val currentTotalMinutes = currentHour * 60 + currentMinute

            // 종료 시간을 넘으면 중단
            if (currentTotalMinutes >= totalEndMinutes) return@forEachIndexed

            val startT = String.format("%02d:%02d", currentHour, currentMinute)

            // 작업 시간 할당 (시간대별 조정)
            val taskDuration = when {
                isEveningTime -> 60
                isMorningTime -> 90
                else -> 90
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
                    description = getTaskDescription(task, isEveningTime),
                    startTime = startT,
                    endTime = endT,
                    date = date
                )
            )

            // 시간대별 휴식시간 추가
            if (index < tasks.size - 1) {
                addRestTime(scheduleList, date, currentHour, currentMinute, isEveningTime)

                // 휴식 시간만큼 현재 시간 증가
                currentMinute += if (isEveningTime) 15 else 30
                currentHour += currentMinute / 60
                currentMinute %= 60
            }
        }

        // 특별 이벤트 추가 (시간대별)
        addSpecialEvents(scheduleList, date, startTime, endTime, isEveningTime)

        return scheduleList.sortedBy { it.startTime }
    }

    // 시간대별 작업 설명 생성
    private fun getTaskDescription(task: String, isEveningTime: Boolean): String {
        return when {
            isEveningTime -> "저녁 시간 - 편안하게 진행하세요"
            task.contains("운동", ignoreCase = true) -> "몸과 마음을 건강하게!"
            task.contains("공부", ignoreCase = true) -> "집중해서 학습해보세요"
            else -> "AI가 추천한 시간"
        }
    }

    // 휴식시간 추가
    private fun addRestTime(
        scheduleList: MutableList<Task>,
        date: String,
        hour: Int,
        minute: Int,
        isEveningTime: Boolean
    ) {
        val restDuration = if (isEveningTime) 15 else 30
        val restStartTime = String.format("%02d:%02d", hour, minute)

        var restEndMinute = minute + restDuration
        var restEndHour = hour + restEndMinute / 60
        restEndMinute %= 60

        val restEndTime = String.format("%02d:%02d", restEndHour, restEndMinute)

        scheduleList.add(
            Task(
                id = "${date}_rest_${scheduleList.size}",
                title = if (isEveningTime) "간단한 휴식" else "커피 타임",
                description = if (isEveningTime) "잠깐 쉬어가세요" else "재충전 시간",
                startTime = restStartTime,
                endTime = restEndTime,
                date = date
            )
        )
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
                        description = "맛있는 점심 드세요",
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

    // 시스템 프롬프트 - 시간대 정보 포함
    private fun getSystemPrompt(startTime: String, endTime: String): String {
        val hour = startTime.split(":")[0].toInt()
        val timeContext = when {
            hour >= 19 -> "저녁 시간대로 가벼운 활동과 휴식 위주로 배치해주세요."
            hour >= 12 -> "오후 시간대로 집중력이 필요한 작업을 적절히 배치해주세요."
            hour >= 6 -> "아침 시간대로 중요한 업무를 앞쪽에 배치해주세요."
            else -> "심야 시간대로 가벼운 활동만 포함해주세요."
        }

        return """
당신은 효율적인 일정 관리 전문가입니다.
현재 시간대: $startTime ~ $endTime
시간대 특성: $timeContext

다음 규칙에 따라 시간대별 스케줄을 생성해주세요:
1. 반드시 "$startTime" 이후부터 시작
2. "$endTime" 이전에 모든 일정 완료
3. 반드시 "HH:MM-HH:MM: 작업명" 형식으로 응답
4. 각 작업은 30분~2시간 사이로 배치
5. 저녁 시간대라면 점심시간 대신 간단한 휴식시간 포함
        """.trimIndent()
    }

    // 프롬프트 생성
    private fun createSchedulePrompt(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): String {
        val tasksText = tasks.joinToString("\n") { "- $it" }
        val hour = startTime.split(":")[0].toInt()

        val timeAdvice = when {
            hour >= 19 -> "저녁 시간이므로 무리하지 말고 편안한 페이스로 진행하세요."
            hour >= 12 -> "오후 시간이므로 적당한 휴식을 포함해주세요."
            hour < 9 -> "이른 시간이므로 가벼운 활동으로 시작하세요."
            else -> "집중할 수 있는 시간대입니다."
        }

        return """
오늘 날짜: $date
시간 범위: $startTime ~ $endTime
조언: $timeAdvice

해야 할 일:
$tasksText

위 할 일들을 ${startTime}부터 $endTime 사이에 효율적으로 배치해서
반드시 다음 형식으로 여러 줄에 걸쳐 답변해주세요:

$startTime-XX:XX: 첫 번째 작업
XX:XX-XX:XX: 휴식
XX:XX-XX:XX: 두 번째 작업

주의사항:
- 반드시 $startTime 부터 시작
- $endTime 전에 완료
- 각 작업은 최소 30분, 최대 2시간
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
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val timePart = parts[0].trim()
                    val taskName = parts[1].trim()

                    Log.d("ScheduleRepository", "Parsing line: timePart='$timePart', taskName='$taskName'")

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
                            Log.d("ScheduleRepository", "Added task: $startTime-$endTime: $taskName")
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