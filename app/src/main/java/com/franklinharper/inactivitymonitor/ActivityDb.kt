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
    driver = AndroidSqliteDriver(Database.Schema, application, "database.db"),
    transitionAdapter = Transition.Adapter(
      // Write enums as a text values
      activity_typeAdapter = EnumColumnAdapter(ActivityType.values()),
      transition_typeAdapter = EnumColumnAdapter(TransitionType.values())
    )
  )
  val queries = database.transitionQueries
}