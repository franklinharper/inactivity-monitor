package com.franklinharper.inactivitymonitor

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver

/**
 * A SQLDelight ColumnAdapter which maps an enum class `T` to a string in the database.
 */
private class EnumColumnAdapter<T : Enum<T>>(
    private val enumValues: Array<out T>
) : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T = enumValues.first { it.name == databaseValue }
    override fun encode(value: T) = value.name
}

class ActivityDb(application: Context) {

    private val database: Database = Database(
        driver = AndroidSqliteDriver(Database.Schema, application, "activity.db"),
        userActivityAdapter = UserActivity.Adapter(
            // Write the ActivityType enum to the DB as a text value
            typeAdapter = EnumColumnAdapter(ActivityType.values())
        )
    )
    val queries = database.userActivityQueries
}