package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber
import java.time.Instant
import kotlin.math.max

class TransitionProcessor(
  private val eventRepository: EventRepository = appComponent().eventRepository,
  private val alarmScheduler: AlarmScheduler = appComponent().alarmScheduler,
  private val myVibrator: MyVibrator = appComponent().myVibrator,
  private val myNotificationManager: MyNotificationManager = appComponent().myNotificationManager
) {

  companion object {
    const val MIN_WAIT_SECS = 30L
    const val STILL_MAX_TIMEOUT_SECS = 30 * 60L // 30 minutes
    const val INFO_VIBRATION_MILLIS = 1000
    const val MOVE_VIBRATION_MILLIS = 3000
  }

  fun processTransitionResult(transitionResult: ActivityTransitionResult?) {
    val previousActivity = eventRepository.mostRecentActivity()
    val mostRecentActivity = insertEvents(transitionResult, previousActivity)
    val waitSecs = waitSecs(mostRecentActivity)
    if (waitSecs == null) {
      alarmScheduler.removePreviousAlarm()
    } else {
      alarmScheduler.replacePreviousAlarm(waitSecs)
    }
    if (userIsStillForTooLong(mostRecentActivity) && myNotificationManager.doNotDisturbOff) {
      remindUserToMove(mostRecentActivity)
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
  private fun insertEvents(
    transitionResult: ActivityTransitionResult?,
    previousActivity: UserActivity
  ): UserActivity {
    Timber.d("processTransitions, previous ${previousActivity.type}")
    if (transitionResult == null) {
      Timber.d("Ignoring null transitionResult")
      return previousActivity
    }

    var previousType = previousActivity.type
    for (transition in transitionResult.transitionEvents) {
      val newType = EventType.from(transition.activityType)
      if (
        newType != previousType &&
        transition.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
      ) {
        // TODO move notification logic into a separate component
//        if (myNotificationManager.doNotDisturbOff) {
//          informUserOfNewActivity(newType)
//        }
        eventRepository.insert(newType, Status.NEW)
        previousType = newType
      }
    }
    val nowSecs = Instant.now().epochSecond
    return UserActivity.toActivity(
      // The Id is set to a fake value, but it isn't used in this case so it should be OK.
      Event.Impl(id = 0, type = previousType, status = Status.NEW, occurred = Timestamp(nowSecs)),
      end = nowSecs
    )
  }

  private fun informUserOfNewActivity(activityType: EventType) {
    myVibrator.vibrate(INFO_VIBRATION_MILLIS)
    myNotificationManager.sendCurrentActivityNotification(activityType)
  }

  private fun waitSecs(activity: UserActivity?): Long? {
    return when (activity?.type) {
      // Wait 1 extra second to ensure that the timeout has expired when the app is woken up.
      // Never wait less than MIN_WAIT_SECS.
      EventType.STILL_START -> {
        max(MIN_WAIT_SECS + 1, STILL_MAX_TIMEOUT_SECS - activity.durationSecs + 1)
      }
      else -> null
    }
  }

  private fun userIsStillForTooLong(latestActivity: UserActivity): Boolean {
    return latestActivity.type == EventType.STILL_START &&
        latestActivity.durationSecs > STILL_MAX_TIMEOUT_SECS
  }

  private fun remindUserToMove(activity: UserActivity) {
    myNotificationManager.sendMoveNotification(activity.type, activity.durationSecs / 60.0)
    myVibrator.vibrate(MOVE_VIBRATION_MILLIS)
  }
}

