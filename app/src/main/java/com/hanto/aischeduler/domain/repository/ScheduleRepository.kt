package com.hanto.aischeduler.domain.repository

import android.annotation.SuppressLint
import android.util.Log
import com.hanto.aischeduler.BuildConfig
import com.hanto.aischeduler.data.api.GroqApiService
import com.hanto.aischeduler.data.model.AppException
import com.hanto.aischeduler.data.model.GroqMessage
import com.hanto.aischeduler.data.model.GroqRequest
import com.hanto.aischeduler.data.model.NetworkResult
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.data.model.TaskCategory
import com.hanto.aischeduler.data.model.TaskPriority
import com.hanto.aischeduler.domain.entity.Schedule
import com.hanto.aischeduler.domain.entity.ScheduleRequest
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

    suspend fun generateSchedule(request: ScheduleRequest): NetworkResult<Schedule> {
        val dataTasksResult = generateSchedule(
            tasks = request.tasks,
            date = request.date,
            startTime = request.timeRange.startTime,
            endTime = request.timeRange.endTime
        )

        return when (dataTasksResult) {
            is NetworkResult.Success -> {
                val schedule = convertToScheduleEntity(dataTasksResult.data, request)
                NetworkResult.Success(schedule)
            }

            is NetworkResult.Error -> NetworkResult.Error(dataTasksResult.exception)
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }

    // 기존 메서드 (List<String> -> List<Task>)
    suspend fun generateSchedule(
        tasks: List<String>,
        date: String,
        startTime: String = "09:00",
        endTime: String = "18:00"
    ): NetworkResult<List<Task>> {
        return try {
            validateInput(tasks, startTime, endTime)
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

    private fun convertToScheduleEntity(dataTasks: List<Task>, request: ScheduleRequest): Schedule {
        val domainTasks = dataTasks.map { dataTask ->
            Task(
                id = dataTask.id,
                title = dataTask.title,
                description = dataTask.description,
                startTime = dataTask.startTime,
                endTime = dataTask.endTime,
                date = dataTask.date,
                isCompleted = dataTask.isCompleted,
                priority = mapPriority(dataTask.title),
                category = mapCategory(dataTask.title)
            )
        }

        return Schedule(
            id = "schedule_${request.date}_${System.currentTimeMillis()}",
            date = request.date,
            tasks = domainTasks,
            timeRange = request.timeRange
        )
    }

    private fun mapPriority(title: String): TaskPriority {
        return when {
            title.contains("중요", ignoreCase = true) ||
                    title.contains("urgent", ignoreCase = true) ||
                    title.contains("긴급", ignoreCase = true) -> TaskPriority.URGENT

            title.contains("회의", ignoreCase = true) ||
                    title.contains("미팅", ignoreCase = true) ||
                    title.contains("발표", ignoreCase = true) -> TaskPriority.HIGH

            title.contains("검토", ignoreCase = true) ||
                    title.contains("확인", ignoreCase = true) -> TaskPriority.LOW

            else -> TaskPriority.NORMAL
        }
    }

    private fun mapCategory(title: String): TaskCategory {
        return when {
            title.contains("운동", ignoreCase = true) ||
                    title.contains("헬스", ignoreCase = true) ||
                    title.contains("요가", ignoreCase = true) -> TaskCategory.HEALTH

            title.contains("회의", ignoreCase = true) ||
                    title.contains("업무", ignoreCase = true) ||
                    title.contains("프로젝트", ignoreCase = true) -> TaskCategory.WORK

            title.contains("공부", ignoreCase = true) ||
                    title.contains("학습", ignoreCase = true) ||
                    title.contains("독서", ignoreCase = true) -> TaskCategory.LEARNING

            title.contains("쇼핑", ignoreCase = true) ||
                    title.contains("청소", ignoreCase = true) ||
                    title.contains("개인", ignoreCase = true) -> TaskCategory.PERSONAL

            else -> TaskCategory.GENERAL
        }
    }

    private suspend fun <T> executeWithRetry(operation: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(RETRY_COUNT) { attempt ->
            try {
                Log.d(TAG, "Attempting operation (${attempt + 1}/$RETRY_COUNT)")
                return operation()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < RETRY_COUNT - 1) {
                    val delayMs = RETRY_DELAY * (attempt + 1)
                    Log.d(TAG, "Retrying in ${delayMs}ms...")
                    delay(delayMs)
                }
            }
        }
        throw lastException ?: AppException.NetworkException("모든 재시도가 실패했습니다")
    }

    private fun validateInput(tasks: List<String>, startTime: String, endTime: String) {
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

    private fun mapException(exception: Exception): AppException {
        return when (exception) {
            is HttpException -> AppException.ApiException(
                code = exception.code(),
                message = exception.message()
            )

            is SocketTimeoutException -> AppException.TimeoutException()
            is IOException -> AppException.NetworkException(
                message = "네트워크 연결 오류",
                cause = exception
            )

            else -> AppException.NetworkException(
                message = "알 수 없는 오류: ${exception.message}",
                cause = exception
            )
        }
    }

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