package com.franklinharper.inactivitymonitor.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.franklinharper.inactivitymonitor.NotificationSender

class SystemSettings {

  @RequiresApi(26)
  fun openNotificationChannel(activity: Context, channelId: String) {
//    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
//      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//      .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
//      .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationSender.MOVE_CHANNEL_ID);
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
      .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    activity.startActivity(intent);
  }

}