package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

// Receive all broadcasts sent to the app (e.g. alarms, notification actions, etc.)
class AppBroadcastReceiver : BroadcastReceiver() {

  // I tried having one SNOOZE action, and using an Extra for the DURATION.
  //
  // That did not turn out well because:
  // - by default PendingIntent.getBroadcast does NOT include the Extras that are set!
  // - setting a flag PendingIntent.FLAG_UPDATE_CURRENT did pass the Extra value,
  //     but only for one DURATION!
  //
  // The workaround I used is to create separate actions for each DURATION.
  //
  // For details see:
  // https://stackoverflow.com/questions/20204284/is-it-possible-to-create-multiple-pendingintents-with-the-same-requestcode-and-d
  //
  enum class Action {
    SNOOZE_15_MINUTES,
    SNOOZE_30_MINUTES,
    SNOOZE_1_HOUR,
  }

  // This class is instantiated by the Android OS.
  // The constructor of this class can't be used to supply the dependencies
  // So we fall back to manually injecting dependencies.
  private val eventRepository = appComponent().eventRepository
  private val alarmScheduler = appComponent().alarmScheduler
  private val reminder = appComponent().reminder
  private val snooze = appComponent().snooze
  private val notificationSender = appComponent().notificationSender

  override fun onReceive(context: Context, intent: Intent?) {
    log(intent)
    if (intent != null) {
      val action = intent.action
      when (action) {
        Action.SNOOZE_15_MINUTES.name -> snoozeAction(SnoozeDuration.FIFTEEN_MINUTES)
        Action.SNOOZE_30_MINUTES.name -> snoozeAction(SnoozeDuration.THIRTY_MINUTES)
        Action.SNOOZE_1_HOUR.name -> snoozeAction(SnoozeDuration.ONE_HOUR)
        else -> {
          val transitionResult = ActivityTransitionResult.extractResult(intent)
          recordEvents(transitionResult)
          alarmScheduler.update()
          reminder.update()
        }
      }
    }
  }

  private fun snoozeAction(duration: SnoozeDuration) {
    snooze.start(duration)
    notificationSender.cancelNotification()
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
    Timber.v("intent $intent")
    Timber.d("action ${intent?.action}")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.d("extra $key ${bundle[key]}")
    }
  }

  companion object {

    fun pendingBroadcastIntent(context: Context, receiverAction: Action): PendingIntent {
      val intent = Intent(context, AppBroadcastReceiver::class.java).apply {
        action = receiverAction.name
      }
      return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        0
      )
    }

  }

}

