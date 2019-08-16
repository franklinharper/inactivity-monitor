package com.franklinharper.inactivitymonitor

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import java.time.ZonedDateTime

class LocalDb(application: Context) {

  private class TimestampColunmAdapter() : ColumnAdapter<Timestamp, Long> {
    override fun encode(value: Timestamp) = value.unixTime
    override fun decode(databaseValue: Long) = Timestamp(databaseValue)
  }

  /**
   * A SQLDelight ColumnAdapter which maps an enum class `T` to a string in the database.
   */
  private class EnumColumnAdapter<T : Enum<T>>(
    private val enumValues: Array<out T>
  ) : ColumnAdapter<T, String> {

    override fun decode(databaseValue: String): T = enumValues.first { it.name == databaseValue }
    override fun encode(value: T) = value.name

  }

  private val database: Database = Database(
    driver = AndroidSqliteDriver(Database.Schema, application, "local.db"),
    eventAdapter = Event.Adapter(
      // Store enums as text
      typeAdapter = EnumColumnAdapter(EventType.values()),
      statusAdapter = EnumColumnAdapter(Status.values()),
      timeAdapter = TimestampColunmAdapter()
    )
  )

  val queries = database.localdbQueries

}