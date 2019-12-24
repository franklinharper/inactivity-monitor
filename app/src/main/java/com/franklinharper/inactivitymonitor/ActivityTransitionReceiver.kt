package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

class ActivityTransitionReceiver : BroadcastReceiver() {

  // In this case the constructor of this class can't be used to supply the dependency
  // because this class is instantiated by the Android OS.
  // So we fall back to initializing the field.
  private val transitionProcessor = appComponent().transitionProcessor

  override fun onReceive(context: Context, intent: Intent?) {
    log(intent)
    val transitionResult = ActivityTransitionResult.extractResult(intent)
    transitionProcessor.processTransitionResult(transitionResult)
  }

  private fun log(intent: Intent?) {
    Timber.v("intent = $intent")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.v("extra $key ${bundle[key]}")
    }
  }
}

