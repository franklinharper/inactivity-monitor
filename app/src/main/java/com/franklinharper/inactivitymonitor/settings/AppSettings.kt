package com.franklinharper.inactivitymonitor.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.franklinharper.inactivitymonitor.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppSettings @Inject constructor(
  @ApplicationContext private val context: Context,
  private val sharedPreferences: SharedPreferences
) {

  fun notify(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_notify),
      true
    )
  }

  fun vibrate(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_vibrate),
      true
    )
  }

  fun smartStart(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_smart_start),
      true
    )
  }

  // TODO Implement an IntegerPickerPreference
  fun maxStillMinutes(): Int {
    val key = context.getString(R.string.pref_key_reminder_max_still_minutes)
    val value = sharedPreferences.getString(key, "30")
    return value!!.toInt()
  }

  // Storing the start time as a String is a quick hack to save time by not having to implementing
  // a TimePickerPreference.
  // TODO Implementing a TimePickerPreference
  fun reminderStart(): Long {
    val value = sharedPreferences.getString(
      context.getString(R.string.pref_key_reminder_start_hour),
      "6"
    )
    return value!!.toLong()
  }

  // TODO Implementing a TimePickerPreference
  fun reminderEnd(): Long {
    val key = context.getString(R.string.pref_key_reminder_end_hour)
    val value = sharedPreferences.getString(key, "22")
    return value!!.toLong()
  }

  // TODO Implementing a TimePickerPreference
  fun reminderInterval(): Long {
    val key = context.getString(R.string.pref_key_reminder_interval)
    val value = sharedPreferences.getString(key, "30")
    return value!!.toLong()
  }

  private val snoozeEndKey = context.getString(R.string.pref_key_reminder_snooze_end_secs)
  var snoozeEndSecond: Long = -1
    get() = sharedPreferences.getLong(snoozeEndKey, -1)
    set(value) {
      sharedPreferences.edit { putLong(snoozeEndKey, value) }
      field = value
    }
}

