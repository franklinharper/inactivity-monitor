package com.franklinharper.inactivitymonitor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.VibrationEffect.EFFECT_HEAVY_CLICK
import android.os.Vibrator
import android.util.Log
import timber.log.Timber

class VibrationManager private constructor(context: Context) {

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

    companion object : SingletonHolder<VibrationManager, Context>(::VibrationManager)
}