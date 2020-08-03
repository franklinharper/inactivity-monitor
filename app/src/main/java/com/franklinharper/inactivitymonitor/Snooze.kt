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

  private val UNDEFINED_END = -1L

  fun start(seconds: Long) {
    val end = Instant.now().plusSeconds(seconds)
    appSettings.snoozeEndSecond = end.epochSecond
    Timber.d("Starting snooze which ends at ${TimeFormatters.dateTime.format(end)}")
  }

  fun start(snoozeDuration: SnoozeDuration) {
    start(snoozeDuration.second)
  }


  fun cancel() {
    Timber.d("Snooze cancelled")
    appSettings.snoozeEndSecond = UNDEFINED_END
  }

  val end: Instant?
    get() {
      val end = appSettings.snoozeEndSecond
      return if (end != UNDEFINED_END && end > Instant.now().epochSecond)
        Instant.ofEpochSecond(appSettings.snoozeEndSecond)
      else
        null
    }
}
