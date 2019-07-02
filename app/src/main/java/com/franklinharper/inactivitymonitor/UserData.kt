package com.franklinharper.inactivitymonitor

data class UserActivity(
    val type: String,
    val duration: Long = 0 // In seconds
)
