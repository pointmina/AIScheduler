package com.hanto.aischeduler.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// 저장된 스케줄 엔티티
@Entity(tableName = "saved_schedules")
data class SavedScheduleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String, // "2024-08-22"
    val startTime: String, // "09:00"
    val endTime: String, // "18:00"
    val totalTasks: Int,
    val completedTasks: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

// 저장된 작업 엔티티
@Entity(
    tableName = "saved_tasks",
    foreignKeys = [
        ForeignKey(
            entity = SavedScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId")]
)
data class SavedTaskEntity(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val title: String,
    val description: String = "",
    val startTime: String, // "09:00"
    val endTime: String, // "10:00"
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

// 스케줄과 작업들을 함께 가져오는 관계 클래스
data class SavedScheduleWithTasks(
    @Embedded val schedule: SavedScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val tasks: List<SavedTaskEntity>
)