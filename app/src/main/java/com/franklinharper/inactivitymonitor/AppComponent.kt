package com.franklinharper.inactivitymonitor

import android.content.Context

fun app() = AppComponent.instance

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
}