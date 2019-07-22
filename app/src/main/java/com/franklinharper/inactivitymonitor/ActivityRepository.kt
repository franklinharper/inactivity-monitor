package com.franklinharper.inactivitymonitor

import timber.log.Timber
import java.time.ZonedDateTime

// The repository depends on the Database layer, and provides the API for the
// application layer.
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

class ActivityRepository(activityDb: ActivityDb = app().activityDb) {

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

      // The duration of the previous Activity is calculated using the time difference between the start times of
      // 2 successive transitions.
      //
      // For the last transition the next transition doesn't exist!
      //
      // To avoid adding a special case for calculating the duration of the last Activity
      // we add an *end* transition at the end of the list.
      val endTransition = Transition.Impl(
        time = now, activity_type = ActivityType.ON_BICYCLE, transition_type = TransitionType.EXIT, id = Long.MAX_VALUE
      )
      return filter(shortLimit, transitions.toMutableList().also { it.add(endTransition) })
    }

    private fun filter(shortLimit: Long, transitions: List<Transition>): List<UserActivity> {
      val indexFirstNonShortStill = indexFirstNonShortStill(shortLimit, transitions)

      if (indexFirstNonShortStill == -1) {
        val duration = transitions.last().time - transitions.first().time
        return if (duration < shortLimit) {
          emptyList()
        } else {
          listOf(UserActivity(ActivityType.STILL, transitions.first().time, duration))
        }
      }

      val activities = mutableListOf<UserActivity>()
      // The initial activityDuration is 0 when the first
      var activityDuration = transitions[indexFirstNonShortStill].time - transitions.first().time
      var previousType = transitions[indexFirstNonShortStill].activity_type
      // previousStart includes the STILL time before the first non STILL transition.
      var previousStart = transitions.first().time
      for (i in indexFirstNonShortStill + 1 until transitions.size) {
        val previous = transitions[i - 1]
        val next = transitions[i]
        val transitionDuration = next.time - previous.time
        when {
          next.activity_type == ActivityType.STILL && transitionDuration < shortLimit -> {
            activityDuration += transitionDuration
          }
          previousType != next.activity_type -> {
            activities.add(UserActivity(previousType, previousStart, activityDuration + transitionDuration))
            previousType = next.activity_type
            previousStart = next.time
            activityDuration = 0L
          }
        }
      }
      return activities
    }

    private fun indexFirstNonShortStill(shortLimit: Long, transitions: List<Transition>): Int {
      if (transitions.isEmpty()) {
        return -1
      }
      var index = 0
      val startTime = transitions.first().time
      while (index < transitions.size &&
        transitions[index].activity_type == ActivityType.STILL &&
        transitions[index].time - startTime < shortLimit
      ) {
        index++
      }
      return if (index == transitions.size) -1 else index
    }

    private fun validateArguments(shortLimit: Long, now: Long, transitions: List<Transition>) {
      if (shortLimit <= 0) {
        throw IllegalArgumentException()
      }
      if (transitions.isNotEmpty() && now < transitions.last().time) {
        throw IllegalArgumentException()
      }
      for (i in 0 until transitions.size - 1) {
        if (transitions[i + 1].time < transitions[i].time) {
          throw IllegalArgumentException()
        }
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

//      var previousStart = transitions[0].time
////      var activityType = transitions[0].activity_type
////
////      val activities = mutableListOf<UserActivity>()
////
////      for (i in 1 until transitions.size) {
////        val next = transitions[i]
////        val duration = next.time - previousStart
////        when {
////          activityType != ActivityType.STILL || duration > shortLimit -> {
////            activities.add(UserActivity(activityType, previousStart, duration))
////            previousStart = next.time
////            activityType = next.activity_type
////          }
////          else -> {
////            activityType = next.activity_type
////          }
////        }.exhaustiveWhen
////      }
////      return activities
//    }
  }

//      var startCurrentActivity = transitions[0].time
//      var endCurrentActivity = null
//      return transitions
//        .asSequence()
//        .drop(1)
//        .filter { it.activity_type != ActivityType.STILL || it.time - startCurrentActivity < shortLimit }
//        .map {
//          val activity = UserActivity(
//            it.activity_type,
//           startCurrentActivity,
//
//          )
//          endCurrentActivity = it.time
//          activity
//        }
//        .toList()
//    }


  private var queries = activityDb.queries

  fun selectLatestActivity(end: Long): UserActivity? =
    UserActivity.from(queries.selectLatest().executeAsOneOrNull(), end)

  fun todaysActivities(
    stillnessThreshold: Long,
    now: ZonedDateTime = ZonedDateTime.now()
  ): List<UserActivity> {
    val todayMidnight = now.startOfDay()
    val tomorrowMidnight = now.plusDays(1).startOfDay()

    val todaysActivities = queries
      .selectRange(
        startInclusive = todayMidnight.toEpochSecond(),
        endExclusive = tomorrowMidnight.toEpochSecond()
      )
      .executeAsList()
    return filterShortStillActivities(stillnessThreshold, now.toEpochSecond(), todaysActivities)
  }

  fun insert(activityType: ActivityType, transitionType: TransitionType) {
    Timber.d("Db insert: $activityType, $transitionType")
    queries.insert(activityType, transitionType)
  }

  // For an in depth discussion on calculating the start of a day in local time see:
  //   https://stackoverflow.com/questions/29143910/java-8-date-time-get-start-of-day-from-zoneddatetime
  //
  // When the local time changes (e.g. when changing from Summer time to Winter time) there can be 2 midnights!
  // In this case we choose to use the earlier start of the day, and the "day" will be 25 hours long!
  fun ZonedDateTime.startOfDay(): ZonedDateTime = toLocalDate().atStartOfDay(zone)
}
