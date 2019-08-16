package com.franklinharper.inactivitymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import timber.log.Timber

class MyNotificationManager(private val context: Context) {

  companion object {
    private const val MOVE_CHANNEL_ID = "MOVE"
    private const val STATUS_CHANNEL_ID = "STATUS"
    private const val NOTIFICATION_ID = 1
  }

  private val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannels()
    }
  }

  val doNotDisturbOff: Boolean
    get() = currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

  val doNotDisturbOn: Boolean
    get() = !doNotDisturbOff

  private val currentInterruptionFilter: Int
    get() = notificationManager.currentInterruptionFilter

  private fun createNotificationChannels() {
    createChannel(
      id = MOVE_CHANNEL_ID,
      name = context.getString(R.string.notification_move_channel_name),
      description = context.getString(R.string.notification_move_channel_description),
      importance = NotificationManager.IMPORTANCE_HIGH
    )
    createChannel(
      id = STATUS_CHANNEL_ID,
      name = context.getString(R.string.notification_status_channel_name),
      description = context.getString(R.string.notification_status_channel_description),
      importance = NotificationManager.IMPORTANCE_LOW
    )
  }

  private fun createChannel(id: String, name: String, description: String, importance: Int) {
    val channel = NotificationChannel(id, name, importance)
    channel.description = description
    notificationManager.createNotificationChannel(channel)
  }

  fun cancelNotification() {
    notificationManager.cancel(NOTIFICATION_ID)
  }

  fun sendMoveNotification(type: EventType, minutes: Double) {
    Timber.d("Move notification: $type, minutes: $minutes")
    val builder = defaultNotificationBuilder(MOVE_CHANNEL_ID)
    builder
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setContentTitle(context.getString(R.string.notification_time_to_move_title))
      .setContentText(context.getString(R.string.notification_time_to_move_text, type, minutes))
    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  fun sendCurrentActivityNotification(type: EventType) {
    Timber.d("Current Activity notification: $type")
    val builder = defaultNotificationBuilder(STATUS_CHANNEL_ID)
    builder
      .setContentTitle(context.getString(R.string.notification_current_activity_title))
      .setContentText(context.getString(R.string.notification_current_activity_text, type))
    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  private fun defaultNotificationBuilder(channelId: String): NotificationCompat.Builder {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
    return NotificationCompat.Builder(context, channelId)
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.drawable.ic_notifications_black_24dp)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
  }

}
