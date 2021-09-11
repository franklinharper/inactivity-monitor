package com.franklinharper.inactivitymonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhoneCall @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private val tm = context.getSystemService(TelecomManager::class.java)

  fun isActive(): Boolean {
    val readPhoneStatePermission = ActivityCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_PHONE_STATE
    )
    return readPhoneStatePermission == PackageManager.PERMISSION_GRANTED
        && tm.isInCall
  }
}
