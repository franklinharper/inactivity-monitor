package com.franklinharper.inactivitymonitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class Settings(val context: Context) {

  val sharedPreferences = context.getSharedPreferences(
    "com.franklinharper.inactivitymonitor.preferences",
    Context.MODE_PRIVATE
  )

  fun notify() : Boolean = sharedPreferences.getBoolean(
    context.getString(R.string.pref_key_notify),
    true
  )

  fun vibrate() : Boolean = sharedPreferences.getBoolean(
    context.getString(R.string.pref_key_vibrate),
    true
  )

}

class SettingsFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, rootKey)
  }
}

class SettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    supportFragmentManager
      .beginTransaction()
      .replace(R.id.settings_container, SettingsFragment())
      .commit()
  }

  companion object {
    fun newIntent(context: Context): Intent {
      return Intent(context, SettingsActivity::class.java)
    }
  }
}

