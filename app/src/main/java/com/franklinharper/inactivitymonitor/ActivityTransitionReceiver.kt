package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import timber.log.Timber

class ActivityTransitionReceiver : BroadcastReceiver() {

  private val ALARM_INTERVAL = 30
  private val STILL_TIME_LIMIT_SECS = 30 * 60 // 30 minutes

  private lateinit var activityRepository: ActivityRepository
  private lateinit var vibrationManager: VibrationManager
  private lateinit var myAlarmManager: MyAlarmManager
  private lateinit var myNotificationManager: MyNotificationManager
  private lateinit var context: Context


  override fun onReceive(context: Context, intent: Intent?) {
    logDebugInfo(context, intent)
    initializeFromContext(context)
    storeIntentDataInDb(intent)
    scheduleNextWakeupAlarm()

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

  private fun logDebugInfo(context: Context, intent: Intent?) {
    Timber.d("onReceive(context = $context,\n intent = $intent)\n")
    val bundle = intent?.extras
    bundle?.keySet()?.forEach { key ->
      Timber.d("extra $key: ${bundle[key]}")
    }
  }

  private fun initializeFromContext(context: Context) {
    this.context = context
    val db = ActivityDb.from(context)
    activityRepository = ActivityRepository.from(db)
    vibrationManager = VibrationManager.from(context)
    myAlarmManager = MyAlarmManager.from(context)
    myNotificationManager = MyNotificationManager.from(context)
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
    myNotificationManager.sendNotification(
      context.getString(R.string.notification_time_to_move_title),
      context.getString(R.string.notification_time_to_move_text, activity.type, activity.secsSinceStart() / 60.0)
    )
    vibrationManager.vibrate(3000)
  }

}

