package com.franklinharper.inactivitymonitor

import androidx.recyclerview.widget.DiffUtil

sealed class LogEntry(
  val timestamp: String
) {

  data class DefaultEntry(
    val t: String,
    val message: String
  ) : LogEntry(t)

  data class StartOfDay(
    val date: String,
  )

  companion object {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LogEntry>() {
      override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
        return oldItem.timestamp == newItem.timestamp
      }

      override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
        return oldItem == newItem
      }

    }
  }

}
