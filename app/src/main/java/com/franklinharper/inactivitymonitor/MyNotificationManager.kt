package com.franklinharper.inactivitymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MyNotificationManager private constructor(private val context: Context) {

    private val CHANNEL_ID = "DEFAULT"
    private val NOTIFICATION_ID = 1

    private val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    val currentInterruptionFilter: Int
        get() = notificationManager.currentInterruptionFilter

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    companion object : SingletonHolder<MyNotificationManager, Context>(::MyNotificationManager)

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

    fun sendNotification(title:String, text: String) {
        val intent = Intent(context, MainActivity::class.java)
            .apply {
                //            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
//            .setContentTitle("Move ASAP!")
//            .setContentText("$latestActivityType for the last $formattedMinutes minutes")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
