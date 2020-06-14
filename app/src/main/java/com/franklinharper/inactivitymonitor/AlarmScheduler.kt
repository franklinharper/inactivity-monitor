package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService
import com.franklinharper.inactivitymonitor.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

class AlarmScheduler @Inject constructor(
  @ApplicationContext private val context: Context,
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
    val alarmSecs = timeoutSecs(eventRepository.mostRecentMovement()) + 1
    replacePreviousAlarm(alarmSecs)
  }

  private fun replacePreviousAlarm(alarmSecs: Long) {
    Timber.d("replacePreviousAlarm, alarmSecs $alarmSecs")
    alarmManager.cancel(alarmIntent)
    alarmManager.setExactAndAllowWhileIdle(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + alarmSecs * 1000,
      alarmIntent
    )
  }

  private fun timeoutSecs(movement: UserMovement?): Long {
    val minWaitSecs = appSettings.reminderInterval()
    val maxWaitSecs = appSettings.maxStillMinutes() * 60L
    return when (movement?.type) {
      MovementType.STILL_START -> {
        // Never wait less than minWaitSecs.
        max(minWaitSecs, maxWaitSecs - movement.durationSecs)
      }
      else -> maxWaitSecs
    }
  }

}
