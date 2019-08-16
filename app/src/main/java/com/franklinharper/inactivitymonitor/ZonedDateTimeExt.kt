package com.franklinharper.inactivitymonitor

import java.time.ZonedDateTime

// For an in depth discussion on calculating the start of a day in local time see:
//   https://stackoverflow.com/questions/29143910/java-8-date-time-get-start-of-day-from-zoneddatetime
//
// When the local time changes (e.g. when changing from Summer time to Winter time) there can be 2 midnights!
// In this case we choose to use the earlier start of the day, and the difference between 2 successive
// "start of days", will be 25 hours!
val ZonedDateTime.startOfDay: ZonedDateTime
  get() = toLocalDate().atStartOfDay(zone)

val ZonedDateTime.timestamp: Timestamp
  get() = Timestamp(toEpochSecond())
