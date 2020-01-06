package com.franklinharper.inactivitymonitor.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.franklinharper.inactivitymonitor.NotificationSender
import com.franklinharper.inactivitymonitor.R

class SettingsFragment(val systemSettings: SystemSettings) : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, rootKey)

    findPreference<Preference>(context?.getString(R.string.pref_key_system_notification)!!)!!
      .setOnPreferenceClickListener {
        val context = activity
        if (context != null) {
          systemSettings.openNotificationChannel(context, NotificationSender.MOVE_CHANNEL_ID)
        }
        true
      }

    val reminderStartPref = findPreference<EditTextPreference>(
      getString(R.string.pref_key_reminder_start)
    )
    reminderStartPref!!.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

    val reminderEndPref = findPreference<EditTextPreference>(
      getString(R.string.pref_key_reminder_end)
    )
    reminderEndPref!!.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
  }
}