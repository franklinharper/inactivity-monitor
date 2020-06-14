package com.franklinharper.inactivitymonitor

import androidx.annotation.StringRes
import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

enum class SnoozeDuration(val second: Long, @StringRes val stringId: Int) {
  FIFTEEN_MINUTES(900, R.string.snooze_15_minutes),
  THIRTY_MINUTES(1800, R.string.snooze_30_minutes),
  FORTY_FIVE_MINUTES(2700, R.string.snooze_45_minutes),
  ONE_HOUR(3600, R.string.snooze_1_hour),
  TWO_HOURS(7200, R.string.snooze_2_hours)
}

class Snooze @Inject constructor(
  private val appSettings: AppSettings
) {
  fun start(seconds: Long) {
    appSettings.snoozeEndSecond = Instant.now().plusSeconds(seconds).epochSecond
    Timber.d("Starting snooze, ends at ${TimeFormatters.dateTime.format(end())}")
  }

  fun start(snoozeDuration: SnoozeDuration) {
    start(snoozeDuration.second)
  }

  fun cancel() {
    Timber.d("Snooze cancelled")
    appSettings.snoozeEndSecond = -1
  }

  fun isActive(): Boolean {
    return appSettings.snoozeEndSecond > Instant.now().epochSecond
  }

  fun end(): Instant? {
    val end = appSettings.snoozeEndSecond
    return if (end == -1L) null else Instant.ofEpochSecond(end)
  }
}
