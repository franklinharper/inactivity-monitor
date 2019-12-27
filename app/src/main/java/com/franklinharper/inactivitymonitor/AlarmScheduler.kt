package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import timber.log.Timber
import kotlin.math.max

class AlarmScheduler(
  private val eventRepository: EventRepository,
  application: Context
) {

  private val minWaitSecs = 30L

  private val alarmManager: AlarmManager =
    application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val alarmIntent =
    Intent(application, ActivityTransitionReceiver::class.java).let {
      PendingIntent.getBroadcast(application, 0, it, 0)
    }

  fun update() {
    val waitSecs = waitSecs(eventRepository.mostRecentActivity())
    if (waitSecs == null) {
      removePreviousAlarm()
    } else {
      replacePreviousAlarm(waitSecs)
    }
  }

  private fun replacePreviousAlarm(waitSecs: Long) {
    Timber.d("replacePreviousAlarm waitSecs $waitSecs")
    alarmManager.cancel(alarmIntent)
    alarmManager.set(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + waitSecs * 1000,
      alarmIntent
    )
  }

  private fun removePreviousAlarm() {
    Timber.d("removePreviousAlarm")
    alarmManager.cancel(alarmIntent)
  }

  private fun waitSecs(activity: UserActivity?): Long? {
    return when (activity?.type) {
      // Wait 1 extra second to ensure that the timeout has expired when the app is woken up.
      // Never wait less than MIN_WAIT_SECS.
      EventType.STILL_START -> {
        // TODO read 30 * 60 from SharedPrefs instead of hard coding it here
        max(minWaitSecs + 1, 30 * 60 - activity.durationSecs + 1)
      }
      else -> null
    }
  }

}
