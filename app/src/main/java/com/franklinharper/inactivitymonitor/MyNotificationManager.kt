package com.franklinharper.inactivitymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MyNotificationManager(private val context: Context) {

  private val currentInterruptionFilter: Int
    get() = notificationManager.currentInterruptionFilter

  val doNotDisturbOff: Boolean
    get() = currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

  val doNotDisturbOn: Boolean
    get() = !doNotDisturbOff

  private val CHANNEL_ID = "DEFAULT"
  private val NOTIFICATION_ID = 1

  private val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  private fun createNotificationChannel() {
    val name = context.getString(R.string.channel_name)
    val descriptionText = context.getString(R.string.channel_description)
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(CHANNEL_ID, name, importance)
      .apply {
        description = descriptionText
      }
    notificationManager.createNotificationChannel(channel)
  }

  fun cancelNotification() {
    notificationManager.cancel(NOTIFICATION_ID)
  }

  fun sendMoveNotification(type: ActivityType, minutes: Double) {
    val builder = defaultNotificationBuilder()
    builder
      .setContentTitle(context.getString(R.string.notification_time_to_move_title))
      .setContentText(context.getString(R.string.notification_time_to_move_text, type, minutes))
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  fun sendCurrentActivityNotification(type: ActivityType, minutes: Double) {
    val builder = defaultNotificationBuilder()
    builder
      .setContentTitle(context.getString(R.string.notification_current_activity_title))
      .setContentText(context.getString(R.string.notification_current_activity_text, type, minutes))
      .setCategory(NotificationCompat.CATEGORY_STATUS)
    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  private fun defaultNotificationBuilder(): NotificationCompat.Builder {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
    return NotificationCompat.Builder(context, CHANNEL_ID)
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.drawable.ic_notifications_black_24dp)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
  }

}
