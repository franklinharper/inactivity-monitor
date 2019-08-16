package com.franklinharper.inactivitymonitor

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val yyyymmddFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun ZonedDateTime.yyyymmdd(): String = format(yyyymmddFormatter)

