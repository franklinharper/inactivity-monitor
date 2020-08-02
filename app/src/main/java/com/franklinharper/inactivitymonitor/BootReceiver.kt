package com.franklinharper.inactivitymonitor;

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : DaggerBroadcastReceiver() {

  @Inject
  lateinit var alarmScheduler: AlarmScheduler

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    Timber.d("Boot broadcast received")
    if (intent.action == Intent.ACTION_BOOT_COMPLETED)
      alarmScheduler.update()
  }
}

