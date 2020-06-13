package com.franklinharper.inactivitymonitor

import timber.log.Timber

class MovementAcknowledger(
  private val eventRepository: EventRepository,
  private val appVibrator: AppVibrator = appComponent().appVibrator
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

