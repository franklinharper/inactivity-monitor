package com.franklinharper.inactivitymonitor.testfakes

import com.franklinharper.inactivitymonitor.settings.AppSettings

class AppSettingsFake(
  val notify: Boolean = true,
  val vibrate: Boolean = true,
  val smartStart: Boolean = true,
  val maxStillMinutes: Int = 30,
  val reminderStart: Long = 7L,
  val reminderEnd: Long = 22L,
  val reminderInterval: Long = 1L,
  override var snoozeEndSecond:  Long = 0L,
): AppSettings {
  override fun notify() = notify
  override fun vibrate() = vibrate
  override fun smartStart() = smartStart
  override fun maxStillMinutes() = maxStillMinutes
  override fun reminderStart() = reminderStart
  override fun reminderEnd() = reminderEnd
  override fun reminderInterval() = reminderInterval
}