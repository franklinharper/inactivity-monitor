package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import timber.log.Timber

/**
 * Subscribe to updates from the ActivityRecognition service.
 * This subscription must be performed every time the app launches.
 */

class MovementRecognitionSubscriber {

  fun subscribe(context: Context) {
    val transitions = createTransitions()
    val intent = Intent(context, AppBroadcastReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
    val client = ActivityRecognition.getClient(context)
    val activityTransitionRequest = ActivityTransitionRequest(transitions)
    val task = client.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)

    task.addOnSuccessListener {
      Timber.d("Subscription Success")
    }

    task.addOnFailureListener { e: Exception ->
      Timber.e(e, "Subscription Fail")
    }
  }

  private fun createTransitions(): List<ActivityTransition> {
    return listOf(
      createActivityTransition(DetectedActivity.IN_VEHICLE),
      createActivityTransition(
        DetectedActivity.ON_BICYCLE
      )
      ,
      createActivityTransition(
        DetectedActivity.ON_FOOT
      )
      ,
      createActivityTransition(DetectedActivity.STILL)
      ,
      createActivityTransition(
        DetectedActivity.WALKING
      )
      ,
      createActivityTransition(
        DetectedActivity.RUNNING
      )
      // TILTING is not supported. When it is added to this list,
      // requestActivityTransitionUpdates throws an exception with this message:
      //    SecurityException: ActivityTransitionRequest specified an unsupported transition activity type
      // , createActivityTransition(DetectedActivity.TILTING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    )
  }

  // Only detect when activities start (aka ACTIVITY_TRANSITION_ENTER)
  // This implies that we will ignore all ACTIVITY_TRANSITION_EXIT transitions.
  private fun createActivityTransition(type: Int): ActivityTransition {
    return ActivityTransition.Builder()
      .setActivityType(type)
      .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
      .build()
  }

}
