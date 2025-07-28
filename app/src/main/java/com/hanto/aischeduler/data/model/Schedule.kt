package com.hanto.aischeduler.data.model

data class Schedule(
    val id: String = "",
    val date: String,
    val tasks: List<Task>,
    val createdAt: Long = System.currentTimeMillis()
)