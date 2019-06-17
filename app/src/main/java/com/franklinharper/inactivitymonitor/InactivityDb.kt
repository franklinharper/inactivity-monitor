package com.franklinharper.inactivitymonitor

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

class InactivityDb private constructor(context: Context) {

    val database : Database
    val queries: UserActivityQueries

    init {
        val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "inactivity.db")
        database = Database(driver)
        queries = database.userActivityQueries
    }

    companion object : SingletonHolder<InactivityDb, Context>(::InactivityDb)
}