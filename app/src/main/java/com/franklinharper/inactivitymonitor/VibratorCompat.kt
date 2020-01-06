package com.franklinharper.inactivitymonitor

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import timber.log.Timber

class VibratorCompat(context: Context) {

  private val vibrator = context.getSystemService<Vibrator>()!!

  @RequiresPermission(Manifest.permission.VIBRATE)
  fun vibrate(milliseconds: Long) {
    Timber.i("Start vibration for $milliseconds milliseconds")
    if (Build.VERSION.SDK_INT >= 26) {
      vibrator.vibrate(
        VibrationEffect.createOneShot(milliseconds, DEFAULT_AMPLITUDE)
      )
    } else {
      // On older platforms fall back to deprecated API
      @Suppress("DEPRECATION")
      vibrator.vibrate(milliseconds)
    }
  }
}
