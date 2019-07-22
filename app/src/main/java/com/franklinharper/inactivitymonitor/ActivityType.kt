package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity

enum class ActivityType {

  IN_VEHICLE,
  ON_BICYCLE,
  ON_FOOT,
  STILL,
  WALKING,
  RUNNING,
  OTHER;

  companion object {
    // Map the Int provided by the Google API to an enum which is under our control
    fun from(intType: Int): ActivityType {
      return when (intType) {
        DetectedActivity.IN_VEHICLE -> IN_VEHICLE
        DetectedActivity.ON_BICYCLE -> ON_BICYCLE
        DetectedActivity.ON_FOOT -> ON_FOOT
        DetectedActivity.STILL -> STILL
        DetectedActivity.WALKING -> WALKING
        DetectedActivity.RUNNING -> RUNNING
        else -> OTHER
      }
    }
  }
}

enum class TransitionType {

  ENTER, EXIT;

  companion object {
    // Map the Int provided by the Google API to an enum which is under our control
    fun fromInt(intType: Int): TransitionType {
      return when (intType) {
        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> EXIT
        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> ENTER
        else -> throw IllegalStateException()
      }
    }
  }
}