package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.squareup.sqldelight.Query
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TransitionRepository private constructor(transitionDb: InactivityDb) {

    private val TIMEOUT = 30 * 60 // 30 minutes

    private val zoneId = ZoneId.systemDefault()
    private var userActivityQueries = transitionDb.queries

    companion object : SingletonHolder<TransitionRepository, InactivityDb>(::TransitionRepository)

    fun todaysTransitions(): Query<UserActivityTransition> {
        val todayMidnight = LocalDate.now().atStartOfDay(zoneId)
        val tomorrowMidnight = todayMidnight.plusDays(1)

        return userActivityQueries.select(
            transition = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            startInclusive = todayMidnight.toEpochSecond(),
            endExclusive = tomorrowMidnight.toEpochSecond()
        )
    }

    fun latest() = userActivityQueries.latest()

    fun userIsStillForTooLong(): Boolean {
        val yesterdayMidnight = LocalDate.now().atStartOfDay(zoneId).minusDays(1)
        val transitions = userActivityQueries.selectStarting(
            transition = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            startInclusive = yesterdayMidnight.toEpochSecond()
        )
            .executeAsList()

        if (transitions.isEmpty()) {
            return false
        }

        // Transitions are in reverse chronological order. I.e. most recent at index 0.
        val mostRecentActivityType = activityTypeToString(transitions[0].activity_type)
        var indexFirstDifferent = 1
        while (
            indexFirstDifferent < transitions.size
            && activityTypeToString(transitions[indexFirstDifferent].activity_type) == mostRecentActivityType
        ) {
            indexFirstDifferent++
        }
        val lastSame = transitions[indexFirstDifferent - 1]

        val now = ZonedDateTime.now()
        return mostRecentActivityType == "STILL" && now.toEpochSecond() - lastSame.timestamp > TIMEOUT
    }

    fun insert(activityType: Int, transitionType: Int, elapsedMillis: Long) {
        userActivityQueries.insert(activityType, transitionType, elapsedMillis)
    }
}