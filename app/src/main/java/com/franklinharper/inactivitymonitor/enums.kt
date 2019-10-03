package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.DetectedActivity

enum class EventType {

  // The names of these enum constants is written to the DB.
  // WARNING: changing the name of one of the enum constants will break compatibility
  //          with DBs containing the previous name.
  IN_VEHICLE_START,
  ON_BICYCLE_START,
  ON_FOOT_START, // The user started walking or running.
  STILL_START,
  WALKING_START, // The user started walking.
  RUNNING_START, // The user started running.
  UNKNOWN_START, // The user started some unknown activity
  // ACTIVITY_END is used to indicate the time at which the end of an Activity occurred.
  ACTIVITY_END;

  companion object {
    // Map the Int provided by the Google API to an enum which is under our control
    fun from(intType: Int): EventType {
      return when (intType) {
        DetectedActivity.IN_VEHICLE -> IN_VEHICLE_START
        DetectedActivity.ON_BICYCLE -> ON_BICYCLE_START
        DetectedActivity.ON_FOOT -> ON_FOOT_START
        DetectedActivity.STILL -> STILL_START
        DetectedActivity.WALKING -> WALKING_START
        DetectedActivity.RUNNING -> RUNNING_START
        // In the case of UNKNOWN_START we are losing information because we won't know what Int value was returned
        // by DetectedActivity.
        //
        // This should NEVER happen, but if it does it will need to be investigated and fixed.
        //
        // If this does become an issue, the enum could be replaced by a sealed class hierarchy,
        // which could store the Int value returned by the DetectedActivity API.
        else -> UNKNOWN_START
      }
    }
  }

}

enum class Status {
  UPLOADED, NEW, DUMMY
}
