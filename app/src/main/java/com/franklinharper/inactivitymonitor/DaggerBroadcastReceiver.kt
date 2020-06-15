package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// This class is a workaround for a Hilt bug.
// See https://github.com/google/dagger/issues/1918
abstract class DaggerBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {}
}