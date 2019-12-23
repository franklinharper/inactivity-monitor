package com.franklinharper.inactivitymonitor

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class Timestamp(val epochSecond: Long) {

  fun toZonedDateTime(): ZonedDateTime {
    val instant = Instant.ofEpochSecond(epochSecond)
    return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
  }

  companion object {
    fun from(time: ZonedDateTime): Timestamp {
      return  Timestamp(time.toEpochSecond())
    }
  }

}

