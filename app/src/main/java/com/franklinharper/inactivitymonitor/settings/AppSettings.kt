package com.franklinharper.inactivitymonitor.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.franklinharper.inactivitymonitor.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AppSettings {
  fun notify(): Boolean
  fun vibrate(): Boolean
  fun smartStart(): Boolean
  fun maxStillMinutes(): Int
  fun reminderStart(): Long
  fun reminderEnd(): Long
  fun reminderInterval(): Long
  var snoozeEndSecond: Long
}

class AppSettingsProduction @Inject constructor(
  @ApplicationContext private val context: Context,
  private val sharedPreferences: SharedPreferences
) : AppSettings {

  override fun notify(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_notify),
      true
    )
  }

  override fun vibrate(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_vibrate),
      true
    )
  }

  override fun smartStart(): Boolean {
    return sharedPreferences.getBoolean(
      context.getString(R.string.pref_key_reminder_smart_start),
      true
    )
  }

  // TODO Implement an IntegerPickerPreference
  override fun maxStillMinutes(): Int {
    val key = context.getString(R.string.pref_key_reminder_max_still_minutes)
    val value = sharedPreferences.getString(key, "30")
    return value!!.toInt()
  }

  // Storing the start time as a String is a quick hack to save time by not having to implementing
  // a TimePickerPreference.
  // TODO Implementing a TimePickerPreference
  override fun reminderStart(): Long {
    val value = sharedPreferences.getString(
      context.getString(R.string.pref_key_reminder_start_hour),
      "6"
    )
    return value!!.toLong()
  }

  // TODO Implement a TimePickerPreference
  override fun reminderEnd(): Long {
    val key = context.getString(R.string.pref_key_reminder_end_hour)
    val value = sharedPreferences.getString(key, "22")
    return value!!.toLong()
  }

  // TODO Implement a TimePickerPreference
  override fun reminderInterval(): Long {
    val key = context.getString(R.string.pref_key_reminder_interval)
    val value = sharedPreferences.getString(key, "30")
    return value!!.toLong()
  }

  private val snoozeEndKey = context.getString(R.string.pref_key_reminder_snooze_end_secs)
  override var snoozeEndSecond: Long
    get() = sharedPreferences.getLong(snoozeEndKey, -1)
    set(value) {
      sharedPreferences.edit { putLong(snoozeEndKey, value) }
    }
}
