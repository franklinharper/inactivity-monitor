package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

class ActivityTransitionReceiver : BroadcastReceiver() {

  // This class is instantiated by the Android OS.
  // The constructor of this class can't be used to supply the dependencies
  // So we fall back to manually injecting dependencies.
  private val eventRepository = appComponent().eventRepository
  private val alarmScheduler  = appComponent().alarmScheduler
  private val reminder = appComponent().reminder

  override fun onReceive(context: Context, intent: Intent?) {
    log(intent)
    val transitionResult = ActivityTransitionResult.extractResult(intent)
    recordEvents(transitionResult)
    alarmScheduler.update()
    reminder.update()
  }

  // A Transition returned by the DetectedActivity API can be of the same type as the previous
  // Transition.
  //
  // E.g. over time the stream of Transitions can be:
  //   ..., STILL_START, STILL_START, WALKING_START, ...
  //
  // EXIT Transition events are not written to the DB.
  //
  // When successive Activity Types are equal the Transition event is not written to the DB.
  // Deduping the Transition events in this way allows calculating durations by subtracting
  // successive start times.
  //
  // If the Transition events weren't deduped the same calculation would require looping over
  // multiple Transition events.
  private fun recordEvents(transitionResult: ActivityTransitionResult?) {
    val previousActivity = eventRepository.mostRecentActivity()
    Timber.d("processTransitions, previous ${previousActivity.type}")
    if (transitionResult == null) {
      Timber.d("Ignoring null transitionResult")
      return
    }

    var previousType = previousActivity.type
    for (transition in transitionResult.transitionEvents) {
      val newType = EventType.from(transition.activityType)
      if (
        newType != previousType &&
        transition.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
      ) {
        eventRepository.insert(newType, Status.NEW)
        previousType = newType
      }
    }
  }

  private fun log(intent: Intent?) {
    Timber.v("intent = $intent")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.v("extra $key ${bundle[key]}")
    }
  }
}

