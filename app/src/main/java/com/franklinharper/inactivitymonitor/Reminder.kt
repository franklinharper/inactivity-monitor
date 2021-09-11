package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject

class Reminder @Inject constructor(
  private val eventRepository: EventRepository,
  private val appVibrator: AppVibrator,
  private val notificationSender: NotificationSender,
  private val snooze: Snooze,
  private val appSettings: AppSettings,
  private val phoneCall: PhoneCall,
) {

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
    val userIsStill = latestMovement.type == MovementType.STILL_START
    val userStillForTooLong = userIsStill
        && latestMovement.durationSecs > appSettings.maxStillMinutes() * 60
    // FIXME phoneCallIsActive is NOT true when a phone call is active :(
    val phoneCallIsActive = phoneCall.isActive()
    Timber.d("phoneCallIsActive $phoneCallIsActive")
    val snoozeEnd = snooze.end
    Timber.d(
      "reminderStart $reminderStart, reminderEnd $reminderEnd, snoozeEnd $snoozeEnd," +
          "still $userIsStill, tooLong $userStillForTooLong"
    )
    return userStillForTooLong
        && !tooEarly
        && !tooLate
        && snoozeEnd == null
        && !phoneCallIsActive
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

