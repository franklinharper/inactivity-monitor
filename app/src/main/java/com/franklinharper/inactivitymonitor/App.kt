package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.app.Application
import timber.log.Timber

// Suprressing erroneous Lint warning about this class being unused.
@Suppress("unused")
class App : Application() {

  init {
    val originalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Timber.e(throwable, "thread $thread")
      originalExceptionHandler?.uncaughtException(thread, throwable)
    }
  }

  // TODO: Each component should perform its own initialization instead of sub-classing Application.
  override fun onCreate() {
    super.onCreate()
    AppComponent.instance = AppModule(this)
    if (BuildConfig.DEBUG) {
      logToLogcat()
    } else {
      // TODO configure production logging
      // Timber.plant(CrashReportingTree ())
    }
    logToLocalFile()
    Timber.d("========================")
    Timber.d("App Launch, Version ${BuildConfig.VERSION_NAME}")
    // Uncomment line below to test logging of UncaughtExceptions
    // throw IllegalStateException()
  }

  private fun logToLogcat() {
    Timber.plant(Timber.DebugTree())
  }

  @SuppressLint("LogNotTimber")
  private fun logToLocalFile() {
    Timber.plant(AppComponent.instance.fileLogger)
  }
}