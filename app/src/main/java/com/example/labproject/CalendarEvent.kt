package com.example.labproject

data class CalendarEvent(
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val location: String
)
