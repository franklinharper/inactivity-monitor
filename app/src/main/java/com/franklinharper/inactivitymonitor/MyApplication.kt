package com.franklinharper.inactivitymonitor

import android.app.Application
import timber.log.Timber

class MyApplication : Application() {

  // TODO: Each component should have its own initialization instead of sub-classing Application.
  override fun onCreate() {
    super.onCreate()
    AppComponent.instance = AppModule(this)
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    } else {
      // TODO configure production logging
      // Timber.plant(CrashReportingTree ())
    }
  }
}