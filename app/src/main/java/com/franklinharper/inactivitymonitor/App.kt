package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import fr.bipi.tressence.file.FileLoggerTree
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

  @Inject lateinit var fileLoggerTree: FileLoggerTree

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
    Timber.plant(fileLoggerTree)
  }
}