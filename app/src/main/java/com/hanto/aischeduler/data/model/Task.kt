package com.hanto.aischeduler.data.model

data class Task(
    val id: String = "",
    val title: String,
    val description: String = "",
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val date: String,      // "2024-01-15"
    val isCompleted: Boolean = false
)
