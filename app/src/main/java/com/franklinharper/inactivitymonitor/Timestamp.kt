package com.franklinharper.inactivitymonitor

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class Timestamp(val unixTime: Long) {

  fun toZonedDateTime(): ZonedDateTime {
    val instant = Instant.ofEpochSecond(unixTime)
    return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
  }

  companion object {
    fun from(time: ZonedDateTime): Timestamp {
      return  Timestamp(time.toEpochSecond())
    }
  }

}

