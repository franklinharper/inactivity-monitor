package com.franklinharper.inactivitymonitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class MyAlarmManager(application: Context) {

    private val systemAlarmManager: AlarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alarmIntent = Intent(application, ActivityTransitionReceiver::class.java).let { intent ->
        PendingIntent.getBroadcast(application, 0, intent, 0)
    }

    fun createNextAlarm(whenToTriggerSecs: Int) {
        removeAlarm()
        systemAlarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + whenToTriggerSecs * 1000,
            alarmIntent
        )
    }

    private fun removeAlarm() {
        systemAlarmManager.cancel(alarmIntent)
    }

}
