package com.franklinharper.inactivitymonitor

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver

/** A [ColumnAdapter] which maps the enum class `T` to a string in the database. */
private class EnumColumnAdapter<T : Enum<T>> constructor(
    private val enumValues: Array<out T>
) : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T = enumValues.first { it.name == databaseValue }
    override fun encode(value: T) = value.name
}

class ActivityDb private constructor(context: Context) {

    val database: Database = Database(
        driver = AndroidSqliteDriver(Database.Schema, context, "activity.db"),
        userActivityAdapter = UserActivity.Adapter(
            // Write the ActivityType enum to the DB as a text value
            typeAdapter = EnumColumnAdapter(ActivityType.values())
        )
    )
    val queries = database.userActivityQueries

    companion object : SingletonHolder<ActivityDb, Context>(::ActivityDb)
}