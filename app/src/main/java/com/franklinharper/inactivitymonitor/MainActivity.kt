package com.franklinharper.inactivitymonitor

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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

// TODO Handle case where the phone is turned off for a while by receiving shutdown broadcasts, and insert a row into the DB. See https://www.google.com/search?client=firefox-b-1-d&q=android+receive+broadcast+when+shutdown
// TODO make MainActivity reactive.
// TODO vibrate on the watch
// TODO detect activity transitions on the watch

enum class RequestCode {
  SIGN_IN
}

class MainActivity : AppCompatActivity() {

  // We can't inject dependencies through the constructor
  // because this class is instantiated by the Android OS.
  //
  // So we fall back to injecting dependencies directly into the fields.
  private val activityRepository = appComponent().eventRepository
  private val myNotificationManager = appComponent().notificationSender
  private val myAlarmManager = appComponent().alarmScheduler
  private val myVibrator = appComponent().vibratorWrapper
  private val logFileAdapter = LogFileAdapter()
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
    initializeLogView()
    showActivity()

    myAlarmManager.update()

    auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    if (currentUser == null) {
      startUserSignin()
    }
  }

  private fun initializeLogView() {
    developerLog.apply {
      layoutManager = LinearLayoutManager(
        applicationContext,
        LinearLayoutManager.VERTICAL,
        true
      )
      addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
      adapter = logFileAdapter
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
        myVibrator.vibrate(2500)
        true
      }
      R.id.action_notify -> {
        myNotificationManager.sendMoveNotification(EventType.STILL_START, 0.0)
        true
      }
      R.id.action_about -> {
        showAbout()
        true
      }
      else -> {
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        super.onOptionsItemSelected(item)
      }
    }

  }

  private fun showAbout() {
    AlertDialog.Builder(this)
      .setTitle(R.string.action_about)
      .setMessage(getString(R.string.main_activity_version) + " ${BuildConfig.VERSION_NAME}")
      .setPositiveButton(android.R.string.ok, null)
      .create()
      .show()
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
      R.id.navigation_activity -> showActivity()
      R.id.navigation_events -> showEvents()
      R.id.navigation_log -> showDeveloperLog()
      else -> throw IllegalStateException()
    }
  }

  private fun showActivity() {
    message.isVisible = true
    developerLog.isVisible = false
    val contents = SpannableStringBuilder()
    val latestActivity = activityRepository.mostRecentActivity()
    if (latestActivity == null) {
      contents.append(getString(R.string.main_activity_no_activies_detectd))
    } else {
      val minutes = latestActivity.durationSecs / 60.0
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

  private fun showEvents() {
    message.isVisible = true
    developerLog.isVisible = false
    val todaysLog = StringBuilder()
    val nowSecs = System.currentTimeMillis() / 1000
    todaysLog.append("Updated ${timeFormatter.format(Instant.ofEpochSecond(nowSecs))}\n\n")

    activityRepository
      .todaysActivities(stillnessThreshold = 60)
      .reversed()
      .forEach { activity ->
        // Go backwards in time displaying the duration of each successive activity
        val timestamp = timeFormatter.format(activity.start.toZonedDateTime())
        val minutes = "%.2f".format(activity.durationSecs / 60.0)
        todaysLog.append("$timestamp => ${activity.type} $minutes minutes\n")
      }
    message.text = todaysLog.toString()
  }

  private fun showDeveloperLog() {
    developerLog.isVisible = true
    message.isVisible = false
    logFileAdapter.updateData(appComponent().fileLogger.files.first())
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

