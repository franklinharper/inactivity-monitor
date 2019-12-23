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

    if (BuildConfig.DEBUG) {
      addDebugLogger()
    } else {
      // TODO configure production logging
      // Timber.plant(CrashReportingTree ())
    }
    addFileLogger()
  }

  private fun addDebugLogger() {
    Timber.plant(Timber.DebugTree())
  }

  @SuppressLint("LogNotTimber")
  private fun addFileLogger() {
    val logDir = File(filesDir, "logs")
    Log.i("LOGGER", "logDir: $logDir")
    if (!logDir.exists()) {
      Log.i("LOGGER", "created logDir")
      logDir.mkdir()
    }
    val fileLogger = FileLoggerTree.Builder()
      .withFileName("file%g.log")
      .withDir(logDir)
      .withSizeLimit(1_000_000)
      .withFileLimit(3)
      .withMinPriority(Log.DEBUG)
      .appendToFile(true)
      .build()
    Timber.plant(fileLogger)
    Timber.d("========================")
    Timber.d("Logging to files enabled")
  }
}