package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import timber.log.Timber

class AlarmScheduler(application: Context) {

  private val alarmManager: AlarmManager =
    application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private val alarmIntent =
    Intent(application, ActivityTransitionReceiver::class.java).let { intent ->
      PendingIntent.getBroadcast(application, 0, intent, 0)
    }

  fun replacePreviousAlarm(waitSecs: Long) {
    Timber.d("replacePreviousAlarm waitSecs:$waitSecs")
    alarmManager.cancel(alarmIntent)
    alarmManager.set(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + waitSecs * 1000,
      alarmIntent
    )
  }

  fun removePreviousAlarm() {
    Timber.d("removePreviousAlarm")
    alarmManager.cancel(alarmIntent)
  }

}
