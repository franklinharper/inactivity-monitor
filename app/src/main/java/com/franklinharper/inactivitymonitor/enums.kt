package com.franklinharper.inactivitymonitor

import androidx.annotation.StringRes
import com.google.android.gms.location.DetectedActivity

enum class MovementType(@StringRes val stringId: Int) {

  // The names of these enum constants is written to the DB.
  // WARNING: changing the name of one of the enum constants will break compatibility
  //          with DBs containing the previous name.
  IN_VEHICLE_START(R.string.event_type_in_vehicle),
  ON_BICYCLE_START(R.string.event_type_on_bicycle),
  ON_FOOT_START(R.string.event_type_on_foot), // The user started walking or running.
  STILL_START(R.string.event_type_still),
  WALKING_START(R.string.event_type_walking), // The user started walking.
  RUNNING_START(R.string.event_type_running), // The user started running.
  UNKNOWN_START(R.string.event_type_unknown), // The user started some unknown activity
  // ACTIVITY_END is used to indicate the time at which the end of an Activity occurred.
  ACTIVITY_END(R.string.event_type_end);

  companion object {
    // Map the Int provided by the Google API to an enum which is under our control
    fun from(detectedActivityType: Int): MovementType {
      return when (detectedActivityType) {
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
