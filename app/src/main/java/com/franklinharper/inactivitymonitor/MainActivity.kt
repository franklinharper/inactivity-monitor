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

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val intent = Intent(this, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        val client = ActivityRecognition.getClient(this)

        val task = client.requestActivityTransitionUpdates(request,  pendingIntent)

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
}
