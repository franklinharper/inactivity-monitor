package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.IllegalStateException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    private lateinit var userActivityQueries: UserActivityQueries

    private val zoneId = ZoneId.systemDefault()
    private val timeFormatter = DateTimeFormatter.ofPattern("kk:HH:mm:ss").withZone(zoneId)

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                showActivitiesForToday()
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
        userActivityQueries = InactivityDb.from(this).queries

        showActivitiesForToday()

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

    override fun onResume() {
        super.onResume()
        showActivitiesForToday()
    }

    private fun showActivitiesForToday() {
        val todayMidnight = LocalDate.now().atStartOfDay(zoneId)
        val tomorrowMidnight = todayMidnight.plusDays(1)
        val todaysActivities = StringBuilder()

        todaysActivities.append("Activities\n")
        var previousTimestamp = System.currentTimeMillis() / 1000
        var previousActivityType = ""
        var first = true
        var delta = 0L
        userActivityQueries.select(
            transition = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            start = todayMidnight.toEpochSecond(),
            end = tomorrowMidnight.toEpochSecond()
        )
            .executeAsList()
            .forEach {
                with(it) {
                    val formattedTimestamp = timeFormatter.format(Instant.ofEpochSecond(timestamp))
                    val activityType = typeToString(activity_type)
                    delta = when {
//                        previousTimestamp == -1L -> 0L
                        first || activityType == previousActivityType -> {
                            first = false
                            delta + previousTimestamp - timestamp
                        }
                        else -> previousTimestamp - timestamp
                    }
                    todaysActivities.append("$formattedTimestamp => $activityType $delta secs\n")
                    previousTimestamp = timestamp
                    previousActivityType = activityType
                }
            }
        message.text = todaysActivities.toString()
    }

    private fun createActivityTransition(type: Int, transition: Int): ActivityTransition {
        return ActivityTransition.Builder()
            .setActivityType(type)
            .setActivityTransition(transition)
            .build()
    }

    private fun transitionToString(type: Int): String {
        return when (type) {
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
