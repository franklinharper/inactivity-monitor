package com.franklinharper.inactivitymonitor

import com.squareup.sqldelight.Query
import java.time.LocalDate
import java.time.ZoneId

class ActivityRepository(
    activityDb: ActivityDb = app().activityDb
) {
    private val TIMEOUT = 30 * 60 // 30 minutes

    private val zoneId = ZoneId.systemDefault()
    private var queries = activityDb.queries

    fun selectLatestActivity() = queries.selectLatest()

    fun todaysActivities(): Query<UserActivity> {
        val todayMidnight = LocalDate.now().atStartOfDay(zoneId)
        val tomorrowMidnight = todayMidnight.plusDays(1)

        return queries.selectRange(
            startInclusive = todayMidnight.toEpochSecond(),
            endExclusive = tomorrowMidnight.toEpochSecond()
        )
    }

    fun insert(activityType: ActivityType, transitionType: Int) {
        queries.insert(activityType, transitionType)
    }

    fun userIsStillForTooLong(): Boolean {
        return selectLatestActivity().executeAsOneOrNull().let {
            it != null
                    && it.type == ActivityType.STILL
                    && it.secsSinceStart() > TIMEOUT
        }
    }
}