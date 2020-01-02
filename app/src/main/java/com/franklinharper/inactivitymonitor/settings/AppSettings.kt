package com.franklinharper.inactivitymonitor.settings

import android.content.Context
import androidx.core.content.edit
import com.franklinharper.inactivitymonitor.R

class AppSettings(val context: Context) {

  private val sharedPreferences = context.getSharedPreferences(
    "com.franklinharper.inactivitymonitor.preferences",
    Context.MODE_PRIVATE
  )

  fun notify(): Boolean = sharedPreferences.getBoolean(
    context.getString(R.string.pref_key_notify),
    true
  )

  fun vibrate(): Boolean = sharedPreferences.getBoolean(
    context.getString(R.string.pref_key_vibrate),
    true
  )

  private val snoozeEndKey = context.getString(R.string.pref_key_snooze_end_secs)
  var snoozeEndSecond: Long = -1
    get() = sharedPreferences.getLong(snoozeEndKey, -1)
    set(value) {
      sharedPreferences.edit { putLong(snoozeEndKey, value) }
      field = value
    }
}

