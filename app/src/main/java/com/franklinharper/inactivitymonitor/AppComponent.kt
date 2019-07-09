package com.franklinharper.inactivitymonitor

import android.content.Context

fun app() = AppComponent.instance

interface AppComponent {
  val transitionProcessor: TransitionProcessor
  val myNotificationManager: MyNotificationManager
  val vibrationManager: VibrationManager
  val activityRepository: ActivityRepository
  val activityDb: ActivityDb
  val myAlarmManager: MyAlarmManager

  companion object {
    lateinit var instance: AppComponent
  }
}

class AppModule(application: Context) : AppComponent {
  override val myAlarmManager = MyAlarmManager(application)
  override val myNotificationManager = MyNotificationManager(application)
  override val vibrationManager = VibrationManager(application)
  override val activityDb = ActivityDb(application)
  override val activityRepository = ActivityRepository(activityDb)
  // We can't use default arguments to provide the dependencies,
  // because the default arguments use "app().instance" which would cause an infinite recursion loop.
  override val transitionProcessor = TransitionProcessor(
    activityRepository = activityRepository,
    myAlarmManager = myAlarmManager,
    vibrationManager = vibrationManager,
    myNotificationManager = myNotificationManager
  )
}