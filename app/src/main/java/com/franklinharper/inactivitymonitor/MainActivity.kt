package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    companion object {
        private const val UNKNOWN_ACTIVITY = "UNKNOWN_ACTIVITY"
    }

    @Suppress("SpellCheckingInspection")
    private val todaysLog = StringBuilder()

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


    private var latestActivityType = UNKNOWN_ACTIVITY
    private var latestActivityMinutes = 0.0

    private lateinit var userActivityQueries: UserActivityQueries

    private val zoneId = ZoneId.systemDefault()
    private val timeFormatter = DateTimeFormatter.ofPattern("kk:HH:mm:ss").withZone(zoneId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userActivityQueries = InactivityDb.from(this).queries

        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val intent = Intent(this, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        val client = ActivityRecognition.getClient(this)

        val activityTransitionRequest = ActivityTransitionRequest(transitions)
        val task = client.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)

        task.addOnSuccessListener {
            Log.d("MAIN", "Success")
        }

        task.addOnFailureListener { e: Exception ->
            Log.d("MAIN", "Fail")
        }
        calculateTodaysActivities()
        updateSelectedNavigationItem(navigationView.selectedItemId)
    }

    override fun onResume() {
        super.onResume()
        calculateTodaysActivities()
        updateSelectedNavigationItem(navigationView.selectedItemId)
    }

    private fun updateSelectedNavigationItem(id:Int) {
        calculateTodaysActivities()
        when (id) {
            R.id.navigation_dashboard -> showDashboardForToday()
            R.id.navigation_log -> showLogForToday()
            R.id.navigation_notifications -> showNotificationsForToday()
            else -> IllegalStateException()
        }
    }

    // For faster development, we'll delay implementing I18n until it becomes necessary
    @SuppressLint("SetTextI18n")
    private fun showDashboardForToday() {
        val formattedMinutes = "%.2f".format(latestActivityMinutes)
        message.text =
            """
               
             Current status:
             
             $latestActivityType for the last $formattedMinutes minutes.
           """.trimIndent()
    }

    private fun showLogForToday() {
        message.text = todaysLog.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun showNotificationsForToday() {
        message.text = "Not yet implemented"
    }

    @Suppress("SpellCheckingInspection")
    private fun calculateTodaysActivities() {

        todaysLog.clear()
        val now = System.currentTimeMillis() / 1000
        val formattedNow = timeFormatter.format(Instant.ofEpochSecond(now))
        todaysLog.append("Updated $formattedNow\n\n")
        val todayMidnight = LocalDate.now().atStartOfDay(zoneId)
        val tomorrowMidnight = todayMidnight.plusDays(1)

        var previousTimestamp = now
        var previousActivityType = UNKNOWN_ACTIVITY
        var currentActivitySecs = 0L
        var isLatestActivity = true
        userActivityQueries.select(
            transition = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            startInclusive = todayMidnight.toEpochSecond(),
            endExclusive = tomorrowMidnight.toEpochSecond()
        )
            .executeAsList()
            // Go backwards in time calculating the duration of each successive activity transition
            .forEach {userActivity ->
                with(userActivity) {
                    val formattedTimestamp = timeFormatter.format(Instant.ofEpochSecond(timestamp))
                    val currentActivityType = activity_type.activityTypeToString()
                    if (previousActivityType != currentActivityType && previousActivityType != UNKNOWN_ACTIVITY) {
                        if (isLatestActivity) {
                            latestActivityType = previousActivityType
                            latestActivityMinutes = currentActivitySecs / 60.0
                            isLatestActivity = false
                        }
                        // start calculation for the new activity type
                        currentActivitySecs = 0L
                    }
                    currentActivitySecs += previousTimestamp - timestamp
                    todaysLog.append("$formattedTimestamp => $currentActivityType $currentActivitySecs secs\n")
                    previousTimestamp = timestamp
                    previousActivityType = currentActivityType
                }
            }
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        updateSelectedNavigationItem(item.itemId)
        return@OnNavigationItemSelectedListener true
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

    private fun Int.activityTypeToString(): String {
        return when (this) {
            0 -> "IN_VEHICLE"
            1 -> "ON_BICYCLE"
            2 -> "ON_FOOT"
            3 -> "STILL"
            4 -> "UNKNOWN_ACTIVITY"
            5 -> "TILTING"
            6, 9, 10, 11, 12, 13, 14, 15 -> this.toString()
            7 -> "WALKING"
            8 -> "RUNNING"
            16 -> "IN_ROAD_VEHICLE"
            17 -> "IN_RAIL_VEHICLE"
            18 -> "IN_TWO_WHEELER_VEHICLE"
            19 -> "IN_FOUR_WHEELER_VEHICLE"
            else -> this.toString()
        }
    }
}
