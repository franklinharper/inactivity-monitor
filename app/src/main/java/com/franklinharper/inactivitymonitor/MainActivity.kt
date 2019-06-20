package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val transitions = listOf<ActivityTransition>(
        createActivityTransition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
    )

    val request = ActivityTransitionRequest(transitions)

    private lateinit var textMessage: TextView

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                textMessage.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                textMessage.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                textMessage.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val userActivityQueries = InactivityDb.from(this).queries

        val u = userActivityQueries.selectAll().executeAsList()
        u.forEach {
            with(it) {
                val activityType = typeToString(activity_type)
                val transitionType = transitionToString(transition_type)
                val elapsedSeconds = elapsed_real_time_millis / 1000
                Log.i("InactivityMonitor", "$id, $date_time, $activityType, $transitionType, $elapsedSeconds ")
            }
        }

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val intent = Intent(this, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        val client = ActivityRecognition.getClient(this)

        val task = client.requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            Log.d("MAIN", "Success")
        }

        task.addOnFailureListener { e: Exception ->
            Log.d("MAIN", "Fail")
        }
    }

    private fun createActivityTransition(type: Int, transition: Int): ActivityTransition {
        return ActivityTransition.Builder()
            .setActivityType(type)
            .setActivityTransition(transition)
            .build()
    }

    private fun transitionToString(type: Int): String {
        return when(type) {
            0 -> "ENTER"
            1 -> "EXIT"
            else -> type.toString()
        }
    }

    private fun typeToString(type: Int): String {
        return when (type) {
            0 -> "IN_VEHICLE"
            1 -> "ON_BICYCLE"
            2 -> "ON_FOOT"
            3 -> "STILL"
            4 -> "UNKNOWN"
            5 -> "TILTING"
            6, 9, 10, 11, 12, 13, 14, 15 -> type.toString()
            7 -> "WALKING"
            8 -> "RUNNING"
            16 -> "IN_ROAD_VEHICLE"
            17 -> "IN_RAIL_VEHICLE"
            18 -> "IN_TWO_WHEELER_VEHICLE"
            19 -> "IN_FOUR_WHEELER_VEHICLE"
            else -> type.toString()
        }
    }
}
