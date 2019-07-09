package com.franklinharper.inactivitymonitor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import timber.log.Timber

class VibrationManager(context: Context) {

    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun vibrate(lengthMillis: Int) {
        if (vibrator.hasVibrator()) {
            Timber.i("Start vibration")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(lengthMillis.toLong(), DEFAULT_AMPLITUDE));
            } else {
                // On older platforms fall back to deprecated API
                @Suppress("DEPRECATION")
                vibrator.vibrate(lengthMillis.toLong());
            }
        } else {
            Timber.e("No vibrator")
        }
    }
}