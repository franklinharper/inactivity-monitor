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

    fun toActivity(event: Event?, end: Long): UserActivity? {
      if (event == null) {
        return null
      }
      return UserActivity(
        event.type,
        event.time,
        end - event.time.epochSecond
      )
    }

  }

}

class EventRepository(
  val localDb: LocalDb = app().localDb,
  val remoteDb: RemoteDb = app().remoteDb
) {

  fun mostRecentActivity(end: Long = Instant.now().epochSecond): UserActivity? {
    return UserActivity.toActivity(localDb.queries.selectLatest().executeAsOneOrNull(), end)
  }

  fun todaysActivities(
    stillnessThreshold: Long,
    now: ZonedDateTime = ZonedDateTime.now()
  ): List<UserActivity> {
    val todaysEvents = todaysEvents(now)
    return filterShortStillActivities(stillnessThreshold, Timestamp.from(now), todaysEvents)
  }

  private fun todaysEvents(now: ZonedDateTime): List<Event> {
    val todayMidnight = now.startOfDay
    // Due to daylight saving time changes; the time between the successive midnights can be more or less than 24 hours,
    // that's why it is important to NOT naively add one day to todayMidnight!
    val tomorrowMidnight = now.plusDays(1).startOfDay

    return localDb.queries.selectRange(
      startInclusive = todayMidnight.timestamp,
      endExclusive = tomorrowMidnight.timestamp
    )
      .executeAsList()
  }

  fun insert(activityType: EventType, status: Status) {
    // TODO Prevent inserting an EventType that is equivalent to thelatest activity recorded in
    //      the DB.
    //
    //      What should the UX for this error be?
    //      Should the user be alerted?
    //      Or should the invalid insertion be ignored?
    Timber.d("localDb insert: $activityType, $status")
    localDb.queries.insert(activityType, status)
  }


  fun syncToCloud() {
    // TODO Add a flag to the local DB schema, and use it to track which Activities have already been uploaded
    // TODO Handle the case where it hasn't been possible to sync for more than 24 hours,
    //  and more than one day of activites needs to be written.
    val newEvents = localDb.queries.selectByStatus(Status.NEW).executeAsList()

    newEvents
      .groupBy { event ->
        EventsKey.from(event.time)
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
            onFailure = { e -> Timber.e(e, "Error adding document")
              rollback()
            }
          )
        }
      }
  }

  companion object {

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
        val firstDuration = now.epochSecond - first.time.epochSecond
        if (first.type == STILL_START && firstDuration < shortLimit) {
          return emptyList()
        } else {
          return listOf(UserActivity(first.type, first.time, firstDuration))
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
        time = now,
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
                    waitingToAdd.time,
                    waitingToAdd.timeUntil(previous)
                  )
                )
                waitingToAdd = null
              }
              activities.add(UserActivity(STILL_START, previous.time, stillDuration))
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
                    waitingToAdd.time,
                    waitingToAdd.timeUntil(previous)
                  )
                )
                activities.add(
                  UserActivity(
                    previous.type,
                    previous.time,
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
                    previous.time,
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
            waitingToAdd.time,
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
      if (events.isNotEmpty() && now < last.time.epochSecond) {
        throw IllegalArgumentException("The 'now' timestamp must NOT be before the latest Transition, now: $now, last:  $last")
      }

      if (events.isEmpty()) {
        return
      }

      // Validate ascending order
      var previous = events[0]
      events.drop(1).forEach { next ->
        if (next.time.epochSecond < previous.time.epochSecond) {
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
        activities.add(UserActivity(previous.type, previous.time, duration))
        previous = next
      }
      val latestTransition = events.last()
      val latestActivity = UserActivity(
        latestTransition.type, latestTransition.time, now - latestTransition.time.epochSecond
      )
      activities.add(latestActivity)
      return activities
    }
  }
}

