package com.franklinharper.inactivitymonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationCompat
import timber.log.Timber

class ForegroundService : Service() {

  override fun onBind(intent: Intent?): IBinder? {
    Timber.i("franktag onBind")
    return null
  }

  override fun onCreate() {
    super.onCreate()
    Timber.i("franktag Service onCreate")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.i("franktag onStartCommand")
    if (intent?.action != null && intent.action.equals(
        ACTION_STOP_FOREGROUND, ignoreCase = true
      )
    ) {
      stopForeground(true)
      stopSelf()
    }
    startSelf()
    return START_STICKY

    //Normal Service To test sample service comment the above    generateForegroundNotification() && return START_STICKY
    // Uncomment below return statement And run the app.
//        return START_NOT_STICKY
  }

  private var iconNotification: Bitmap? = null
  private var notification: Notification? = null
  var mNotificationManager: NotificationManager? = null
  private val mNotificationId = 123

  private fun startSelf() {
    val intentMainLanding = Intent(this, MainActivity::class.java)
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        intentMainLanding,
        PendingIntent.FLAG_IMMUTABLE,
      )
    iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    if (mNotificationManager == null) {
      mNotificationManager =
        this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    assert(mNotificationManager != null)
    mNotificationManager?.createNotificationChannelGroup(
      NotificationChannelGroup("chats_group", "Chats")
    )
    val notificationChannel =
      NotificationChannel(
        "service_channel", "Service Notifications",
        NotificationManager.IMPORTANCE_MIN
      )
    notificationChannel.enableLights(false)
    notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
    mNotificationManager?.createNotificationChannel(notificationChannel)
    val builder = NotificationCompat.Builder(this, NotificationSender.MOVE_CHANNEL_ID)

    builder.setContentTitle(
      StringBuilder(resources.getString(R.string.app_name)).append(" service is running")
        .toString()
    )
      .setTicker(
        StringBuilder(resources.getString(R.string.app_name)).append("service is running")
          .toString()
      )
      .setContentText("Touch to open") //                    , swipe down for more options.
//        .setSmallIcon(R.drawable.ic_alaram)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setWhen(0)
      .setOnlyAlertOnce(true)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
    if (iconNotification != null) {
      builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
    }
//      builder.color = resources.getColor(R.color.purple_200)
    notification = builder.build()
    startForeground(mNotificationId, notification)
  }

  companion object {

    const val ACTION_STOP_FOREGROUND = "${BuildConfig.APPLICATION_ID}.stopforeground"
  }
}