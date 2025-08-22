package com.hanto.aischeduler.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface SavedScheduleDao {

    // === 기본 저장 ===
    @Upsert
    suspend fun insertSchedule(schedule: SavedScheduleEntity): Long

    @Upsert
    suspend fun insertTasks(tasks: List<SavedTaskEntity>)

    // === 기본 조회 ===
    @Transaction
    @Query("SELECT * FROM saved_schedules WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    suspend fun getScheduleByDate(date: String): SavedScheduleWithTasks?

    @Transaction
    @Query("SELECT * FROM saved_schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: String): SavedScheduleWithTasks?

    // === 기본 업데이트 ===
    @Update
    suspend fun updateSchedule(schedule: SavedScheduleEntity)

    @Query("UPDATE saved_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean)

    // === 완료 카운트 업데이트 ===
    @Query(
        """
        UPDATE saved_schedules 
        SET completedTasks = (
            SELECT COUNT(*) FROM saved_tasks 
            WHERE scheduleId = :scheduleId AND isCompleted = 1
        ),
        lastModified = :timestamp
        WHERE id = :scheduleId
    """
    )
    suspend fun updateScheduleCompletionCount(
        scheduleId: String,
        timestamp: Long = System.currentTimeMillis()
    )
}