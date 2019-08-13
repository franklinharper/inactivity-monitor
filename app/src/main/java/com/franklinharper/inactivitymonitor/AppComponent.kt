package com.franklinharper.inactivitymonitor

import android.content.Context

fun app() = AppComponent.instance

interface AppComponent {
  val transitionProcessor: TransitionProcessor
  val myNotificationManager: MyNotificationManager
  val myVibrationManager: MyVibrationManager
  val myAlarmManager: MyAlarmManager
  val localDb: LocalDb
  val cloudDb: MyFirebaseDb
  val activityRepository: ActivityRepository

  companion object {
    lateinit var instance: AppComponent
  }
}

class AppModule(application: Context) : AppComponent {
  override val myAlarmManager = MyAlarmManager(application)
  override val myNotificationManager = MyNotificationManager(application)
  override val myVibrationManager = MyVibrationManager(application)
  override val localDb = LocalDb(application)
  override val cloudDb = MyFirebaseDb()
  override val activityRepository = ActivityRepository(localDb, cloudDb)
  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val transitionProcessor = TransitionProcessor(
    activityRepository = activityRepository,
    myAlarmManager = myAlarmManager,
    myVibrationManager = myVibrationManager,
    myNotificationManager = myNotificationManager
  )
}