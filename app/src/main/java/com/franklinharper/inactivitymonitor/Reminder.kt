package com.franklinharper.inactivitymonitor

class Reminder(
  private val eventRepository: EventRepository,
  private val vibratorWrapper: VibratorWrapper = appComponent().vibratorWrapper,
  private val notificationSender: NotificationSender = appComponent().notificationSender
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
    notificationSender.sendMoveNotification(activity.type, activity.durationSecs / 60.0)
    if (notificationSender.doNotDisturbOff) {
      vibratorWrapper.vibrate(moveReminderVibrationMillis)
    }
  }
}

