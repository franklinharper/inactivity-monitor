package com.franklinharper.inactivitymonitor

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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// TODO snooze vibrations and notifications
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
  private val vibratorWrapper = appComponent().vibratorWrapper
  private val logFileAdapter = LogFileAdapter()
  private lateinit var auth: FirebaseAuth

  @Suppress("SpellCheckingInspection")
  private val zoneId = ZoneId.systemDefault()
  private val timeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss").withZone(zoneId)
  private val compositeDisposable = CompositeDisposable()
  private val minutesFormat = DecimalFormat("0.0")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById(R.id.my_toolbar))
    initializeBottomNavView()
    initializeLogView()
    showActivity()

    alarmScheduler.update()

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
        vibratorWrapper.vibrate(2500)
        true
      }
      R.id.action_notify -> {
        notificationSender.sendMoveNotification(EventType.STILL_START, 0.0)
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
    val latestActivity = activityRepository.mostRecentActivity()
    val minutes = minutesFormat.format(latestActivity.durationSecs / 60.0)
    val activityType = getString(latestActivity.type.stringId)
    val dndStatus = if (notificationSender.doNotDisturbOn)
      getText(R.string.main_activity_do_not_disturb_on)
    else
      getText(R.string.main_activity_do_not_disturb_off)

    message.text = buildSpannedString {
      append("\nYou have been ")
      bold { append(activityType) }
      append(" for the last ")
      bold { append(minutes) }
      append(" minutes\n")
      append(dndStatus)
    }
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
        val minutes = "%.1f".format(activity.durationSecs / 60.0)
        val activityType = getString(activity.type.stringId)
        todaysLog.append(getString(R.string.main_activity_event_log, timestamp, activityType, minutes))
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

}

