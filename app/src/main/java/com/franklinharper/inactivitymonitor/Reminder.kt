package com.franklinharper.inactivitymonitor

import timber.log.Timber

class Reminder(
  private val eventRepository: EventRepository,
  private val vibratorWrapper: VibratorWrapper = appComponent().vibratorWrapper,
  private val notificationSender: NotificationSender = appComponent().notificationSender,
  private val settings: Settings = appComponent().settings
) {

  private val maxStillSecs = 30 * 60L // 30 minutes
  private val moveReminderVibrationMillis = 2500L

  fun update() {
    val mostRecentActivity = eventRepository.mostRecentActivity()
    if (stillForTooLong(mostRecentActivity)) {
      remindToMove(mostRecentActivity)
    } else {
      notificationSender.cancelNotification()
    }
  }

  private fun stillForTooLong(latestActivity: UserActivity): Boolean {
    return latestActivity.type == EventType.STILL_START &&
        latestActivity.durationSecs > maxStillSecs
  }

  private fun remindToMove(activity: UserActivity) {
    if (settings.notify()) {
      notificationSender.sendMoveNotification(activity.type, activity.durationSecs / 60.0)
    } else {
      Timber.d("notification off")
    }
    if (settings.vibrate()) {
      vibratorWrapper.vibrate(moveReminderVibrationMillis)
    } else {
      Timber.d("vibration off")
    }
  }
}

