package com.franklinharper.inactivitymonitor

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
        fun fromInt(intType: Int): ActivityType {
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