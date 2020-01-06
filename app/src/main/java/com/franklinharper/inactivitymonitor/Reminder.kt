package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import java.time.LocalTime

class Reminder(
  private val eventRepository: EventRepository,
  private val vibratorCompat: VibratorCompat = appComponent().vibratorCompat,
  private val notificationSender: NotificationSender = appComponent().notificationSender,
  private val snooze: Snooze,
  private val appSettings: AppSettings = appComponent().appSettings
) {

  private val moveReminderVibrationMillis = 2500L

  fun update() {
    val mostRecentActivity = eventRepository.mostRecentActivity()
    if (shouldRemind(mostRecentActivity)) {
      remindToMove(mostRecentActivity)
    } else {
      notificationSender.cancelNotification()
    }
  }

  fun shouldRemind(latestActivity: UserActivity): Boolean {
    val hour = LocalTime.now().hour
    val tooEarly = hour <  appSettings.reminderStart()
    val tooLate = hour >= appSettings.reminderEnd()
    val snoozed = snooze.isActive()
    val still = latestActivity.type == EventType.STILL_START
    val tooLong = latestActivity.durationSecs > appSettings.maxStillMinutes() * 60
    return still && tooLong && !tooEarly && !tooLate && !snoozed
  }

  private fun remindToMove(activity: UserActivity) {
    if (appSettings.notify()) {
      notificationSender.sendMoveNotification(activity.type, activity.durationSecs / 60.0)
    } else {
      Timber.d("notifications off")
    }
    if (appSettings.vibrate()) {
      vibratorCompat.vibrate(milliseconds = moveReminderVibrationMillis)
    } else {
      Timber.d("vibrations off")
    }
  }
}

