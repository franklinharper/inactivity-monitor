package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.DetectedActivity

enum class EventType {

  START_IN_VEHICLE,
  START_ON_BICYCLE,
  START_ON_FOOT, // The user started walking or running.
  START_STILL,
  START_WALKING, // The user started walking.
  START_RUNNING, // The user started running.
  START_UNKNOWN, // The user started some unknown activity
  // END_ACTIVITY is used to indicate the time at which the end of an Activity occurred.
  END_ACTIVITY;

  companion object {
    // Map the Int provided by the Google API to an enum which is under our control
    fun from(intType: Int): EventType {
      return when (intType) {
        DetectedActivity.IN_VEHICLE -> START_IN_VEHICLE
        DetectedActivity.ON_BICYCLE -> START_ON_BICYCLE
        DetectedActivity.ON_FOOT -> START_ON_FOOT
        DetectedActivity.STILL -> START_STILL
        DetectedActivity.WALKING -> START_WALKING
        DetectedActivity.RUNNING -> START_RUNNING
        // In the case of START_UNKNOWN we are losing information because we won't know what Int value was returned
        // by DetectedActivity.
        //
        // This should NEVER happen, but if it does it will need to be investigated and fixed.
        //
        // If this does become an issue, the enum could be replaced by a sealed class hierarchy,
        // which could store the Int value returned by the DetectedActivity API.
        else -> START_UNKNOWN
      }
    }
  }

}

enum class Status {
  UPLOADED, NEW, DUMMY
}
