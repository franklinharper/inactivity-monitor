package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.EventType.STILL_START
import timber.log.Timber
import java.time.Instant
import java.time.ZonedDateTime

// The repository
//
// 1) depends on the data layers (local and remote DBs)
// 2) provides the API for the application layer.
//
// The repository should not leak to higher layers ANY implementation details of the underlying layers.

data class UserActivity(
  val type: EventType,
  val start: Timestamp,
  val durationSecs: Long
) {

  companion object {

    fun fromEvent(event: Event, end: Long): UserActivity {
      return UserActivity(
        event.type,
        event.occurred,
        end - event.occurred.epochSecond
      )
    }

    fun fromEvents(events: List<Event>, end: Long): List<UserActivity> {
      var nextEnd = end
      val activities = mutableListOf<UserActivity>()
      events.forEach { event ->
        activities.add(fromEvent(event, nextEnd))
        nextEnd = event.occurred.epochSecond
      }
      return activities
    }

  }

}

interface EventRepository {
  fun mostRecentActivity(end: Long = Instant.now().epochSecond): UserActivity
  fun mostRecentActivities(count: Long, end: Long = Instant.now().epochSecond): List<UserActivity>
  fun insert(activityType: EventType, status: Status)
  fun syncToCloud()
  fun firstMovementAfter(start: ZonedDateTime): EventType?
  fun todaysActivities(
    stillnessThreshold: Long,
    now: ZonedDateTime = ZonedDateTime.now()
  ): List<UserActivity>
}

class DbEventRepository(
  private val localDb: LocalDb = appComponent().localDb,
  private val remoteDb: RemoteDb = appComponent().remoteDb
) : EventRepository {

  override fun mostRecentActivity(end: Long): UserActivity {
    return UserActivity.fromEvent(
      localDb.queries.selectLatest(limit = 1).executeAsOne(),
      end
    )
  }

  override fun mostRecentActivities(limit: Long, end: Long): List<UserActivity> {
    return UserActivity.fromEvents(
      localDb.queries.selectLatest(limit = limit).executeAsList(),
      end
    )
  }

  override fun todaysActivities(
    stillnessThreshold: Long,
    now: ZonedDateTime
  ): List<UserActivity> {
    val todaysEvents = todaysEvents(now)
    return filterShortStillActivities(stillnessThreshold, Timestamp.from(now), todaysEvents)
  }

  private fun todaysEvents(now: ZonedDateTime, limit: Long = UNLIMITED): List<Event> {
    return localDb.queries.selectRange(
      startInclusive = now.startOfDay.timestamp,
      endExclusive = startOfTomorrow(now).timestamp,
      limit = limit
    )
      .executeAsList()
  }

  override fun insert(activityType: EventType, status: Status) {
    if (mostRecentActivity().type == activityType) {
      Timber.w("localDb insert ignoring duplicate, $activityType, $status")
    } else {
      Timber.d("localDb insert, $activityType, $status")
      localDb.queries.insert(activityType, status)
    }
  }

  override fun firstMovementAfter(start: ZonedDateTime): EventType? {
    return localDb.queries.selectRangeExcluding(
      type = STILL_START,
      startInclusive = start.timestamp,
      endExclusive = ZonedDateTime.now().timestamp,
      limit = 1
    )
      .executeAsOneOrNull()
      ?.type
  }

  // Due to daylight saving time changes; the time between the successive days can be more or less
  // than 24 hours!
  // That is why it is important to use the "startOfDay" function instead of naively adding 24 hours to
  // the start of today.
  private fun startOfTomorrow(now: ZonedDateTime): ZonedDateTime {
    return now.plusDays(1).startOfDay
  }

  override fun syncToCloud() {
    // TODO Handle the case where it hasn't been possible to sync for more than 24 hours,
    //  and more than one day of activites needs to be written.
    val newEvents = localDb.queries.selectByStatus(Status.NEW).executeAsList()

    newEvents
      .groupBy { event ->
        EventsKey.from(event.occurred)
      }
      .forEach { eventKey, eventList ->
        localDb.transaction {
          // TODO make writeEvents be synchronous to avoid having to think about what would happen when data is
          //      written to more than one week in the same transaction.
          remoteDb.writeEvents(
            eventKey,
            eventList,
            onSuccess = {
              Timber.d("DocumentSnapshot successfully written!")
              val ids = eventList.map { it.id }
              localDb.queries.setStatus(status = Status.UPLOADED, eventKeys = ids)
            },
            onFailure = { e ->
              Timber.e(e, "Error adding document")
              rollback()
            }
          )
        }
      }
  }

  companion object {

    // In SQLite if the LIMIT expression evaluates to a negative value, then there is no
    // upper bound on the number of rows returned.
    // Arbitrarily the value -1 is used for queries that don't set a limit.
    const val UNLIMITED = -1L

    fun filterShortStillActivities(
      shortLimit: Long,
      now: Timestamp,
      events: List<Event>
    ): List<UserActivity> {

      validateArguments(shortLimit, now.epochSecond, events)

      if (events.isEmpty()) {
        return emptyList()
      }

      if (events.size == 1) {
        val first = events.first()
        val firstDuration = now.epochSecond - first.occurred.epochSecond
        if (first.type == STILL_START && firstDuration < shortLimit) {
          return emptyList()
        } else {
          return listOf(UserActivity(first.type, first.occurred, firstDuration))
        }
      }
      // The duration of the previous Activity is calculated using the time difference between the start times of
      // 2 successive events.
      //
      // For the last event the next event doesn't exist!
      //
      // To avoid adding a special case for calculating the duration of the last Activity
      // we add an *end* event to the end of the list.
      val endTransition = Event.Impl(
        occurred = now,
        type = EventType.ACTIVITY_END,
        id = Long.MAX_VALUE,
        status = Status.DUMMY
      )
      return filter(shortLimit, events.toMutableList().also { it.add(endTransition) })
    }

    private fun filter(shortLimit: Long, events: List<Event>): List<UserActivity> {

      var waitingToAdd: Event? = null
      var previous: Event = events[0]
      val activities = mutableListOf<UserActivity>()
      for (nextIndex in 1 until events.size) {
        val next = events[nextIndex]
        when {
          previous.type == STILL_START -> {
//            val stillDuration = next.time - previous.time
//            val stillDuration = previous.time.unixTime - next.time.unixTime
            val stillDuration = previous.timeUntil(next)
            if (stillDuration >= shortLimit) {
              if (waitingToAdd != null) {
                activities.add(
                  UserActivity(
                    waitingToAdd.type,
                    waitingToAdd.occurred,
                    waitingToAdd.timeUntil(previous)
                  )
                )
                waitingToAdd = null
              }
              activities.add(UserActivity(STILL_START, previous.occurred, stillDuration))
            }
          }

          else -> {
            if (next.type == STILL_START && waitingToAdd == null) {
              waitingToAdd = previous
            } else {
              if (waitingToAdd != null && waitingToAdd.type != previous.type) {
                activities.add(
                  UserActivity(
                    waitingToAdd.type,
                    waitingToAdd.occurred,
                    waitingToAdd.timeUntil(previous)
                  )
                )
                activities.add(
                  UserActivity(
                    previous.type,
                    previous.occurred,
                    previous.timeUntil(next)
                  )
                )
                waitingToAdd = null
              } else if (waitingToAdd != null && waitingToAdd.type == previous.type) {
                // DO NOTHING
              } else {
                activities.add(
                  UserActivity(
                    previous.type,
                    previous.occurred,
                    previous.timeUntil(next)
                  )
                )
              }
            }
          }
        }
        previous = next
      }
      if (waitingToAdd != null) {
        activities.add(
          UserActivity(
            waitingToAdd.type,
            waitingToAdd.occurred,
            waitingToAdd.timeUntil(events.last())
          )
        )
      }
      return activities
    }

    private fun validateArguments(shortLimit: Long, now: Long, events: List<Event>) {
      if (shortLimit <= 0) {
        throw IllegalArgumentException("shortLimit: $shortLimit")
      }

      if (events.isEmpty()) return

      val last = events.last()
      if (events.isNotEmpty() && now < last.occurred.epochSecond) {
        throw IllegalArgumentException("The 'now' timestamp must NOT be before the latest Transition, now: $now, last:  $last")
      }

      if (events.isEmpty()) {
        return
      }

      // Validate ascending order
      var previous = events[0]
      events.drop(1).forEach { next ->
        if (next.occurred.epochSecond < previous.occurred.epochSecond) {
          throw IllegalArgumentException(
            "The next transition must NOT be before the previous Transition, previous: $previous, next: $next"
          )
        }
        previous = next
      }
    }

    private fun List<Event>.toActivities(now: Long): List<UserActivity> {
      val events = this
      if (events.isEmpty()) {
        return emptyList()
      }

      val activities = mutableListOf<UserActivity>()
      var previous = events[0]
      for (i in 1 until events.size) {
        val next = events[i]
        val duration = previous.timeUntil(next)
        activities.add(UserActivity(previous.type, previous.occurred, duration))
        previous = next
      }
      val latestTransition = events.last()
      val latestActivity = UserActivity(
        latestTransition.type,
        latestTransition.occurred,
        now - latestTransition.occurred.epochSecond
      )
      activities.add(latestActivity)
      return activities
    }
  }
}

