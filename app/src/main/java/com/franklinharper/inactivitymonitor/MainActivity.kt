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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// For faster development, we'll delay implementing I18n until it becomes necessary
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

  @Suppress("SpellCheckingInspection")
  private val todaysLog = StringBuilder()

  // Track Activity starts (aka ENTER)
  private val transitions = listOf<ActivityTransition>(
    createActivityTransition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    , createActivityTransition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    , createActivityTransition(DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    , createActivityTransition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    , createActivityTransition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    , createActivityTransition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
    // TILTING is not supported. When it is added to this list,
    // requestActivityTransitionUpdates throws an exception with this message:
    //    SecurityException: ActivityTransitionRequest specified an unsupported transition activity type
    // , createActivityTransition(DetectedActivity.TILTING, ActivityTransition.ACTIVITY_TRANSITION_ENTER)
  )

  private var dashBoardText = ""

  private lateinit var activityRepository: ActivityRepository
  private lateinit var myAlarmManager: MyAlarmManager

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

    val db = ActivityDb.from(this)
    activityRepository = ActivityRepository.from(db)
    myAlarmManager = MyAlarmManager.from(this)

    myAlarmManager.createNextAlarm(30)

    observeRepository()

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
    val latestActivity = activityRepository.selectLatestActivity().executeAsOneOrNull()
    if (latestActivity == null) {
      message.text = "No activities have been detected, try moving around with your phone in your pocket"
    } else {
      val minutes = "%.2f".format(latestActivity.secsSinceStart() / 60.0)
      message.text =
        """

             Current status

             ${latestActivity.type.name} for $minutes minutes.
           """.trimIndent()
    }
  }

  private fun showLogForToday() {
    calculateTodaysActivities()
    message.text = todaysLog.toString()
  }

  private fun showNotificationsForToday() {
    message.text = "Not yet implemented"
  }

  private fun calculateTodaysActivities() {
    todaysLog.clear()
    val nowSecs = System.currentTimeMillis() / 1000
    todaysLog.append("Updated ${timeFormatter.format(Instant.ofEpochSecond(nowSecs))}\n\n")

    var currentEnd = nowSecs
    activityRepository.todaysActivities()
      .executeAsList()
      // Go backwards in time displaying the duration of each successive activity
      .forEach { activity ->
        val timestamp = timeFormatter.format(Instant.ofEpochSecond(activity.start))
        val minutes = "%.2f".format(activity.secsSinceStart(end = currentEnd) / 60.0)
        todaysLog.append("$timestamp => ${activity.type} $minutes mins\n")
        currentEnd = activity.start
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

  private fun observeRepository() {

  }

}

private operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
  add(disposable)
}
