package com.franklinharper.inactivitymonitor

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.franklinharper.inactivitymonitor.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Instant

// TODO Optimize alarms so that the app doesn't run as often (e.g. every 30 secs during the night)
// TODO Distribute app updates through Play Store internal test channel
// TODO For API < 26, Provide alternative to opening notification CHANNEL system settings directly
// TODO add custom icon for launcher + notification
// TODO Fix bug: Screen Title is not displayed on Settings screen
// TODO Polish UI: Colors, Home screen, etc.
// TODO Add picker to define reminder start/stop times
// == Nice to haves, can be done after the first release ==
// TODO Don't set alarms all night long, instead sleep until the reminders start next morning, requires listening to Settings changes so that alarms can be re-scheduled
// TODO add extend snooze feature
// TODO Handle case where the phone is turned off for a while by receiving shutdown broadcasts, and insert a row into the DB. See https://www.google.com/search?client=firefox-b-1-d&q=android+receive+broadcast+when+shutdown

enum class RequestCode {
  SIGN_IN
}

class MainActivity : AppCompatActivity() {

  // We can't inject dependencies through the constructor
  // because this class is instantiated by the Android OS.
  //
  // So we fall back to injecting dependencies directly into the fields.
  private val activityRepository = appComponent().eventRepository
  private val notificationSender = appComponent().notificationSender
  private val alarmScheduler = appComponent().alarmScheduler
  private val appVibrator = appComponent().appVibrator
  private val logFileAdapter = LogFileAdapter()
  private val snooze = appComponent().snooze
  private val reminder = appComponent().reminder

  private lateinit var auth: FirebaseAuth

  @Suppress("SpellCheckingInspection")
  private val compositeDisposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById(R.id.main_toolbar))
    initializeBottomNavView()
    initializeHomeView()
    initializeLogView()
    showHome()

    alarmScheduler.update()

    auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    if (currentUser == null) {
      startUserSignin()
    }
  }

  private fun initializeHomeView() {
    snoozeButton.setOnClickListener {
      handleSnoozeClick()
    }
  }

  private fun handleSnoozeClick() {
    if (snooze.isActive()) {
      snooze.cancel()
      showHome()
      snoozeButton.text = getString(R.string.main_activity_start_snooze)
    } else {
      val dialogItems = SnoozeDuration.values()
        .map { getString(it.stringId) }
        .toTypedArray()
      AlertDialog.Builder(this)
        .setTitle(R.string.main_activity_snooze_reminders_for)
        .setItems(dialogItems) { _: DialogInterface, which: Int ->
          snooze.start(SnoozeDuration.values()[which])
          showHome()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create()
        .show()
    }
  }

  private fun initializeLogView() {
    logContainer.apply {
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
      R.id.action_test_reminder -> {
        reminder.update()
        true
      }
      R.id.action_vibrate -> {
        appVibrator.moveReminder()
        true
      }
      R.id.action_notify -> {
        notificationSender.sendMoveNotification(EventType.STILL_START, 0.0)
        true
      }
      R.id.action_settings -> {
        startActivity(SettingsActivity.newIntent(this))
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

  private fun initializeBottomNavView() {
    navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
  }

  private fun updateSelectedNavigationItem(id: Int) {
    when (id) {
      R.id.navigation_activity -> showHome()
      R.id.navigation_events -> showTodaysActivities()
      R.id.navigation_log -> showDeveloperLog()
      else -> throw IllegalStateException()
    }
  }

  private fun showHome() {
    homeContainer.isVisible = true
    todayContainer.isVisible = false
    logContainer.isVisible = false
    val latestActivity = activityRepository.mostRecentActivity()
    val minutes = TimeFormatters.minutes.format(latestActivity.durationSecs / 60.0)
    val activityType = getString(latestActivity.type.stringId)
//    val dndStatus = if (notificationSender.doNotDisturbOn)
//      getText(R.string.main_activity_do_not_disturb_on)
//    else
//      getText(R.string.main_activity_do_not_disturb_off)

    currentStatus.text = buildSpannedString {
      append("\nYou have been ")
      bold { append(activityType) }
      append(" for the last ")
      bold { append(minutes) }
      append(" minutes\n")
//      append(dndStatus)
    }
    if (snooze.isActive()) {
      snoozeStatus.isVisible = true
      val end = TimeFormatters.time.format(snooze.end())
      snoozeStatus.text = getString(R.string.main_activity_snooze_status, end)
      snoozeButton.text = getString(R.string.main_activity_cancel_snooze)
    } else {
      snoozeStatus.isVisible = false
      snoozeButton.text = getString(R.string.main_activity_start_snooze)
    }
  }

  private fun showTodaysActivities() {
    homeContainer.isVisible = false
    todayContainer.isVisible = true
    logContainer.isVisible = false
    val todaysActivities = StringBuilder()
    val nowSecs = System.currentTimeMillis() / 1000
    todaysActivities.append(
      "Updated ${TimeFormatters.time.format(
        Instant.ofEpochSecond(
          nowSecs
        )
      )}\n\n"
    )

    activityRepository
      .todaysActivities(stillnessThreshold = 60)
      .reversed()
      .forEach { activity ->
        // Go backwards in time displaying the duration of each successive activity
        val timestamp = TimeFormatters.time.format(activity.start.toZonedDateTime())
        val minutes = "%.1f".format(activity.durationSecs / 60.0)
        val activityType = getString(activity.type.stringId)
        todaysActivities.append(
          getString(R.string.main_activity_event_log, timestamp, activityType, minutes)
        )
      }
    activities.text = todaysActivities.toString()
  }

  private fun showDeveloperLog() {
    homeContainer.isVisible = false
    todayContainer.isVisible = false
    logContainer.isVisible = true
    logFileAdapter.update(appComponent().fileLogger.files.first())
  }

  private val onNavigationItemSelectedListener =
    BottomNavigationView.OnNavigationItemSelectedListener { item ->
      updateSelectedNavigationItem(item.itemId)
      return@OnNavigationItemSelectedListener true
    }

}

