package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

class TransitionProcessor(
  private val eventRepository: EventRepository = appComponent().eventRepository,
  private val alarmScheduler: AlarmScheduler = appComponent().alarmScheduler,
  private val myVibrator: MyVibrator = appComponent().myVibrator,
  private val myNotificationManager: MyNotificationManager = appComponent().myNotificationManager
) {

  companion object {
    const val DEFAULT_MAX_WAIT_SECS = 30L
    const val STILL_MAX_TIMEOUT_SECS = 30 * 60L // 30 minutes
    const val INFO_VIBRATION_MILLIS = 1000
    const val MOVE_VIBRATION_MILLIS = 3000
  }

  fun processTransitionResult(transitionResult: ActivityTransitionResult?) {
    if (eventRepository.mostRecentActivity() == null) {
      // This will occur when the app is launched for the first time.
      // We'll pretend the user is STILL
      eventRepository.insert(EventType.STILL_START, Status.NEW)
    }
    val previousActivity = eventRepository.mostRecentActivity()
    processTransitions(transitionResult, previousActivity)
    val mostRecemtActivity = eventRepository.mostRecentActivity()
    val waitSecs = timeoutSecs(mostRecemtActivity)
    if (waitSecs == null) {
      alarmScheduler.removePreviousAlarm()
    } else {
      alarmScheduler.replacePreviousAlarm(waitSecs)
    }
    if (
      mostRecemtActivity != null
      && userIsStillForTooLong(mostRecemtActivity)
      && myNotificationManager.doNotDisturbOff
    ) {
      remindUserToMove(mostRecemtActivity)
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
    transitionResult: ActivityTransitionResult?,
    previousActivity: UserActivity?
  ) {
    Timber.d("processTransitions, previous ${previousActivity?.type}")
    if (transitionResult == null) {
      Timber.d("transitionResult $transitionResult")
      return
    }

    var previousType = previousActivity?.type
    for (transition in transitionResult.transitionEvents) {
      val newType = EventType.from(transition.activityType)
      if (newType != previousType && transition.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
        if (myNotificationManager.doNotDisturbOff) {
          informUserOfNewActivity(newType)
        }
        eventRepository.insert(newType, Status.NEW)
        previousType = newType
      }
    }
  }

  private fun informUserOfNewActivity(activityType: EventType) {
    myVibrator.vibrate(INFO_VIBRATION_MILLIS)
    myNotificationManager.sendCurrentActivityNotification(activityType)
  }

  private fun timeoutSecs(activity: UserActivity?): Long? {
    return when (activity?.type) {
      // Wait 1 extra second to ensure that the timeout has elapsed when the
      // app is woken up.
      EventType.STILL_START -> STILL_MAX_TIMEOUT_SECS - activity.durationSecs + 1
      else -> null
    }
  }

  private fun userIsStillForTooLong(latestActivity: UserActivity): Boolean =
// TODO Decide how to handle ActivityType.IN_VEHICLE_START? Is it the same as STILL_START?
    latestActivity.type == EventType.STILL_START && latestActivity.durationSecs > STILL_MAX_TIMEOUT_SECS

  private fun remindUserToMove(activity: UserActivity) {
    myNotificationManager.sendMoveNotification(activity.type, activity.durationSecs / 60.0)
    myVibrator.vibrate(MOVE_VIBRATION_MILLIS)
  }
}

