package com.franklinharper.inactivitymonitor

import androidx.annotation.StringRes
import timber.log.Timber
import java.time.Instant

enum class SnoozeDuration(val second: Long, @StringRes val stringId: Int) {
  FIFTEEN_MINUTES(900, R.string.snooze_15_minutes),
  THIRTY_MINUTES(1800, R.string.snooze_30_minutes),
  FORTY_FIVE_MINUTES(2700, R.string.snooze_45_minutes),
  ONE_HOUR(3600, R.string.snooze_1_hour),
  TWO_HOURS(7200, R.string.snooze_2_hours)
}

class Snooze(
  private val settings: Settings = appComponent().settings
) {
  fun start(snoozeDuration: SnoozeDuration) {
    settings.snoozeEndSecond = Instant.now().plusSeconds(snoozeDuration.second).epochSecond
    Timber.d("Starting snooze, snooze will end at ${TimeFormatters.dateTime.format(end())}")
  }

  fun cancel() {
    Timber.d("Snooze cancelled")
    settings.snoozeEndSecond = -1
  }

  fun isActive(): Boolean {
    return settings.snoozeEndSecond > Instant.now().epochSecond
  }

  fun end(): Instant? {
    val end = settings.snoozeEndSecond
    return if (end == -1L) null else Instant.ofEpochSecond(end)
  }
}
