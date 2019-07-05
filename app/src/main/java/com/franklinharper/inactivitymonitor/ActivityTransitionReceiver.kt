package com.franklinharper.inactivitymonitor

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber
import java.time.LocalTime

class ActivityTransitionReceiver : BroadcastReceiver() {

  //    private val TIMEOUT_SECS = 30 * 60 // 30 * 60
  private val TIMEOUT_SECS = 30 // 30 * 60
  private val STILL_THRESHOLD = 30 * 60 // 30 minutes
  private lateinit var activityRepository: ActivityRepository
  private lateinit var vibrationManager: VibrationManager
  private lateinit var myAlarmManager: MyAlarmManager
  private lateinit var notificationManager: MyNotificationManager


  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("onReceive(context = $context,\n intent = $intent)\n")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.d("extra $key: ${bundle[key]}")
    }
    initialize(context)
    processIntent(intent)
    manageWakeupAlarms()
    activityRepository.selectLatestActivity().executeAsOneOrNull()?.let {
      val durationInSecs = it.secsSinceStart()
      if (it.type == ActivityType.STILL && durationInSecs > STILL_THRESHOLD) {
        informUser(it)
      }
    }
  }

  private fun initialize(context: Context) {
    val db = ActivityDb.from(context)
    activityRepository = ActivityRepository.from(db)
    vibrationManager = VibrationManager.from(context)
    myAlarmManager = MyAlarmManager.from(context)
    notificationManager = MyNotificationManager.from(context)
  }

  // A new Activity Transition can be of the same type as the previous Activity Transition.
  // E.g. over time the stream could be: ..., STILL, STILL, WALKING, ...
  // When successive Activity Types are equal the data is not written to the DB.
  //
  // Deduping the stream allows calculating time duration of an Activity by subtracting successive start timestamps.
  // If the stream wasn't deduped the same calculation would require looping over multiple rows in the table.
  private fun processIntent(intent: Intent?) {
    if (ActivityTransitionResult.hasResult(intent)) {
      vibrationManager.vibrate(1000)
      val result = ActivityTransitionResult.extractResult(intent)!!
      var previousTransitionType = activityRepository
        .selectLatestActivity()
        .executeAsOneOrNull()
        ?.transition_activity_type
      for (transition in result.transitionEvents) {
        val newType = transition.activityType
        if (newType != previousTransitionType) {
          val activityType = ActivityType.fromInt(newType)
          activityRepository.insert(activityType, transition.activityType)
        }
        previousTransitionType = newType
      }
    }
  }

  private fun manageWakeupAlarms() {
    // TODO wake ourselves up much less often, this brute force approach wastes battery!
    myAlarmManager.createNextAlarm(TIMEOUT_SECS)
//        val latestActivity = transitionRepository.previous().executeAsOneOrNull()
//        if (latestActivity?.activity_type == DetectedActivity.STILL) {
//            myAlarmManager.createNextAlarm(TIMEOUT_SECS)
//        } else {
//            myAlarmManager.removeAlarm()
//        }
  }

  private fun informUser(activity: UserActivity) {
    val localTime = LocalTime.now()
    val allowInterruptions =
      notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
        && localTime.hour > 6
        && localTime.hour < 22

    val minutes = "%.2f".format((activity.secsSinceStart()) / 60.0)

    notificationManager.sendNotification("Time to move!", "${activity.type} for $minutes")
    // Only vibrate when Do Not Disturb mode is off!
    if (allowInterruptions && activityRepository.userIsStillForTooLong()) {
      vibrationManager.vibrate(3000)
    }
  }

}

