package com.franklinharper.inactivitymonitor

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService


class AppVibrator(context: Context) {

  private val vibrator = context.getSystemService<Vibrator>()!!

  // AudioAttributes are required for Vibrations from background processes that work
  // on API >= 29
  //
  // Found on SO:
  // https://stackoverflow.com/questions/57893054/vibration-on-widget-click-not-working-since-api-29
  //
  private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .setUsage(AudioAttributes.USAGE_ALARM)
    .build()

  private val ackMovePattern = longArrayOf(0, 400, 400, 400, 400, 400)
  private val ackMoveAmplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
  private val doNotRepeat = -1
  private val acknowledgeMoveEffect: VibrationEffect = VibrationEffect.createWaveform(
    ackMovePattern,
    ackMoveAmplitudes,
    doNotRepeat
  )

  fun moveReminder() {
    vibrator.vibrate(acknowledgeMoveEffect, audioAttributes)
  }

  fun acknowledgeMovement() {
    vibrator.vibrate(acknowledgeMoveEffect, audioAttributes)
  }

}
