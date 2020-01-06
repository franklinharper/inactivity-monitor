package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.franklinharper.inactivitymonitor.settings.AppSettings
import com.franklinharper.inactivitymonitor.settings.SystemSettings
import fr.bipi.tressence.file.FileLoggerTree
import java.io.File

fun appComponent() = AppComponent.instance

interface AppComponent {
  // The activityRecognitionSubscriber is not called anywhere in,the app.
  // But it must be instantiated so that the app will receive
  // updates when a new user activity is recognized.
  val activityRecognitionSubscriber: ActivityRecognitionSubscriber
  val systemSettings: SystemSettings
  val fileLogger: FileLoggerTree
  val reminder: Reminder
  val notificationSender: NotificationSender
  val vibratorCompat: VibratorCompat
  val alarmScheduler: AlarmScheduler
  val localDb: LocalDb
  val remoteDb: RemoteDb
  val eventRepository: DbEventRepository
  val appSettings: AppSettings
  val snooze: Snooze

  companion object {
    lateinit var instance: AppComponent
  }

}

class AppModule(application: Context) : AppComponent {
  override val activityRecognitionSubscriber = ActivityRecognitionSubscriber(application)
  override val systemSettings = SystemSettings()
  override val appSettings = AppSettings(
    application,
    PreferenceManager.getDefaultSharedPreferences(application)
  )
  override val snooze = Snooze(appSettings)
  override val notificationSender = NotificationSender(application)
  override val vibratorCompat = VibratorCompat(application)
  override val localDb = LocalDb(application)
  override val remoteDb = RemoteDb()
  override val eventRepository = DbEventRepository(localDb, remoteDb)
  override val alarmScheduler = AlarmScheduler(eventRepository, application)

  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val reminder = Reminder(
    eventRepository = eventRepository,
    vibratorCompat = vibratorCompat,
    notificationSender = notificationSender,
    appSettings = appSettings,
    snooze = snooze
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