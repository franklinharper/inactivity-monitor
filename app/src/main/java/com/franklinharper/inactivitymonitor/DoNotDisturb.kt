package com.franklinharper.inactivitymonitor

import android.app.NotificationManager
import javax.inject.Inject

class DoNotDisturb @Inject constructor(
  private val notificationManager: NotificationManager
) {

  val off: Boolean
    get() = currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

  val on: Boolean
    get() = !off

  private val currentInterruptionFilter: Int
    get() = notificationManager.currentInterruptionFilter

}
