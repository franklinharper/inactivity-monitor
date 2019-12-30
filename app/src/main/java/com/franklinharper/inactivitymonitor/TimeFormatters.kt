package com.franklinharper.inactivitymonitor

import io.reactivex.disposables.CompositeDisposable
import java.text.DecimalFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatters {
  private val zoneId = ZoneId.systemDefault()
  val time: DateTimeFormatter =
    DateTimeFormatter.ofPattern("kk:mm:ss").withZone(zoneId)
  val minutes = DecimalFormat("0.0")
  val dateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId)
}