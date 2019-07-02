package com.franklinharper.inactivitymonitor

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import timber.log.Timber

class ActivityTransitionReceiver : BroadcastReceiver() {

    //    private val TIMEOUT_SECS = 30 * 60 // 30 * 60
    private val TIMEOUT_SECS = 30 // 30 * 60
    private val STILL_THRESHOLD = 30 * 60 // 30 minutes
    private lateinit var transitionRepository: TransitionRepository
    private lateinit var vibrationManager: VibrationManager
    private lateinit var myAlarmManager: MyAlarmManager
    private lateinit var notificationManager: MyNotificationManager

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("onReceive(context = $context,\n intent = $intent)\n")
        initialize(context)
        processIntent(intent)
        manageWakeupAlarms()
        val latestActivity = transitionRepository.latestActivity()
        latestActivity.let {
            if (it?.type == "STILL" && it.duration > STILL_THRESHOLD) {
                informUser(it)
            }
        }
    }

    private fun initialize(context: Context) {
        val db = InactivityDb.from(context)
        transitionRepository = TransitionRepository.from(db)
        vibrationManager = VibrationManager.from(context)
        myAlarmManager = MyAlarmManager.from(context)
        notificationManager = MyNotificationManager.from(context)
    }

    private fun processIntent(intent: Intent?) {
        if (ActivityTransitionResult.hasResult(intent)) {
            vibrationManager.vibrate(1000)
            val result = ActivityTransitionResult.extractResult(intent)!!
            processTransitionResult(result)
        }
    }

    private fun manageWakeupAlarms() {
        // TODO wake up much less often, this wastes battery!
        myAlarmManager.createNextAlarm(TIMEOUT_SECS)
//        val latestActivity = transitionRepository.latest().executeAsOneOrNull()
//        if (latestActivity?.activity_type == DetectedActivity.STILL) {
//            myAlarmManager.createNextAlarm(TIMEOUT_SECS)
//        } else {
//            myAlarmManager.removeAlarm()
//        }
    }

    private fun informUser(activity: UserActivity) {
        val minutes = "%.2f".format((activity.duration ?: 0) / 60.0)

        notificationManager.sendNotification("Time to move!","${activity.type} for $minutes")
        // Only vibrate when Do Not Disturb mode is off!
        val allowInterruptions =
            notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

        if (allowInterruptions && transitionRepository.userIsStillForTooLong()) {
            vibrationManager.vibrate(3000)
        }
    }

    private fun processTransitionResult(result: ActivityTransitionResult) {
        for (event in result.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {
        val detectedActivity = DetectedActivity(activity.activityType, 100)
        Timber.i("onDetectedTransitionEvent: $detectedActivity")
//        val transition = activity.transitionType.toEnum()
        with(activity) {
            val elapsedMillis = (elapsedRealTimeNanos / 1e+6).toLong()
            transitionRepository.insert(activityType, transitionType, elapsedMillis)
        }
//        when (activity.activityType) {
//            DetectedActivity.ON_BICYCLE,
//            DetectedActivity.RUNNING,
//            DetectedActivity.WALKING -> {
//                // Do whatever you want with the activity
//            }
//            else -> {
//            }
//        }
    }
}

//enum class ActivityTransition {
//
//}
//
//private fun Int.toEnum(): ActivityTransition {
//
//}
//
//private fun Int.toActivityType(): String = when (activity.activityType) {
//    DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
//    DetectedActivity.RUNNING -> "RUNNING"
//    DetectedActivity.WALKING -> "WALKING"
//    else -> this.toString()
//}
