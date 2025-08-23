package com.hanto.aischeduler.domain.repository

import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.viewModel.SavedScheduleItem

interface SavedScheduleRepository {

    // === 기본 저장/불러오기 ===
    suspend fun saveSchedule(
        tasks: List<Task>,
        title: String,
        date: String,
        startTime: String,
        endTime: String
    ): Result<String>

    suspend fun getScheduleByDate(date: String): Result<List<Task>?>
    suspend fun getScheduleById(scheduleId: String): Result<List<Task>?>

    // === 목록 관리 ===
    suspend fun getSavedSchedulesList(
        startDate: String,
        endDate: String
    ): Result<List<SavedScheduleItem>>

    // === 편집 관련 ===
    suspend fun updateTaskTime(taskId: String, startTime: String, endTime: String): Result<Unit>
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean): Result<Unit>
    suspend fun updateMultipleTasks(tasks: List<Task>): Result<Unit>

    // === 삭제 ===
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>
}