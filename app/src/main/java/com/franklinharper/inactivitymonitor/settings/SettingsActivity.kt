package com.franklinharper.inactivitymonitor.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.franklinharper.inactivitymonitor.R
import com.franklinharper.inactivitymonitor.appComponent

class SettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    supportFragmentManager
      .beginTransaction()
      .replace(
        R.id.settings_container,
        SettingsFragment(appComponent().systemSettings)
      )
      .commit()
  }

  companion object {
    fun newIntent(context: Context): Intent {
      return Intent(
        context,
        SettingsActivity::class.java
      )
    }
  }
}