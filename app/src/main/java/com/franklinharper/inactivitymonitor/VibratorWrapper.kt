package com.franklinharper.inactivitymonitor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import androidx.core.content.getSystemService
import timber.log.Timber

class VibratorWrapper(context: Context) {

    private val vibrator = context.getSystemService<Vibrator>()!!

    fun vibrate(milliseconds: Long) {
        if (vibrator.hasVibrator()) {
            Timber.i("Start vibration for $milliseconds milliseconds")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                    milliseconds,
                    DEFAULT_AMPLITUDE
                ))
            } else {
                // On older platforms fall back to deprecated API
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } else {
            Timber.e("No vibrator on this device")
        }
    }
}