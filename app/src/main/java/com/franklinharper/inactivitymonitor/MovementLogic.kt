package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.settings.AppSettings
import javax.inject.Inject

class MovementLogic @Inject constructor(
  private val appSettings: AppSettings,
) {

  fun userStillForTooLong(movement: UserMovement): Boolean {
    val userIsStill = movement.type == MovementType.STILL_START
    return userIsStill && movement.durationSecs > appSettings.maxStillMinutes() * 60
  }
}