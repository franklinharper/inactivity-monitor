package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject

class Reminder @Inject constructor(
  private val eventRepository: EventRepository,
  private val appVibrations: AppVibrations,
  private val notificationSender: NotificationSender,
  private val snooze: Snooze,
  private val appSettings: AppSettings,
  private val phoneCall: PhoneCall,
  private val movementLogic: MovementLogic,
) {

  fun update() {
    val mostRecentMovement = eventRepository.mostRecentMovement()
    val reminderStart = appSettings.reminderStart()
    val todaysFirstMovementAfterStart = eventRepository.firstMovementAfter(
      start = ZonedDateTime.now().startOfDay.plusHours(reminderStart)
    )
    val shouldRemind = shouldRemind(
      now = ZonedDateTime.now(),
      latestMovement = mostRecentMovement,
      firstMovementAfterStart = todaysFirstMovementAfterStart
    )
    if (shouldRemind) {
      remindToMove(movement = mostRecentMovement)
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
    val userStillForTooLong = movementLogic.userStillForTooLong(latestMovement)

    // FIXME phoneCallIsActive is NOT true when a phone call is active :(
    val phoneCallIsActive = phoneCall.isActive()
    Timber.d("phoneCallIsActive $phoneCallIsActive")
    val snoozeEnd = snooze.end
    val message = "reminderStart $reminderStart, reminderEnd $reminderEnd, snoozeEnd $snoozeEnd," +
          "userStillForTooLong: $userStillForTooLong"
    Timber.d(message)

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
      appVibrations.moveReminder()
    } else {
      Timber.d("vibrations off")
    }
  }
}

