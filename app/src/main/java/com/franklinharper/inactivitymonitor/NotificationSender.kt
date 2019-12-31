package com.franklinharper.inactivitymonitor

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import timber.log.Timber

class NotificationSender(
  private val context: Context,
  private val snooze: Snooze
) {

  companion object {
    private const val MOVE_CHANNEL_ID = "MOVE"
    private const val ACTIVITY_STATUS_UPDATE_CHANNEL_ID = "STATUS"
    private const val NOTIFICATION_ID = 1
  }

  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannels()
    }
  }

//  val doNotDisturbOff: Boolean
//    get() = currentInterruptionFilter == NotificationManagerCompat.INTERRUPTION_FILTER_ALL
//
//  val doNotDisturbOn: Boolean
//    get() = !doNotDisturbOff
//
//  private val currentInterruptionFilter: Int
//    get() = notificationManager.currentInterruptionFilter

  private fun createNotificationChannels() {
    createChannel(
      id = MOVE_CHANNEL_ID,
      name = context.getString(R.string.notification_move_channel_name),
      description = context.getString(R.string.notification_move_channel_description),
      importance = NotificationManagerCompat.IMPORTANCE_HIGH
    )
    createChannel(
      id = ACTIVITY_STATUS_UPDATE_CHANNEL_ID,
      name = context.getString(R.string.notification_status_channel_name),
      description = context.getString(R.string.notification_status_channel_description),
      importance = NotificationManagerCompat.IMPORTANCE_LOW
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
    val formattedMinutes = TimeFormatters.minutes.format(minutes)
    if (snooze.isActive()) {
      Timber.d("Snooze active, ignored Move notification $type, $formattedMinutes minutes")
    } else {
      Timber.d("Move notification $type, $formattedMinutes minutes")
      notificationManager.notify(
        NOTIFICATION_ID,
        defaultNotificationBuilder(MOVE_CHANNEL_ID)
          .setCategory(NotificationCompat.CATEGORY_REMINDER)
          .setContentTitle(context.getString(R.string.notification_time_to_move_title))
          .build()
      )
    }
  }

  private fun defaultNotificationBuilder(channelId: String): NotificationCompat.Builder {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      0
    )
    return NotificationCompat.Builder(context, channelId)
      .setContentIntent(pendingIntent)
      .setSmallIcon(R.drawable.ic_notifications_black_24dp)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .addAction(
        R.drawable.ic_snooze_black_24dp,
        context.getString(R.string.notification_snooze_15_mins),
        AppBroadcastReceiver.pendingBroadcastIntent(
          context,
          AppBroadcastReceiver.Action.SNOOZE_15_MINUTES
        )
      )
      .addAction(
        R.drawable.ic_snooze_black_24dp,
        context.getString(R.string.notification_snooze_30_mins),
        AppBroadcastReceiver.pendingBroadcastIntent(
          context,
          AppBroadcastReceiver.Action.SNOOZE_30_MINUTES
        )
      )
      .addAction(
        R.drawable.ic_snooze_black_24dp,
        context.getString(R.string.notification_snooze_1_hour),
        AppBroadcastReceiver.pendingBroadcastIntent(
          context,
          AppBroadcastReceiver.Action.SNOOZE_1_HOUR
        )
      )
  }

}
