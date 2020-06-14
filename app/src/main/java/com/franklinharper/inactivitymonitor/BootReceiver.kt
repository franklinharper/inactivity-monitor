package com.franklinharper.inactivitymonitor;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

  @Inject lateinit var alarmScheduler: AlarmScheduler

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("Boot broadcast received")
    alarmScheduler.update()
  }
}

