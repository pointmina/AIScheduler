package com.hanto.aischeduler.data.repository

import android.annotation.SuppressLint
import android.util.Log
import com.hanto.aischeduler.BuildConfig
import com.hanto.aischeduler.data.api.GroqApiService
import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.data.model.GroqMessage
import com.hanto.aischeduler.data.model.GroqRequest
import com.hanto.aischeduler.data.model.NetworkResult
import com.hanto.aischeduler.data.model.Task
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val groqApiService: GroqApiService
) {
    private val apiKey: String by lazy {
        BuildConfig.GROQ_API_KEY.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("GROQ_API_KEY not configured in local.properties")
    }

    companion object {
        private const val TAG = "ScheduleRepository"
        private const val DEFAULT_TASK_DURATION_EVENING = 60
        private const val DEFAULT_TASK_DURATION_NORMAL = 90
        private const val RETRY_COUNT = 3
        private const val RETRY_DELAY = 1000L
    }

    /**
     * 스케줄 생성 (NetworkResult 반환)
     */
    suspend fun generateSchedule(
        tasks: List<String>,
        date: String,
        startTime: String = "09:00",
        endTime: String = "18:00"
    ): NetworkResult<List<Task>> {
        return try {
            // 입력 검증
            validateInput(tasks, startTime, endTime)

            // 재시도 로직과 함께 스케줄 생성
            val result = executeWithRetry {
                generateScheduleInternal(tasks, date, startTime, endTime)
            }

            NetworkResult.Success(result)

        } catch (e: AppException) {
            Log.e(TAG, "App exception: ${e.message}", e)
            NetworkResult.Error(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            val mappedException = mapException(e)
            NetworkResult.Error(mappedException)
        }
    }

    /**
     * 재시도 로직
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(RETRY_COUNT) { attempt ->
            try {
                Log.d(TAG, "Attempting operation (${attempt + 1}/$RETRY_COUNT)")
                return operation()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")

                // 마지막 시도가 아니면 대기 후 재시도
                if (attempt < RETRY_COUNT - 1) {
                    val delayMs = RETRY_DELAY * (attempt + 1)
                    Log.d(TAG, "Retrying in ${delayMs}ms...")
                    delay(delayMs)
                }
            }
        }

        // 모든 재시도 실패
        throw lastException ?: AppException.NetworkException("모든 재시도가 실패했습니다")
    }

    /**
     * 입력 검증 (강화된 버전)
     */
    private fun validateInput(
        tasks: List<String>,
        startTime: String,
        endTime: String
    ) {
        when {
            tasks.isEmpty() ->
                throw AppException.ValidationException("작업 목록이 비어있습니다")

            tasks.any { it.isBlank() } ->
                throw AppException.ValidationException("빈 작업이 포함되어 있습니다")

            tasks.any { it.length > 100 } ->
                throw AppException.ValidationException("작업 제목은 100자를 초과할 수 없습니다")

            !isValidTimeFormat(startTime) ->
                throw AppException.ValidationException("시작 시간 형식이 올바르지 않습니다: $startTime")

            !isValidTimeFormat(endTime) ->
                throw AppException.ValidationException("종료 시간 형식이 올바르지 않습니다: $endTime")

            timeToMinutes(endTime) <= timeToMinutes(startTime) ->
                throw AppException.TimeRangeException("종료 시간이 시작 시간보다 늦어야 합니다")

            (timeToMinutes(endTime) - timeToMinutes(startTime)) < 60 ->
                throw AppException.TimeRangeException("최소 1시간 이상의 시간 범위가 필요합니다")

            (timeToMinutes(endTime) - timeToMinutes(startTime)) > 16 * 60 ->
                throw AppException.TimeRangeException("16시간을 초과하는 스케줄은 생성할 수 없습니다")
        }
    }

    /**
     * 예외 매핑
     */
    private fun mapException(exception: Exception): AppException {
        return when (exception) {
            is HttpException -> {
                AppException.ApiException(
                    code = exception.code(),
                    message = exception.message()
                )
            }

            is SocketTimeoutException -> {
                AppException.TimeoutException()
            }

            is IOException -> {
                AppException.NetworkException(
                    message = "네트워크 연결 오류",
                    cause = exception
                )
            }

            else -> {
                AppException.NetworkException(
                    message = "알 수 없는 오류: ${exception.message}",
                    cause = exception
                )
            }
        }
    }

    /**
     * 시간 형식 검증 (HH:MM)
     */
    private fun isValidTimeFormat(time: String): Boolean {
        return time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return try {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            hour * 60 + minute
        } catch (e: Exception) {
            throw AppException.ValidationException("시간 형식 파싱 오류: $time")
        }
    }

    /**
     * 실제 스케줄 생성 로직
     */
    private suspend fun generateScheduleInternal(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): List<Task> {
        val prompt = createSchedulePrompt(tasks, date, startTime, endTime)
        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = getSystemPrompt(startTime, endTime)),
                GroqMessage(role = "user", content = prompt)
            )
        )

        val response = groqApiService.generateSchedule(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "API Error: ${response.code()} - $errorBody")
            throw Exception("API 호출 실패 (${response.code()}): ${response.message()}")
        }

        val scheduleText = response.body()?.choices?.firstOrNull()?.message?.content

        if (scheduleText.isNullOrBlank()) {
            throw Exception("AI 응답이 비어있습니다")
        }

        Log.d(TAG, "AI Response: $scheduleText")

        val parsedTasks = parseScheduleResponse(scheduleText, date)
        return parsedTasks.ifEmpty {
            Log.w(TAG, "Parsing failed, using default schedule")
            createDefaultSchedule(tasks, date, startTime, endTime)
        }
    }

    /**
     * 기본 스케줄 생성 (AI 실패 시 fallback)
     */
    @SuppressLint("DefaultLocale")
    private fun createDefaultSchedule(
        tasks: List<String>,
        date: String,
        startTime: String,
        endTime: String
    ): List<Task> {
        Log.d(TAG, "Creating default schedule for ${tasks.size} tasks")

        val scheduleList = mutableListOf<Task>()
        val startTimeParts = startTime.split(":")
        var currentHour = startTimeParts[0].toInt()
        var currentMinute = startTimeParts[1].toInt()

        val endTimeParts = endTime.split(":")
        val endHour = endTimeParts[0].toInt()
        val endMinute = endTimeParts[1].toInt()
        val totalEndMinutes = endHour * 60 + endMinute

        val isEveningTime = currentHour >= 18
        val taskDuration = if (isEveningTime) 60 else 90

        tasks.forEachIndexed { index, task ->
            val currentTotalMinutes = currentHour * 60 + currentMinute

            if (currentTotalMinutes >= totalEndMinutes) {
                Log.w(TAG, "시간 제한 도달, $index 번째 작업에서 중단")
                return@forEachIndexed
            }

            val startT = String.format("%02d:%02d", currentHour, currentMinute)

            currentMinute += taskDuration
            currentHour += currentMinute / 60
            currentMinute %= 60

            val newTotalMinutes = currentHour * 60 + currentMinute
            if (newTotalMinutes > totalEndMinutes) {
                currentHour = endHour
                currentMinute = endMinute
            }

            val endT = String.format("%02d:%02d", currentHour, currentMinute)

            scheduleList.add(
                Task(
                    id = "${date}_default_${index}",
                    title = task,
                    description = "기본 스케줄로 생성됨",
                    startTime = startT,
                    endTime = endT,
                    date = date
                )
            )
        }

        return scheduleList.sortedBy { it.startTime }
    }

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

    private fun getSystemPrompt(startTime: String, endTime: String): String {
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

        try {
            val allLines = response.split("\n")
            val scheduleItems = mutableListOf<String>()

            allLines.forEach { line ->
                if (line.trim().isNotEmpty()) {
                    scheduleItems.addAll(line.split(","))
                }
            }

            val validItems = scheduleItems.filter { item ->
                val trimmedItem = item.trim().replace("*", "").trim()
                trimmedItem.matches(Regex(".*\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}:.*"))
            }

            validItems.forEachIndexed { index, item ->
                try {
                    val trimmedItem = item.trim().replace("*", "").trim()
                    val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2}):\\s*(.+)")
                    val matchResult = timePattern.find(trimmedItem)

                    if (matchResult != null) {
                        val startTime = matchResult.groupValues[1]
                        val endTime = matchResult.groupValues[2]
                        val taskName = matchResult.groupValues[3].trim()
                            .replace(Regex("^\\d+\\.\\s*"), "")
                            .split("(")[0].trim()

                        tasks.add(
                            Task(
                                id = "${date}_ai_${index}",
                                title = taskName,
                                description = "AI로 생성됨",
                                startTime = startTime,
                                endTime = endTime,
                                date = date
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "항목 파싱 실패: $item", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "응답 파싱 중 오류", e)
            throw AppException.ParseException("AI 응답 파싱에 실패했습니다")
        }

        return tasks
    }
}