package com.franklinharper.inactivitymonitor

fun Event.timeUntil(other: Event): Long {
  return other.time.unixTime - time.unixTime
}
