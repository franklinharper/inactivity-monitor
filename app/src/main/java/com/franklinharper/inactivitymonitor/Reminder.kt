package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import java.time.ZonedDateTime

class Reminder(
  private val eventRepository: EventRepository,
  private val appVibrator: AppVibrator = appComponent().appVibrator,
  private val notificationSender: NotificationSender = appComponent().notificationSender,
  private val snooze: Snooze,
  private val appSettings: AppSettings = appComponent().appSettings
) {

  private val moveReminderVibrationMillis = 2500L

  fun update() {
    val mostRecentActivity = eventRepository.mostRecentMovement()
    val reminderStart = appSettings.reminderStart()
    val todaysFirstMovementAfterStart = eventRepository.firstMovementAfter(
      ZonedDateTime.now().startOfDay.plusHours(reminderStart)
    )
    val shouldRemind = shouldRemind(
      ZonedDateTime.now(),
      mostRecentActivity,
      todaysFirstMovementAfterStart
    )
    if (shouldRemind) {
      remindToMove(mostRecentActivity)
    } else {
      notificationSender.cancelNotification()
    }
  }

  fun shouldRemind(
    now: ZonedDateTime,
    latestMovement: UserMovement,
    firstMovementAfterStart: MovementType?
  ): Boolean {
    if (appSettings.smartStart() && firstMovementAfterStart == null) {
      Timber.d("Smart Start is ON, and user hasn't moved yet")
      return false
    }
    val hour = now.hour
    val reminderStart = appSettings.reminderStart()
    val reminderEnd = appSettings.reminderEnd()
    val tooEarly = hour < reminderStart
    val tooLate = hour >= reminderEnd
    val snoozed = snooze.isActive()
    val still = latestMovement.type == MovementType.STILL_START
    val tooLong = latestMovement.durationSecs > appSettings.maxStillMinutes() * 60
    Timber.d("reminderStart $reminderStart, reminderEnd $reminderEnd, snoozed $snoozed, still $still, tooLong $tooLong")
    return still && tooLong && !tooEarly && !tooLate && !snoozed
  }

  private fun remindToMove(movement: UserMovement) {
    if (appSettings.notify()) {
      notificationSender.sendMoveNotification(movement.type, movement.durationSecs / 60.0)
    } else {
      Timber.d("notifications off")
    }
    if (appSettings.vibrate()) {
      appVibrator.moveReminder()
    } else {
      Timber.d("vibrations off")
    }
  }
}

