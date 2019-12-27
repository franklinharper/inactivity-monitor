package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import fr.bipi.tressence.file.FileLoggerTree
import java.io.File

fun appComponent() = AppComponent.instance

interface AppComponent {
  val reminder: Reminder
  val notificationSender: NotificationSender
  val vibratorWrapper: VibratorWrapper
  val alarmScheduler: AlarmScheduler
  val localDb: LocalDb
  val remoteDb: RemoteDb
  val eventRepository: DbEventRepository

  companion object {
    lateinit var instance: AppComponent
  }

  val fileLogger: FileLoggerTree
}

class AppModule(application: Context) : AppComponent {
  override val localDb = LocalDb(application)
  override val remoteDb = RemoteDb()
  override val eventRepository = DbEventRepository(localDb, remoteDb)
  override val alarmScheduler = AlarmScheduler(eventRepository, application)
  override val notificationSender = NotificationSender(application)
  override val vibratorWrapper = VibratorWrapper(application)
  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val reminder = Reminder(
    eventRepository = eventRepository,
    vibratorWrapper = vibratorWrapper,
    notificationSender = notificationSender
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
    .withSizeLimit(50_000)
    .withFileLimit(3)
    .withMinPriority(Log.DEBUG)
    .appendToFile(true)
    .build()
}