package com.franklinharper.inactivitymonitor

import timber.log.Timber
import javax.inject.Inject

class MovementAcknowledger @Inject constructor(
  private val eventRepository: EventRepository,
  private val appVibrator: AppVibrator
) {

  fun update() {
    val mostRecentActivities = eventRepository.mostRecentMovements(2)
    Timber.d("mostRecentActivities $mostRecentActivities")
    if (mostRecentActivities.size != 2) {
      return
    }
    val currentActivity = mostRecentActivities[0]
    val previousActivity = mostRecentActivities[1]
    val isNotStill = currentActivity.type != MovementType.STILL_START
    if (isNotStill && previousActivity.type == MovementType.STILL_START) {
      Timber.d("Inform user that transition from STILL to MOVEMENT was detected")
      appVibrator.acknowledgeMovement()
    }
  }
}

