package com.franklinharper.inactivitymonitor.time

// An alternative would be to use the standard Java `Duration` API.
// For details see: https://developer.android.com/studio/write/java8-support#library-desugaring

@JvmInline
value class Seconds(val value: Long)

fun Int.minutesToSeconds() = Seconds(this * 60L)
fun Int.hoursToSeconds() = Seconds(this * 60L * 60L)

