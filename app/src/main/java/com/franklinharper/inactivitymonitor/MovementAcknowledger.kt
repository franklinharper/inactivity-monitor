package com.franklinharper.inactivitymonitor

import timber.log.Timber
import javax.inject.Inject

class MovementAcknowledger @Inject constructor(
  private val eventRepository: EventRepository,
  private val appVibrations: AppVibrations,
  private val doNotDisturb: DoNotDisturb,
  private val snooze: Snooze,
  private val movementLogic: MovementLogic,
) {

  fun maybeInformUser() {
    if (doNotDisturb.on || snooze.end != null) {
      return
    }

    val mostRecentMovements = eventRepository.mostRecentMovements(count = 2)
    if (mostRecentMovements.size != 2) {
      return
    }

    val currentMovement = mostRecentMovements[0]
    val previousMovement = mostRecentMovements[1]
    Timber.d("currentMovement $currentMovement")
    Timber.d("previousMovement $previousMovement")

    val userIsNotStill = currentMovement.type != MovementType.STILL_START

    val userWasStillForTooLong = movementLogic.userStillForTooLong(previousMovement)
    Timber.d("userWasStillForTooLong: $userWasStillForTooLong")

    // Only inform the user when they move after having been still for too long.
    if (userIsNotStill && userWasStillForTooLong) {
      Timber.d("Inform user that transition from STILL to MOVEMENT was detected")
      appVibrations.acknowledgeMovement()
    }
  }
}

