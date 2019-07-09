package com.franklinharper.inactivitymonitor

import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

class TransitionProcessor(
  private val activityRepository: ActivityRepository = app().activityRepository,
  private val myAlarmManager: MyAlarmManager = app().myAlarmManager,
  private val vibrationManager: VibrationManager = app().vibrationManager,
  private val myNotificationManager: MyNotificationManager = app().myNotificationManager
) {

  companion object {
    private const val ALARM_INTERVAL = 30
    private const val STILL_TIME_LIMIT_SECS = 30 * 60 // 30 minutes
  }

  fun receiveTransition(intent: Intent?) {
    logDebugInfo(intent)
    storeIntentDataInDb(intent)
    scheduleNextWakeupAlarm()
    remindToMoveWhenAppropriate()
  }

  private fun logDebugInfo(intent: Intent?) {
    Timber.d("intent = $intent")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.d("extra $key: ${bundle[key]}")
    }
  }

  // A new Activity Transition can be of the same type as the previous Activity Transition.
  // E.g. over time the stream could be: ..., STILL, STILL, WALKING, ...
  // When successive Activity Types are equal the data is not written to the DB.
  //
  // Deduping the stream allows calculating time duration of an Activity by subtracting successive start timestamps.
  // If the stream wasn't deduped the same calculation would require looping over multiple rows in the table.
  private fun storeIntentDataInDb(intent: Intent?) {
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

  private fun scheduleNextWakeupAlarm() {
    // TODO wake ourselves up much less often, this brute force approach wastes battery!
    myAlarmManager.createNextAlarm(ALARM_INTERVAL)
//        val latestActivity = transitionRepository.previous().executeAsOneOrNull()
//        if (latestActivity?.activity_type == DetectedActivity.STILL) {
//            myAlarmManager.createNextAlarm(ALARM_INTERVAL)
//        } else {
//            myAlarmManager.removeAlarm()
//        }
  }

  private fun remindUserToMove(activity: UserActivity) {
    Timber.d("Reminding user to move")
    myNotificationManager.sendMoveNotification(activity.type, activity.secsSinceStart() / 60.0)
    vibrationManager.vibrate(3000)
  }

  private fun remindToMoveWhenAppropriate() {
    activityRepository
      .selectLatestActivity()
      .executeAsOneOrNull()?.let {
        val remindToMove =
          it.type == ActivityType.STILL // TODO Decide how to handle ActivityType.IN_VEHICLE? The same as being STILL?
            && it.secsSinceStart() > STILL_TIME_LIMIT_SECS
            && myNotificationManager.doNotDisturbOff
        if (remindToMove) {
          remindUserToMove(it)
        } else {
          myNotificationManager.cancelNotification()
        }
      }
  }

}

