package com.franklinharper.inactivitymonitor

private fun now() = System.currentTimeMillis() / 1000

fun UserActivity.secsSinceStart(end: Long = now()): Long {
    return end - start
}

