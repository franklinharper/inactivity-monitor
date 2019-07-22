package com.franklinharper.inactivitymonitor

private fun now() = System.currentTimeMillis() / 1000

fun Transition.duration(end: Long = now()): Long {
    return end - time
}

