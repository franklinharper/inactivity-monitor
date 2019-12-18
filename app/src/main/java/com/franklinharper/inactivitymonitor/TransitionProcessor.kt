package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber
import java.time.Instant

class TransitionProcessor(
  private val eventRepository: EventRepository = app().eventRepository,
  private val alarmScheduler: AlarmScheduler = app().alarmScheduler,
  private val myVibrator: MyVibrator = app().myVibrator,
  private val myNotificationManager: MyNotificationManager = app().myNotificationManager
) {

  companion object {
    const val ALARM_INTERVAL = 30
    const val INFORMATION_VIBRATION_LENGTH = 1000
    const val MOVEMENT_REQUIRED_VIBRATION_LENGTH = 3000
    private const val STILL_TIME_LIMIT_SECS = 30 * 60 // 30 minutes
  }

  fun processTransitionResult(transitionResult: ActivityTransitionResult?) {
    val latestActivity = eventRepository.latestActivity(Instant.now().epochSecond)
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

  // A Transition returned by the DetectedActivity API can be of the same type as the previous Transition.
  //
  // E.g. over time the stream of Transitions can be: ..., STILL_START, STILL_START, WALKING_START, ...
  //
  // EXIT Transition events are not written to the DB.
  //
  // When successive Activity Types are equal the Transition event is not written to the DB.
  // Deduping the Transition events in this way allows calculating durations by subtracting
  // successive start times.
  //
  // If the Transition events weren't deduped the same calculation would require looping over multiple Transition events.
  private fun processTransitions(
    transitionResult: ActivityTransitionResult,
    latestActivity: UserActivity?
  ) {
    Timber.d("processTransitions: $transitionResult, $latestActivity")
    var previousType = latestActivity?.type
    for (transition in transitionResult.transitionEvents) {
      val newType = EventType.from(transition.activityType)
      if (newType != previousType &&  transition.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
        if (myNotificationManager.doNotDisturbOff) {
          informUserOfNewActivity(newType)
        }
        eventRepository.insert(newType, Status.NEW)
        previousType = newType
      }
    }
  }

  private fun informUserOfNewActivity(activityType: EventType) {
    myVibrator.vibrate(INFORMATION_VIBRATION_LENGTH)
    myNotificationManager.sendCurrentActivityNotification(activityType)
  }

  private fun scheduleNextWakeupAlarm() {
    // TODO wake ourselves up much less often, this brute force approach wastes battery!
    alarmScheduler.createNextAlarm(ALARM_INTERVAL)
//        val latestActivity = transitionRepository.previous().executeAsOneOrNull()
//        if (latestActivity?.activity_type == DetectedActivity.STILL_START) {
//            myAlarmManager.createNextAlarm(ALARM_INTERVAL)
//        } else {
//            myAlarmManager.removeAlarm()
//        }
  }

  private fun userIsStillForTooLong(latestActivity: UserActivity): Boolean =
// TODO Decide how to handle ActivityType.IN_VEHICLE_START? Is it the same as being STILL_START?
    latestActivity.type == EventType.STILL_START && latestActivity.duration > STILL_TIME_LIMIT_SECS

  private fun remindUserToMove(activity: UserActivity) {
    myNotificationManager.sendMoveNotification(activity.type, activity.duration / 60.0)
    myVibrator.vibrate(MOVEMENT_REQUIRED_VIBRATION_LENGTH)
  }
}

