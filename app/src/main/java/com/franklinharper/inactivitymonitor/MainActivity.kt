package com.franklinharper.inactivitymonitor

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.sqldelight.runtime.rx.asObservable
import com.squareup.sqldelight.runtime.rx.mapToOptional
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    companion object {
        private const val UNKNOWN_ACTIVITY = "UNKNOWN_ACTIVITY"
    }

    @Suppress("SpellCheckingInspection")
    private val todaysLog = StringBuilder()

    // Track as much data as possible, we'll filter out what we aren't currently interested in when we query the DB.
    private val transitions = listOf<ActivityTransition>(
        createActivityTransition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        // TILTING is not supported. When it is added to this list,
        // requestActivityTransitionUpdates throws an exception with this message:
        //    SecurityException: ActivityTransitionRequest specified an unsupported transition activity type
        // , createActivityTransition(DetectedActivity.TILTING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        // , createActivityTransition(DetectedActivity.TILTING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        , createActivityTransition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        , createActivityTransition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
    )

    private var dashBoardText = ""

    private lateinit var transitionRepository: TransitionRepository
    private lateinit var myAlarmManager: MyAlarmManager

    private var latestActivityType = UNKNOWN_ACTIVITY
    private var latestActivityMinutes = 0.0

    private val zoneId = ZoneId.systemDefault()
    private val timeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss").withZone(zoneId)

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
        update()
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_vibrate -> {
            VibrationManager.from(this).vibrate(2500)
            true
        }

        R.id.action_notify -> {
            MyNotificationManager.from(this).sendNotification("Notification title", "text")
            true
        }

        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...
            Toast.makeText(applicationContext, "Not implemented", Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.TOP or Gravity.END, 0, 200)
                show()
            }
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun update() = updateSelectedNavigationItem(navigationView.selectedItemId)

    private fun initialize() {

        setSupportActionBar(findViewById(R.id.my_toolbar))

        val db = InactivityDb.from(this)
        transitionRepository = TransitionRepository.from(db)
        myAlarmManager = MyAlarmManager.from(this)

        myAlarmManager.createNextAlarm(30)

        observeTransitions()

        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val intent = Intent(this, ActivityTransitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        val client = ActivityRecognition.getClient(this)

        val activityTransitionRequest = ActivityTransitionRequest(transitions)
        val task = client.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)

        task.addOnSuccessListener {
            Timber.d("Success")
        }

        task.addOnFailureListener { e: Exception ->
            Timber.d(e, "Fail")
        }
    }

    private fun updateSelectedNavigationItem(id: Int) {
//        calculateTodaysActivities()
        when (id) {
            R.id.navigation_dashboard -> showDashboard()
            R.id.navigation_log -> showLogForToday()
            R.id.navigation_notifications -> showNotificationsForToday()
            else -> IllegalStateException()
        }
    }

    private fun showDashboard() {
        message.text = dashBoardText
    }

    private fun showLogForToday() {
        calculateTodaysActivities()
        message.text = todaysLog.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun showNotificationsForToday() {
        message.text = "Not yet implemented"
    }

    private fun calculateTodaysActivities() {
        todaysLog.clear()
        val now = System.currentTimeMillis() / 1000
        val formattedNow = timeFormatter.format(Instant.ofEpochSecond(now))
        var previousTimestamp = now
        var previousActivityType = UNKNOWN_ACTIVITY
        var isLatestActivity = true
        var currentActivityMins = 0.0
        todaysLog.append("Updated $formattedNow\n\n")
        transitionRepository.todaysTransitions()
            .executeAsList()
            // Go backwards in time calculating the duration of each successive activity transition
            .forEach { userActivity ->
                with(userActivity) {
                    val formattedTimestamp = timeFormatter.format(Instant.ofEpochSecond(timestamp))
                    val currentActivityType = activityTypeToString(activity_type)
                    if (previousActivityType != currentActivityType && previousActivityType != UNKNOWN_ACTIVITY) {
                        if (isLatestActivity) {
                            latestActivityType = previousActivityType
                            latestActivityMinutes = currentActivityMins
                            isLatestActivity = false
                        }
                        // Reset calculation for the new activity type
                        currentActivityMins = 0.0
                    }
                    currentActivityMins += (previousTimestamp - timestamp) / 60.0
                    val formattedMinutes = "%.2f".format(currentActivityMins)
                    todaysLog.append("$formattedTimestamp => $currentActivityType $formattedMinutes mins\n")
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

    // For faster development, we'll delay implementing I18n until it becomes necessary
    @SuppressLint("SetTextI18n")
    private fun observeTransitions() {
        compositeDisposable += transitionRepository
            .latest()
            .asObservable()
            .mapToOptional()
            .subscribe {
                val currentActivity = if (it.isPresent) activityTypeToString(it.get().activity_type) else "None"
                dashBoardText = "currentActivity: $currentActivity"
            }
//        val formattedMinutes = "%.2f".format(latestActivityMinutes)
//        message.text =
//            """
//
//             Current status:
//
//             $latestActivityType for the last $formattedMinutes minutes.
//
//             latestTransition: $latestTransition
//           """.trimIndent()
    }

}

private operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}
