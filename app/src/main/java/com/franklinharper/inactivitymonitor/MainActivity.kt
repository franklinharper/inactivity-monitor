package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// WIP sync to cloud backend
// TODO optimize AlarmManager usage, by scheduling alarms only when necessary
// TODO Handle case where the phone is turned off for a while by receiving shutdown broadcasts, and insert a row into the DB. See https://www.google.com/search?client=firefox-b-1-d&q=android+receive+broadcast+when+shutdown
// TODO Add ability to turn off  Move It reminders for period of time
// TODO make MainActivity reactive.
// TODO vibrate on the watch
// TODO detect activity transitions on the watch

// ===================================================
// DONE Declutter the Activity log by ignoring short periods of STILLness
// DONE Add first unit tests
// DONE display notification with current Activity, and time
// DONE decouple logic components from UI components

enum class RequestCode {
  SIGN_IN
}

class MainActivity : AppCompatActivity() {

  // We can't inject dependencies through the constructor
  // because this class is instantiated by the Android OS.
  //
  // So we fall back to injecting dependencies directly into the fields.
  private val activityRepository = app().eventRepository
  private val myNotificationManager = app().myNotificationManager
  private val myAlarmManager = app().myAlarmManager
  private val myVibrationManager = app().myVibrationManager
  private lateinit var auth: FirebaseAuth

  @Suppress("SpellCheckingInspection")
  private val zoneId = ZoneId.systemDefault()
  private val timeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss").withZone(zoneId)
  private val compositeDisposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById(R.id.my_toolbar))
    initializeBottomNavView()
    initializeActivityDetection()
    // Schedule a wake up call. Subsequent wake up calls are scheduled when Transition events are processed.
    myAlarmManager.createNextAlarm(30)

    auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    if (currentUser == null) {
      startUserSignin()
    }
  }

  private fun startUserSignin() {
    val authProviders = arrayListOf(
      AuthUI.IdpConfig.GoogleBuilder().build()
    )
    startActivityForResult(
      AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(authProviders)
        .build(),
      RequestCode.SIGN_IN.ordinal
    )
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RequestCode.SIGN_IN.ordinal) {
//      val response = fromResultIntent(data)

      if (resultCode == RESULT_OK) {
        // Successfully signed in
      } else {
        // Sign in failed. If response is null the user canceled the
        // sign-in flow using the back button. Otherwise check
        // response.getError().getErrorCode() and handle the error.
        //
        // TODO display signin retry View
      }
    }
  }


  override fun onResume() {
    super.onResume()
    updateSelectedNavigationItem(navigationView.selectedItemId)
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.toolbar_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {

    return when (item.itemId) {
      R.id.action_record_walking -> {
        activityRepository.insert(EventType.WALKING_START, Status.NEW)
        true
      }
      R.id.action_sync_to_cloud -> {
        activityRepository.syncToCloud()
        true
      }
      R.id.action_vibrate -> {
        myVibrationManager.vibrate(2500)
        true
      }
      R.id.action_notify -> {
        myNotificationManager.sendMoveNotification(EventType.STILL_START, 0.0)
        true
      }
      R.id.action_settings -> {
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

  }

  private fun initializeBottomNavView() =
    navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

  private fun initializeActivityDetection() {
    // Detect when activities start (aka ACTIVITY_TRANSITION_ENTER)
    val transitions = listOf(
      createActivityTransition(
        DetectedActivity.IN_VEHICLE,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER
      )
      ,
      createActivityTransition(
        DetectedActivity.ON_BICYCLE,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER
      )
      ,
      createActivityTransition(
        DetectedActivity.ON_FOOT,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER
      )
      ,
      createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
      ,
      createActivityTransition(
        DetectedActivity.WALKING,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER
      )
      ,
      createActivityTransition(
        DetectedActivity.RUNNING,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER
      )
      // TILTING is not supported. When it is added to this list,
      // requestActivityTransitionUpdates throws an exception with this message:
      //    SecurityException: ActivityTransitionRequest specified an unsupported transition activity type
      // , createActivityTransition(DetectedActivity.TILTING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    )

    val intent = Intent(this, ActivityTransitionReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

    val client = ActivityRecognition.getClient(this)

    val activityTransitionRequest = ActivityTransitionRequest(transitions)
    val task = client.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)

    task.addOnSuccessListener {
      Timber.d("Success")
    }

    task.addOnFailureListener { e: Exception ->
      Timber.e(e, "Fail")
    }
  }

  private fun updateSelectedNavigationItem(id: Int) {
    when (id) {
      R.id.navigation_dashboard -> updateDashboard()
      R.id.navigation_log -> updateLog()
      R.id.navigation_notifications -> updateNotifications()
      else -> throw IllegalStateException()
    }
  }

  private fun updateDashboard() {
    val contents = SpannableStringBuilder()
    val latestActivity = activityRepository.latestActivity(end = Instant.now().epochSecond)
    if (latestActivity == null) {
      contents.append(getString(R.string.main_activity_no_activies_detectd))
    } else {
      val minutes = latestActivity.duration / 60.0
      contents.append(
        getString(
          R.string.main_activity_current_status,
          latestActivity.type,
          minutes
        )
      )
    }
    val dndStatus = if (myNotificationManager.doNotDisturbOn)
      getText(R.string.main_activity_do_not_disturb_on)
    else
      getText(R.string.main_activity_do_not_disturb_off)

    contents.append(dndStatus)
    message.text = contents.toSpannable()
  }

  private fun updateLog() {
    val todaysLog = StringBuilder()
    val nowSecs = System.currentTimeMillis() / 1000
    todaysLog.append("Updated ${timeFormatter.format(Instant.ofEpochSecond(nowSecs))}\n\n")

    activityRepository
      .todaysActivities(stillnessThreshold = 60)
      .reversed()
      .forEach { activity ->
        // Go backwards in time displaying the duration of each successive activity
        val timestamp = timeFormatter.format(activity.start.toZonedDateTime())
        val minutes = "%.2f".format(activity.duration / 60.0)
        todaysLog.append("$timestamp => ${activity.type} $minutes minutes\n")
      }
    message.text = todaysLog.toString()
  }

  private fun updateNotifications() {
    message.text = getString(R.string.main_activity_not_yet_implemented)
  }

  private val onNavigationItemSelectedListener =
    BottomNavigationView.OnNavigationItemSelectedListener { item ->
      updateSelectedNavigationItem(item.itemId)
      return@OnNavigationItemSelectedListener true
    }

  private fun createActivityTransition(type: Int, transition: Int): ActivityTransition {
    return ActivityTransition.Builder()
      .setActivityType(type)
      .setActivityTransition(transition)
      .build()
  }
}

