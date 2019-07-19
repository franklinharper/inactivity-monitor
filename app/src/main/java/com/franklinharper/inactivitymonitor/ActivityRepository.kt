package com.franklinharper.inactivitymonitor

import com.squareup.sqldelight.Query
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId

class ActivityRepository(activityDb: ActivityDb = app().activityDb) {

  private val zoneId = ZoneId.systemDefault()
  private var queries = activityDb.queries

  fun selectLatestActivity(): UserActivity? = queries.selectLatest().executeAsOneOrNull()

  fun todaysActivities(): Query<UserActivity> {
    val todayMidnight = LocalDate.now().atStartOfDay(zoneId)
    val tomorrowMidnight = todayMidnight.plusDays(1)

    return queries.selectRange(
      startInclusive = todayMidnight.toEpochSecond(),
      endExclusive = tomorrowMidnight.toEpochSecond()
    )
  }

  fun insert(activityType: ActivityType, transitionType: Int) {
    Timber.d("Db insert: $activityType, $transitionType")
    queries.insert(activityType, transitionType)
  }
}