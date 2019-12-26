package com.franklinharper.inactivitymonitor

fun Event.timeUntil(other: Event): Long {
  return other.occurred.epochSecond - occurred.epochSecond
}
