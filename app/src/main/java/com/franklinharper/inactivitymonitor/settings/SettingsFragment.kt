package com.franklinharper.inactivitymonitor.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.franklinharper.inactivitymonitor.NotificationSender
import com.franklinharper.inactivitymonitor.R

class SettingsFragment(val appSettings: AppSettings, val systemSettings: SystemSettings) : PreferenceFragmentCompat() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, rootKey)

    findPref<SwitchPreferenceCompat>(getString(R.string.pref_key_reminder_smart_start))
      .summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { pref ->
      if (pref.isChecked) {
        getString(R.string.settings_reminder_smart_start_summary_on, appSettings.reminderStart())
      } else {
        getString(R.string.settings_reminder_smart_start_summary_off, appSettings.reminderStart())
      }
    }

    findPref<SwitchPreferenceCompat>(getString(R.string.pref_key_reminder_notify))
      .summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { pref ->
      if (pref.isChecked) {
        getString(R.string.settings_reminder_notification_summary_on)
      } else {
        getString(R.string.settings_reminder_notification_summary_off)
      }
    }

    findPref<SwitchPreferenceCompat>(getString(R.string.pref_key_reminder_vibrate))
      .summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { pref ->
      if (pref.isChecked) {
        getString(R.string.settings_reminder_vibrate_summary_on)
      } else {
        getString(R.string.settings_reminder_vibrate_summary_off)
      }
    }

    findPref<EditTextPreference>(getString(R.string.pref_key_reminder_max_still_minutes))
      .summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

    findPref<EditTextPreference>(getString(R.string.pref_key_reminder_start_hour))
      .summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

    findPref<EditTextPreference>(getString(R.string.pref_key_reminder_end_hour))
      .summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

    findPref<EditTextPreference>(getString(R.string.pref_key_reminder_interval))
      .summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

    findPref<Preference>(getString(R.string.pref_key_reminder_system_notification))
      .setOnPreferenceClickListener {
        val context = activity
        if (context != null) {
          systemSettings.openNotificationChannel(context, NotificationSender.MOVE_CHANNEL_ID)
        }
        true
      }
  }

  private fun <T : Preference> findPref(string: String): T {
    return findPreference(string)!!
  }

}