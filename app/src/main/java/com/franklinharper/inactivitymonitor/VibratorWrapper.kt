package com.franklinharper.inactivitymonitor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import androidx.core.content.getSystemService
import timber.log.Timber

class VibratorWrapper(context: Context, val snooze: Snooze) {

  private val vibrator = context.getSystemService<Vibrator>()!!

  fun vibrate(milliseconds: Long) {
    when {
      !vibrator.hasVibrator() -> {
        Timber.e("No vibrator on this device")
      }
      snooze.isActive() -> {
        Timber.i("Snooze active, ignore vibration for $milliseconds milliseconds")
      }
      else -> {
        Timber.i("Start vibration for $milliseconds milliseconds")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(
            VibrationEffect.createOneShot(
              milliseconds,
              DEFAULT_AMPLITUDE
            )
          )
        } else {
          // On older platforms fall back to deprecated API
          @Suppress("DEPRECATION")
          vibrator.vibrate(milliseconds)
        }
      }
    }
  }
}