package com.hanto.aischeduler.data.model

data class ScheduleRequest(
    val tasks: List<String>,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val date: String
)
