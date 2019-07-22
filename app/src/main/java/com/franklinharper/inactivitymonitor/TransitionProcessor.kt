package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import java.time.Instant

class TransitionProcessor(
  private val activityRepository: ActivityRepository = app().activityRepository,
  private val myAlarmManager: MyAlarmManager = app().myAlarmManager,
  private val myVibrationManager: MyVibrationManager = app().myVibrationManager,
  private val myNotificationManager: MyNotificationManager = app().myNotificationManager
) {

  companion object {
    const val ALARM_INTERVAL = 30
    const val INFORMATION_VIBRATION_LENGTH = 1000
    const val MOVEMENT_REQUIRED_VIBRATION_LENGTH = 3000
    private const val STILL_TIME_LIMIT_SECS = 30 * 60 // 30 minutes
  }

  fun receiveTransition(transitionResult: ActivityTransitionResult?) {
    val latestActivity = activityRepository.selectLatestActivity(Instant.now().epochSecond)
    if (transitionResult != null) {
      processTransitions(transitionResult, latestActivity)
    }
    scheduleNextWakeupAlarm()
    if (
      latestActivity != null
      && userIsStillForTooLong(latestActivity)
      && myNotificationManager.doNotDisturbOff
    ) {
      remindUserToMove(latestActivity)
    }
  }

  // A new Activity Transition can be of the same type as the previous Activity Transition.
  // E.g. over time the data stream could be: ..., STILL, STILL, WALKING, ...
  //
  // When successive Activity Types are equal the data is not written to the DB.
  // Only ENTER transitions are written to the DB (not EXIT).
  //
  // Deduping the stream in this way allows calculating the duration of an Activity by subtracting
  // successive start timestamps.
  //
  // If the stream wasn't deduped the same calculation would require looping over multiple rows in the table.
  private fun processTransitions(
    transitionResult: ActivityTransitionResult,
    latestActivity: UserActivity?
  ) {
    var previousType = latestActivity?.type
    for (transition in transitionResult.transitionEvents) {
      val newType = ActivityType.from(transition.activityType)
      if (newType != previousType &&  transition.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
        if (myNotificationManager.doNotDisturbOff) {
          informUserOfNewActivity(newType)
        }
        activityRepository.insert(newType, TransitionType.fromInt(transition.transitionType))
        previousType = newType
      }
    }
  }

  private fun informUserOfNewActivity(activityType: ActivityType) {
    myVibrationManager.vibrate(INFORMATION_VIBRATION_LENGTH)
    myNotificationManager.sendCurrentActivityNotification(activityType)
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

  private fun userIsStillForTooLong(latestActivity: UserActivity): Boolean =
// TODO Decide how to handle ActivityType.IN_VEHICLE? Is it the same as being STILL?
    latestActivity.type == ActivityType.STILL && latestActivity.duration > STILL_TIME_LIMIT_SECS

  private fun remindUserToMove(activity: UserActivity) {
    myNotificationManager.sendMoveNotification(activity.type, activity.duration / 60.0)
    myVibrationManager.vibrate(MOVEMENT_REQUIRED_VIBRATION_LENGTH)
  }
}

