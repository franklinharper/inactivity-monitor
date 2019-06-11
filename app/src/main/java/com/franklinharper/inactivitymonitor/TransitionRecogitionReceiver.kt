package com.franklinharper.inactivitymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.lang.IllegalStateException

class ActivityTransitionReceiver : BroadcastReceiver() {

    lateinit var context: Context

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context!!

//        Log.i("RECEIVER", intent.toString())
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            processTransitionResult(result)
        }
    }

    private fun processTransitionResult(result: ActivityTransitionResult) {
//        Log.d("RECEIVER", result.toString())
        for (event in result.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {
        val detectedActivity = DetectedActivity(activity.activityType, 100)
//        val transition = activity.transitionType.toEnum()
//        val elapsedTime = activity.elapsedRealTimeNanos / 1e+9
        Log.i("InactivityMonitor", "$detectedActivity")
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
