package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActivityTransitionReceiver : BroadcastReceiver() {

  // In this case the constructor of this class can't be used to supply the dependency
  // because this class is instantiated by the Android OS.
  // So we fall back to initializing the field.
  private val transitionProcessor = app().transitionProcessor

  override fun onReceive(context: Context, intent: Intent?) {
    transitionProcessor.receiveTransition(intent)
  }
}

