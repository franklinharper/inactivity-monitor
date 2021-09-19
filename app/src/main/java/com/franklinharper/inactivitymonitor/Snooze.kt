package com.franklinharper.inactivitymonitor

import androidx.annotation.StringRes
import com.franklinharper.inactivitymonitor.settings.AppSettings
import com.franklinharper.inactivitymonitor.time.Seconds
import com.franklinharper.inactivitymonitor.time.hoursToSeconds
import com.franklinharper.inactivitymonitor.time.minutesToSeconds
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

enum class SnoozeDuration(val second: Seconds, @StringRes val stringId: Int) {
  FIFTEEN_MINUTES(15.minutesToSeconds(), R.string.snooze_15_minutes),
  THIRTY_MINUTES(30.minutesToSeconds(), R.string.snooze_30_minutes),
  FORTY_FIVE_MINUTES(45.minutesToSeconds(), R.string.snooze_45_minutes),
  ONE_HOUR(1.hoursToSeconds(), R.string.snooze_1_hour),
  TWO_HOURS(2.hoursToSeconds(), R.string.snooze_2_hours),
  FOUR_HOURS(4.hoursToSeconds(), R.string.snooze_4_hours),
  SIX_HOURS(6.hoursToSeconds(), R.string.snooze_6_hours),
  EIGHT_HOURS(8.hoursToSeconds(), R.string.snooze_8_hours),
  TEN_HOURS(10.hoursToSeconds(), R.string.snooze_10_hours),
  TWELVE_HOURS(12.hoursToSeconds(), R.string.snooze_12_hours),
}

class Snooze @Inject constructor(
  private val appSettings: AppSettings
) {

  private val UNDEFINED_END = -1L

  fun start(snoozeDuration: SnoozeDuration) {
    val end = Instant.now().plusSeconds(snoozeDuration.second.value)
    appSettings.snoozeEndSecond = end.epochSecond
    Timber.d("Starting snooze which ends at ${TimeFormatters.dateTime.format(end)}")
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
