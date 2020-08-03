package com.franklinharper.inactivitymonitor

import timber.log.Timber
import javax.inject.Inject

class MovementAcknowledger @Inject constructor(
  private val eventRepository: EventRepository,
  private val appVibrator: AppVibrator,
  private val doNotDisturb: DoNotDisturb,
  private val snooze: Snooze
) {

  fun maybeInformUser() {
    if (doNotDisturb.on || snooze.end != null) {
      return
    }

    val mostRecentMovements = eventRepository.mostRecentMovements(2)
    Timber.d("mostRecentMovements $mostRecentMovements")
    if (mostRecentMovements.size != 2) {
      return
    }

    val currentActivity = mostRecentMovements[0]
    val previousActivity = mostRecentMovements[1]
    val curNotStill = currentActivity.type != MovementType.STILL_START
    val prevNotShortStill = with(previousActivity) {
      // We want to prevent short periods of stillness between periods of movement
      // from generating acknowledgements, because the acknowledgements can be annoying.
      // E.g. the vibrations are disruptive while speaking with others on a call.
      // And the other people on the call can hear them too!
      type == MovementType.STILL_START && durationSecs > 60
    }
    if (curNotStill && prevNotShortStill) {
      Timber.d("Inform user that transition from STILL to MOVEMENT was detected")
      appVibrator.acknowledgeMovement()
    }
  }
}

