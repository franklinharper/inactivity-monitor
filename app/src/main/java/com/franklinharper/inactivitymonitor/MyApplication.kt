package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import fr.bipi.tressence.file.FileLoggerTree
import timber.log.Timber
import java.io.File

// Suprressing Lint warning about this class being unused.
@Suppress("unused")
class MyApplication : Application() {


  // TODO: Each component should perform its own initialization instead of sub-classing Application.
  override fun onCreate() {
    super.onCreate()
    AppComponent.instance = AppModule(this)
    startLoggingToFile()

    if (BuildConfig.DEBUG) {
      addDebugLogger()
    } else {
      // TODO configure production logging
      // Timber.plant(CrashReportingTree ())
    }
  }

  private fun addDebugLogger() {
    Timber.plant(Timber.DebugTree())
  }

  @SuppressLint("LogNotTimber")
  private fun startLoggingToFile() {
    Timber.plant(AppComponent.instance.fileLogger)
    Timber.d("========================")
    Timber.d("App Launch, Version ${BuildConfig.VERSION_NAME}")
  }
}