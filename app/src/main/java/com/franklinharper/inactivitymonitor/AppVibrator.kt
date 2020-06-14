package com.franklinharper.inactivitymonitor

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class AppVibrator @Inject constructor(
  @ApplicationContext private val context: Context
) {

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
  private val doNotRepeat = -1

  // 4 vibrations
  private val ackMovePattern =   longArrayOf(0, 400, 400, 400, 400, 400, 400, 400)
  private val ackMoveAmplitudes = intArrayOf(0, 255,   0, 255,   0, 255,   0, 255)
  private val acknowledgeMoveEffect: VibrationEffect = VibrationEffect.createWaveform(
    ackMovePattern,
    ackMoveAmplitudes,
    doNotRepeat
  )

  // 3 vibrations
  private val moveReminderPattern   = longArrayOf(0, 400, 400, 400, 400, 400)
  private val moveReminderAmplitudes = intArrayOf(0, 255,   0, 255,   0, 255)
  private val moveReminderEffect: VibrationEffect = VibrationEffect.createWaveform(
    moveReminderPattern,
    moveReminderAmplitudes,
    doNotRepeat
  )

  fun moveReminder() {
    vibrator.vibrate(moveReminderEffect, audioAttributes)
  }

  fun acknowledgeMovement() {
    vibrator.vibrate(acknowledgeMoveEffect, audioAttributes)
  }

}
