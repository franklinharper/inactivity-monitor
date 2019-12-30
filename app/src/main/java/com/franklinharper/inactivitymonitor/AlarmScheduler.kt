package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService
import timber.log.Timber
import kotlin.math.max

class AlarmScheduler(
  private val eventRepository: EventRepository,
  context: Context
) {

  private val minWaitSecs = 30L

  private val alarmManager = context.getSystemService<AlarmManager>()!!

  private val alarmIntent =
    Intent(context, ActivityTransitionReceiver::class.java).let {
      PendingIntent.getBroadcast(context, 0, it, 0)
    }

  fun update() {
    // Wait 1 extra second to ensure that the timeout has expired when the app is woken up.
    val alarmSecs = timeoutSecs(eventRepository.mostRecentActivity()) + 1
    replacePreviousAlarm(alarmSecs)
  }

  private fun replacePreviousAlarm(alarmSecs: Long) {
    Timber.d("replacePreviousAlarm, alarmSecs $alarmSecs")
    alarmManager.cancel(alarmIntent)
    alarmManager.set(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + alarmSecs * 1000,
      alarmIntent
    )
  }

  private fun timeoutSecs(activity: UserActivity?): Long {
    // TODO read 30 * 60 from SharedPrefs instead of hard coding it here
    val maxWaitSecs = 30L * 60L
    return when (activity?.type) {
      EventType.STILL_START -> {
        // Never wait less than minWaitSecs.
        max(minWaitSecs, maxWaitSecs - activity.durationSecs)
      }
      else -> maxWaitSecs
    }
  }

}
