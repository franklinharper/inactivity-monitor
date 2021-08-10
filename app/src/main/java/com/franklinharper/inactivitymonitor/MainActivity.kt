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
import com.franklinharper.inactivitymonitor.permission.Permission
import com.franklinharper.inactivitymonitor.permission.PermissionListener
import com.franklinharper.inactivitymonitor.permission.PermissionManager
import com.franklinharper.inactivitymonitor.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import fr.bipi.tressence.file.FileLoggerTree
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

// TODO Color code the daily movement list
// TODO Move settings to the Main Screen
// TODO Fix bug: Screen Title is not displayed on Settings screen
// TODO Distribute app updates through Play Store internal test channel
// TODO Add custom icon for launcher + notifications
// TODO Polish UI: Colors, Home screen, etc.
// TODO Add picker to define reminder start/stop times
// == Nice to haves, can be done after the first release ==
// TODO Don't set alarms all night long, instead sleep until the reminders start next morning, requires listening to Settings changes so that alarms can be re-scheduled
// TODO add extend snooze feature
// TODO Handle case where the phone is turned off for a while by receiving shutdown broadcasts, and insert a row into the DB. See https://www.google.com/search?client=firefox-b-1-d&q=android+receive+broadcast+when+shutdown
// TODO Support API levels < 26
//    * Provide alternative to opening notification CHANNEL system settings directly

enum class RequestCode {
  SIGN_IN
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject lateinit var movementRepository: EventRepository
  @Inject lateinit var notificationSender: NotificationSender
  @Inject lateinit var alarmScheduler: AlarmScheduler
  @Inject lateinit var appVibrator: AppVibrator
  @Inject lateinit var snooze: Snooze
  @Inject lateinit var reminder: Reminder
  @Inject lateinit var movementRecognitionSubscriber: MovementRecognitionSubscriber
  @Inject lateinit var fileLogger: FileLoggerTree
  private val logFileAdapter = LogFileAdapter()
  private val permissionManager = PermissionManager(this)

  @Suppress("SpellCheckingInspection")
  private val compositeDisposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(findViewById(R.id.main_toolbar))
    initializeBottomNavView()
    initializeHomeView()
    initializeLogView()
    startMovementRecognitionIfPermissionGranted()
    requestCallDetectionPermission()
    showHome()

    alarmScheduler.update()

    val currentUser = Firebase.auth.currentUser
    if (currentUser == null) {
      startUserSignin()
    }
  }

  private fun requestCallDetectionPermission() {
    permissionManager.request(Permission.PHONE_STATE, object : PermissionListener {

      override fun onGranted(permission: Permission) {
//        ui.showPhoneStatePermission(granted = true)
//        Timber.d("PHONE_STATE permission granted")
      }

      override fun onDenied(permission: Permission) {
//        ui.showPhoneStatePermission(granted = false)
      }
    })
  }

  private fun startMovementRecognitionIfPermissionGranted() {
    permissionManager.request(Permission.ACTIVITY_RECOGNITION, object : PermissionListener {

      override fun onGranted(permission: Permission) {
        movementRecognitionSubscriber.subscribe((this@MainActivity).applicationContext)
      }

      override fun onDenied(permission: Permission) {
//        ui.showLocationPermissionDenied()
      }
    })
  }

  private fun initializeHomeView() {
    snoozeButton.setOnClickListener {
      handleSnoozeClick()
    }
  }

  private fun handleSnoozeClick() {
    if (snooze.end != null) {
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

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    permissionManager.onRequestPermissionsResult(requestCode, grantResults)
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
        movementRepository.insert(MovementType.WALKING_START, Status.NEW)
        true
      }
      R.id.action_sync_to_cloud -> {
        movementRepository.syncToCloud()
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
        notificationSender.sendMoveNotification(MovementType.STILL_START, 0.0)
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
      R.id.navigation_events -> showTodaysMovements()
      R.id.navigation_log -> showDeveloperLog()
      else -> throw IllegalStateException()
    }
  }

  private fun showHome() {
    homeContainer.isVisible = true
    todayContainer.isVisible = false
    logContainer.isVisible = false
    val latestMovement = movementRepository.mostRecentMovement()
    val minutes = TimeFormatters.minutes.format(latestMovement.durationSecs / 60.0)
    val movementType = getString(latestMovement.type.stringId)

    currentStatus.text = buildSpannedString {
      append("\nYou have been ")
      bold { append(movementType) }
      append(" for the last ")
      bold { append(minutes) }
      append(" minutes\n")
    }
    val snoozeEnd = snooze.end
    if (snoozeEnd != null) {
      snoozeStatus.isVisible = true
      val endTime = TimeFormatters.time.format(snoozeEnd)
      snoozeStatus.text = getString(R.string.main_activity_snooze_status, endTime)
      snoozeButton.text = getString(R.string.main_activity_cancel_snooze)
    } else {
      snoozeStatus.isVisible = false
      snoozeButton.text = getString(R.string.main_activity_start_snooze)
    }
  }

  private fun showTodaysMovements() {
    homeContainer.isVisible = false
    todayContainer.isVisible = true
    logContainer.isVisible = false
    val todaysMovements = StringBuilder()
    val nowSecs = System.currentTimeMillis() / 1000
    todaysMovements.append(
      "Updated ${TimeFormatters.time.format(
        Instant.ofEpochSecond(
          nowSecs
        )
      )}\n\n"
    )

    movementRepository
      .todaysMovements(stillnessThreshold = 60)
      .reversed()
      .forEach { movement ->
        // Go backwards in time displaying the duration of each successive movement
        val timestamp = TimeFormatters.time.format(movement.start.toZonedDateTime())
        val minutes = "%.1f".format(movement.durationSecs / 60.0)
        val movementType = getString(movement.type.stringId)
        todaysMovements.append(
          getString(R.string.main_activity_event_log, timestamp, movementType, minutes)
        )
      }
    movements.text = todaysMovements.toString()
  }

  private fun showDeveloperLog() {
    homeContainer.isVisible = false
    todayContainer.isVisible = false
    logContainer.isVisible = true
    val file = fileLogger.files.first()
    val items = file.readLines().reversed().map {
      it
        .replaceFirst("/", "\n  ")
        .replaceLast(":", "\n  ")
    }
    logFileAdapter.submitList(items)
  }

  private fun String.replaceLast(old: String, new: String): String {
    val index = lastIndexOf(old)
    return if (index < 0) this else this.replaceRange(index, index + 1, new)
  }

  private val onNavigationItemSelectedListener =
    BottomNavigationView.OnNavigationItemSelectedListener { item ->
      updateSelectedNavigationItem(item.itemId)
      return@OnNavigationItemSelectedListener true
    }

}

