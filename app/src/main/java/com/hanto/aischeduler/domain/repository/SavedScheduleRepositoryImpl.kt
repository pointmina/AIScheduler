package com.hanto.aischeduler.domain.repository

import android.util.Log
import com.hanto.aischeduler.data.database.SavedScheduleDao
import com.hanto.aischeduler.data.database.SavedScheduleEntity
import com.hanto.aischeduler.data.database.SavedTaskEntity
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.viewModel.SavedScheduleItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedScheduleRepositoryImpl @Inject constructor(
    private val savedScheduleDao: SavedScheduleDao
) : SavedScheduleRepository {

    companion object {
        private const val TAG = "SavedScheduleRepo"
    }

    override suspend fun saveSchedule(
        tasks: List<Task>,
        title: String,
        date: String,
        startTime: String,
        endTime: String
    ): Result<String> {
        return try {
            val scheduleId = "schedule_${System.currentTimeMillis()}"

            // 1. 스케줄 엔티티 생성
            val scheduleEntity = SavedScheduleEntity(
                id = scheduleId,
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                totalTasks = tasks.size,
                completedTasks = tasks.count { it.isCompleted }
            )

            // 2. 작업 엔티티들 생성
            val taskEntities = tasks.mapIndexed { index, task ->
                SavedTaskEntity(
                    id = "task_${scheduleId}_$index",
                    scheduleId = scheduleId,
                    title = task.title,
                    description = task.description,
                    startTime = task.startTime,
                    endTime = task.endTime,
                    isCompleted = task.isCompleted,
                    sortOrder = index
                )
            }

            // 3. 데이터베이스에 저장
            savedScheduleDao.insertSchedule(scheduleEntity)
            savedScheduleDao.insertTasks(taskEntities)

            Log.d(TAG, "스케줄 저장 성공: $scheduleId")
            Result.success(scheduleId)

        } catch (e: Exception) {
            Log.e(TAG, "스케줄 저장 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun getScheduleByDate(date: String): Result<List<Task>?> {
        return try {
            val savedSchedule = savedScheduleDao.getScheduleByDate(date)

            val tasks = savedSchedule?.tasks?.map { taskEntity ->
                Task(
                    id = taskEntity.id,
                    title = taskEntity.title,
                    description = taskEntity.description,
                    startTime = taskEntity.startTime,
                    endTime = taskEntity.endTime,
                    date = savedSchedule.schedule.date,
                    isCompleted = taskEntity.isCompleted
                )
            }?.sortedBy { it.startTime }

            Result.success(tasks)

        } catch (e: Exception) {
            Log.e(TAG, "날짜별 스케줄 조회 실패: $date", e)
            Result.failure(e)
        }
    }

    override suspend fun getScheduleById(scheduleId: String): Result<List<Task>?> {
        return try {
            val savedSchedule = savedScheduleDao.getScheduleById(scheduleId)

            val tasks = savedSchedule?.tasks?.map { taskEntity ->
                Task(
                    id = taskEntity.id,
                    title = taskEntity.title,
                    description = taskEntity.description,
                    startTime = taskEntity.startTime,
                    endTime = taskEntity.endTime,
                    date = savedSchedule.schedule.date,
                    isCompleted = taskEntity.isCompleted
                )
            }?.sortedBy { it.startTime }

            Result.success(tasks)

        } catch (e: Exception) {
            Log.e(TAG, "스케줄 조회 실패: $scheduleId", e)
            Result.failure(e)
        }
    }

    override suspend fun getSavedSchedulesList(
        startDate: String,
        endDate: String
    ): Result<List<SavedScheduleItem>> {
        return try {
            val savedSchedules = savedScheduleDao.getSchedulesByDateRange(startDate, endDate)

            val scheduleItems = savedSchedules.map { scheduleWithTasks ->
                SavedScheduleItem(
                    id = scheduleWithTasks.schedule.id,
                    title = scheduleWithTasks.schedule.title,
                    date = scheduleWithTasks.schedule.date,
                    startTime = scheduleWithTasks.schedule.startTime,
                    endTime = scheduleWithTasks.schedule.endTime,
                    totalTasks = scheduleWithTasks.schedule.totalTasks,
                    completedTasks = scheduleWithTasks.schedule.completedTasks,
                    createdAt = scheduleWithTasks.schedule.createdAt
                )
            }.sortedByDescending { it.date }

            Result.success(scheduleItems)

        } catch (e: Exception) {
            Log.e(TAG, "스케줄 목록 조회 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskTime(
        taskId: String,
        startTime: String,
        endTime: String
    ): Result<Unit> {
        return try {
            savedScheduleDao.updateTaskTime(taskId, startTime, endTime)

            // 스케줄 수정 시간도 업데이트
            val scheduleId = extractScheduleIdFromTaskId(taskId)
            if (scheduleId.isNotEmpty()) {
                savedScheduleDao.updateScheduleLastModified(scheduleId, System.currentTimeMillis())
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "작업 시간 업데이트 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            savedScheduleDao.updateTaskCompletion(taskId, isCompleted)

            // 스케줄 완료 카운트 업데이트
            val scheduleId = extractScheduleIdFromTaskId(taskId)
            if (scheduleId.isNotEmpty()) {
                savedScheduleDao.updateScheduleCompletionCount(scheduleId)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "작업 완료 상태 업데이트 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun updateMultipleTasks(tasks: List<Task>): Result<Unit> {
        return try {
            tasks.forEach { task ->
                if (task.id.startsWith("task_schedule_")) {
                    savedScheduleDao.updateTaskTime(task.id, task.startTime, task.endTime)
                }
            }

            // 첫 번째 작업의 스케줄 ID로 수정 시간 업데이트
            tasks.firstOrNull()?.let { firstTask ->
                val scheduleId = extractScheduleIdFromTaskId(firstTask.id)
                if (scheduleId.isNotEmpty()) {
                    savedScheduleDao.updateScheduleLastModified(
                        scheduleId,
                        System.currentTimeMillis()
                    )
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "다중 작업 업데이트 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        return try {
            savedScheduleDao.deleteScheduleById(scheduleId)
            Log.d(TAG, "스케줄 삭제 성공: $scheduleId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "스케줄 삭제 실패", e)
            Result.failure(e)
        }
    }

    // 헬퍼 함수
    private fun extractScheduleIdFromTaskId(taskId: String): String {
        return try {
            if (taskId.startsWith("task_schedule_")) {
                val parts = taskId.split("_")
                if (parts.size >= 3) {
                    "schedule_${parts[2]}"
                } else ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}