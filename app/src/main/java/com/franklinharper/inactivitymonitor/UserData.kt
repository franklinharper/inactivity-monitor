package com.franklinharper.inactivitymonitor

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

data class UserActivity(
    val type: String,
    val duration: Int = 0 // In seconds
)
