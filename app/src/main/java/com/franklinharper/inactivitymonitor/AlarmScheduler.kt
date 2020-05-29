package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService
import com.franklinharper.inactivitymonitor.settings.AppSettings
import timber.log.Timber
import kotlin.math.max

class AlarmScheduler(
  context: Context,
  private val eventRepository: EventRepository,
  private val appSettings: AppSettings
) {

  private val alarmManager = context.getSystemService<AlarmManager>()!!

  private val alarmIntent =
    Intent(context, AppBroadcastReceiver::class.java).let {
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
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + alarmSecs * 1000,
      alarmIntent
    )
  }

  private fun timeoutSecs(activity: UserActivity?): Long {
    val minWaitSecs = appSettings.reminderInterval()
    val maxWaitSecs = appSettings.maxStillMinutes() * 60L
    return when (activity?.type) {
      EventType.STILL_START -> {
        // Never wait less than minWaitSecs.
        max(minWaitSecs, maxWaitSecs - activity.durationSecs)
      }
      else -> maxWaitSecs
    }
  }

}
