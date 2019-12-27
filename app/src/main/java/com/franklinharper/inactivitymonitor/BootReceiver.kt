package com.franklinharper.inactivitymonitor;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

  // This class is instantiated by the Android OS.
  // Because of this the constructor of this class can't be used to supply dependencies.
  // So instead we inject the dependency manually.
  private val alarmScheduler = appComponent().alarmScheduler

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("Boot broadcast received")
    alarmScheduler.update()
  }
}

