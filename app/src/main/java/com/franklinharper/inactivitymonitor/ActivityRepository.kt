package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.ActivityType.STILL
import timber.log.Timber
import java.time.ZonedDateTime

// The repository depends on the Database layer, and provides the API for the application layer.
//
// The repository should not leak any information to the application about the underlying database implementation.

data class UserActivity(
  val type: ActivityType,
  val start: Long,
  val duration: Long
) {

  companion object {

    fun from(transition: Transition?, end: Long): UserActivity? {
      if (transition == null) {
        return null
      }
      return UserActivity(
        transition.activity_type,
        transition.time,
        end - transition.time
      )
    }

  }

}

class ActivityRepository(
  val localDb: LocalDb = app().localDb,
  val cloudDb: MyFirebaseDb
) {


  fun selectLatestActivity(end: Long): UserActivity? =
    UserActivity.from(localDb.queries.selectLatest().executeAsOneOrNull(), end)

  fun todaysActivities(
    stillnessThreshold: Long,
    now: ZonedDateTime = ZonedDateTime.now()
  ): List<UserActivity> {
    val todayMidnight = now.startOfDay()
    val tomorrowMidnight = now.plusDays(1).startOfDay()

    val todaysTransitions = localDb.queries.selectRange(
      startInclusive = todayMidnight.toEpochSecond(),
      endExclusive = tomorrowMidnight.toEpochSecond()
    )
      .executeAsList()
    return filterShortStillActivities(stillnessThreshold, now.toEpochSecond(), todaysTransitions)
  }

  fun insert(activityType: ActivityType, transitionType: TransitionType) {
    Timber.d("Db insert: $activityType, $transitionType")
    localDb.queries.insert(activityType, transitionType)
  }

  // For an in depth discussion on calculating the start of a day in local time see:
  //   https://stackoverflow.com/questions/29143910/java-8-date-time-get-start-of-day-from-zoneddatetime
  //
  // When the local time changes (e.g. when changing from Summer time to Winter time) there can be 2 midnights!
  // In this case we choose to use the earlier start of the day, and the "day" will be 25 hours long!
  fun ZonedDateTime.startOfDay(): ZonedDateTime = toLocalDate().atStartOfDay(zone)

  fun syncToCloud(time: ZonedDateTime) {
    // TODO Add a flag to the local DB schema, and use it to track which Activities have already been uploaded
    // TODO Handle the case where it hasn't been possible to sync for more than 24 hours,
    //  and more than one day of activites needs to be written.
    val todaysActivities = todaysActivities(stillnessThreshold = 60)
    cloudDb.writeDailyActivities(time, todaysActivities)
  }

  companion object {

    fun filterShortStillActivities(
      shortLimit: Long,
      now: Long,
      transitions: List<Transition>
    ): List<UserActivity> {

      validateArguments(shortLimit, now, transitions)

      if (transitions.isEmpty()) {
        return emptyList()
      }

      if (transitions.size == 1) {
        val first = transitions.first()
        val firstDuration = now - first.time
        if (first.activity_type == STILL && firstDuration < shortLimit) {
          return emptyList()
        } else {
          return listOf(UserActivity(first.activity_type, first.time, firstDuration))
        }
      }
      // The duration of the previous Activity is calculated using the time difference between the start times of
      // 2 successive transitions.
      //
      // For the last transition the next transition doesn't exist!
      //
      // To avoid adding a special case for calculating the duration of the last Activity
      // we add an *end* transition at the end of the list.
      val endTransition = Transition.Impl(
        time = now, activity_type = ActivityType.SENTINEL, transition_type = TransitionType.EXIT, id = Long.MAX_VALUE
      )
      return filter(shortLimit, transitions.toMutableList().also { it.add(endTransition) })
    }

    private fun filter(shortLimit: Long, transitions: List<Transition>): List<UserActivity> {

      var waitingToAdd: Transition? = null
      var previous: Transition = transitions[0]
      val activities = mutableListOf<UserActivity>()
      for (nextIndex in 1 until transitions.size) {
        val next = transitions[nextIndex]
        when {
          previous.activity_type == STILL -> {
            val stillDuration = next.time - previous.time
            if (stillDuration >= shortLimit) {
              if (waitingToAdd != null) {
                activities.add(
                  UserActivity(
                    waitingToAdd.activity_type,
                    waitingToAdd.time,
                    previous.time - waitingToAdd.time
                  )
                )
                waitingToAdd = null
              }
              activities.add(UserActivity(STILL, previous.time, stillDuration))
            }
          }

          else -> {
            if (next.activity_type == STILL && waitingToAdd == null) {
              waitingToAdd = previous
            } else {
              if (waitingToAdd != null && waitingToAdd.activity_type != previous.activity_type) {
                activities.add(
                  UserActivity(
                    waitingToAdd.activity_type,
                    waitingToAdd.time,
                    previous.time - waitingToAdd.time
                  )
                )
                activities.add(
                  UserActivity(
                    previous.activity_type,
                    previous.time,
                    next.time - previous.time
                  )
                )
                waitingToAdd = null
              } else if (waitingToAdd != null && waitingToAdd.activity_type == previous.activity_type) {
                // DO NOTHING
              } else {
                activities.add(
                  UserActivity(
                    previous.activity_type,
                    previous.time,
                    next.time - previous.time
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
            waitingToAdd.activity_type,
            waitingToAdd.time,
            transitions.last().time - waitingToAdd.time
          )
        )
      }
      return activities
    }

    private fun validateArguments(shortLimit: Long, now: Long, transitions: List<Transition>) {
      if (shortLimit <= 0) {
        throw IllegalArgumentException("shortLimit: $shortLimit")
      }

      if (transitions.isEmpty()) return

      val last = transitions.last()
      if (transitions.isNotEmpty() && now < last.time) {
        throw IllegalArgumentException("The 'now' timestamp must NOT be before the latest Transition, now: $now, last:  $last")
      }

      if (transitions.isEmpty()) {
        return
      }

      // Validate ascending order
      var previous = transitions[0]
      transitions.drop(1).forEach { next ->
        if (next.time < previous.time) {
          throw IllegalArgumentException(
            "The next transition must NOT be before the previous Transition, previous: $previous, next: $next"
          )
        }
        previous = next
      }
    }

    private fun List<Transition>.toActivities(now: Long): List<UserActivity> {
      val transitions = this
      if (transitions.isEmpty()) {
        return emptyList()
      }

      val activities = mutableListOf<UserActivity>()
      var previous = transitions[0]
      for (i in 1 until transitions.size) {
        val next = transitions[i]
        val duration = next.time - previous.time
        activities.add(UserActivity(previous.activity_type, previous.time, duration))
        previous = next
      }
      val latestTransition = transitions.last()
      val latestActivity = UserActivity(
        latestTransition.activity_type, latestTransition.time, now - latestTransition.time
      )
      activities.add(latestActivity)
      return activities
    }
  }
}
