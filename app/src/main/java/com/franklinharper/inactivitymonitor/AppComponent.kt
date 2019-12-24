package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import fr.bipi.tressence.file.FileLoggerTree
import java.io.File

fun appComponent() = AppComponent.instance

interface AppComponent {
  val transitionProcessor: TransitionProcessor
  val myNotificationManager: MyNotificationManager
  val myVibrator: MyVibrator
  val alarmScheduler: AlarmScheduler
  val localDb: LocalDb
  val remoteDb: RemoteDb
  val eventRepository: EventRepository

  companion object {
    lateinit var instance: AppComponent
  }

  val fileLogger: FileLoggerTree
}

class AppModule(application: Context) : AppComponent {
  override val alarmScheduler = AlarmScheduler(application)
  override val myNotificationManager = MyNotificationManager(application)
  override val myVibrator = MyVibrator(application)
  override val localDb = LocalDb(application)
  override val remoteDb = RemoteDb()
  override val eventRepository = EventRepository(localDb, remoteDb)
  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val transitionProcessor = TransitionProcessor(
    eventRepository = eventRepository,
    alarmScheduler = alarmScheduler,
    myVibrator = myVibrator,
    myNotificationManager = myNotificationManager
  )
  @SuppressLint("LogNotTimber")
  private val logDir = File(application.filesDir, "logs").also { dir ->
    if (!dir.exists()) {
      Log.i("LOGGER", "created logDir")
      dir.mkdir()
    }
  }

  override val fileLogger = FileLoggerTree.Builder()
    .withFileName("file%g.log")
    .withDir(logDir)
    .withSizeLimit(1_000_000)
    .withFileLimit(3)
    .withMinPriority(Log.DEBUG)
    .appendToFile(true)
    .build()
}