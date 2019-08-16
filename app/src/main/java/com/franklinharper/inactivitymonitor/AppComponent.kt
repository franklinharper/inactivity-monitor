package com.franklinharper.inactivitymonitor

import android.content.Context

fun app() = AppComponent.instance

interface AppComponent {
  val transitionProcessor: TransitionProcessor
  val myNotificationManager: MyNotificationManager
  val myVibrationManager: MyVibrationManager
  val myAlarmManager: MyAlarmManager
  val localDb: LocalDb
  val remoteDb: RemoteDb
  val eventRepository: EventRepository

  companion object {
    lateinit var instance: AppComponent
  }
}

class AppModule(application: Context) : AppComponent {
  override val myAlarmManager = MyAlarmManager(application)
  override val myNotificationManager = MyNotificationManager(application)
  override val myVibrationManager = MyVibrationManager(application)
  override val localDb = LocalDb(application)
  override val remoteDb = RemoteDb()
  override val eventRepository = EventRepository(localDb, remoteDb)
  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val transitionProcessor = TransitionProcessor(
    eventRepository = eventRepository,
    myAlarmManager = myAlarmManager,
    myVibrationManager = myVibrationManager,
    myNotificationManager = myNotificationManager
  )
}