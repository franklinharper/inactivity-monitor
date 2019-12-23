package com.franklinharper.inactivitymonitor;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

  // In this case the constructor of this class can't be used to supply the dependency
// because this class is instantiated by the Android OS.
// So we fall back to initializing the field.
  private val myAlarmManager = app().alarmScheduler

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("Boot broadcast received")
    myAlarmManager.replacePreviousAlarm(TransitionProcessor.DEFAULT_MAX_WAIT_SECS)
  }
}

