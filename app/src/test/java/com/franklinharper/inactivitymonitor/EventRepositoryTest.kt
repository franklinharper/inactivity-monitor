package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.EventType.*
import com.squareup.sqldelight.Query
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.stream.Stream

class EventRepositoryTest {

  companion object {

    // Ignore "unused" inspection for this function because it is used by the
    // @MethodSource annotation via reflection.
    @Suppress("unused")
    @JvmStatic
    fun argumentsForfilterShortStillActivitiesTest(): Stream<Arguments> {
      // Define inputs, and expected results
      // ===================================
      //
      // Notation
      // ========
      //   SS => Short Still Activity
      //   LS => Long Still Activity
      //   SA => Short Activity (NOT Still)
      //   LA => Long Activity (NOT Still)
      //
      // Input requirements
      // ==================
      //
      // Events must be in ascending chronological order
      // => transition[n].time <= transition[n+1].time
      //
      // The type of a transition must be different from the activity type of the previous transition
      // => transition[n].type != transition[n+1].type
      //
      // Test cases
      // ==========
      //
      // Empty Input
      // ===========
      //   case 1: () -> ))
      //
      //   Input contains 1 Event
      //   case 2: (SS) -> ()
      //   case 3: (x) -> (x) when x in { LS, SA, LA }
      //
      // Input contains 2 Events
      // ============================
      //
      //   Short Still cases
      //   case 4: (SS, x) -> (x), when x in { SA, LA }
      //   case 5: (x, SS) -> (x + SS), when x in { SA, LA }
      //
      //   General case
      //   case 6: (x, y) -> (x, y), when x != y AND x,y in { LS, SA, LA }
      //
      // Input contains 3 Events
      // ============================
      //
      //   Short Still cases
      //   case 7: (SS, x, y) -> (x, y), when x,y in { SA, LA }
      //   case 8: (x, SS, y) -> (x + SS, y), when x != y, AND x in { SA, LA }, y in { SA, LA }
      //   case 9: (x1, SS, x2) -> (x1 + SS + x2), when x1 == x2, AND x1, x2 in { SA, LA }
      //   case 10: (x, y, SS) -> (x, y + SS), when y != x AND x in { LS, SA, LA }, in { LS, SA, LA }
      //
      //   General case
      //   case 11: (x, y, z) -> (x, y, z), when x != y AND y != z AND x,y,z = { LS, SA, LA }
      //
      // Input contains 4 Events
      // ============================
      //
      //   Short Still cases
      //   case 12: (SS1, x1, SS2, x2) -> (x1 + SS2 + x2), when x1, x2 in { SA, LA }
      //   case 13: (x1, SS1, x2, SS2) -> (x1 + SS1 + x2 + SS2), when x1, x2 in { SA, LA }
      //   case 14: (x1, LS, x2, SS) -> (x1, LS, x2 + SS), when x1, x2 in { SA, LA }
      //   case 15: (x1, SS, x2, LS) -> (x1 + SS, x2, LS), when x1, x2 in { SA, LA }

      return Stream.of(

        // Empty input
        //   case 1: () -> ))
        Arguments.of(
          /* caseNumber */ 1,
          /* shortLimit */ 60,
          /* now */ Timestamp(0),
          /* input */
          emptyList<Event.Impl>(),
          /* expected */
          emptyList<UserActivity>()
        ),

        // Cases when the input contains 1 Event
        // ==========================================
        //
        // case 2: (SS) -> ()
        Arguments.of(
          /* caseNumber */ 2,
          /* shortLimit */ 60,
          /* now */ Timestamp(59),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = STILL_START,  id = 0, status = Status.NEW)
          ),
          /* expected */
          emptyList<UserActivity>()
        ),

        // case 3: (x) -> (x) when x == SA
        Arguments.of(
          /* caseNumber */ 3,
          /* shortLimit */ 60,
          /* now */ Timestamp(59),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = WALKING_START,  id = 0, status = Status.NEW)
          ),
          /* expected */
          listOf(UserActivity(WALKING_START, start = Timestamp( 0), durationSecs = 59))
        ),

        // Cases when the input contains 2 Events
        // ===========================================
        //
        //   case 4: (SS, x) -> (x), when x == Short ON_BICYCLE_START
        Arguments.of(
          /* caseNumber */ 4,
          /* shortLimit */ 60,
          /* now */ Timestamp(59),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = STILL_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 59), type = ON_BICYCLE_START,  id = 1, status = Status.NEW)
          ),
          /* expected */
          listOf(UserActivity(ON_BICYCLE_START, start = Timestamp( 59), durationSecs = 0))
        ),
        //   case 5: (x, SS) -> (x + SS), when x in { SA, LA }
        Arguments.of(
          /* caseNumber */ 5,
          /* shortLimit */ 60,
          /* now */ Timestamp(159),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 5), type = WALKING_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 100), type = STILL_START,  id = 1, status = Status.NEW)
          ),
          /* expected */
          listOf(UserActivity(WALKING_START, start = Timestamp( 5), durationSecs = 154))
        ),
        //   case 6: (x, y) -> (x, y), when x != y AND x,y in { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 6,
          /* shortLimit */ 60,
          /* now */ Timestamp(121),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = STILL_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 60), type = RUNNING_START,  id = 1, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(STILL_START, start = Timestamp( 0), durationSecs = 60),
            UserActivity(RUNNING_START, start = Timestamp( 60), durationSecs = 61)
          )
        ),

        // Cases when the input contains 3 Events
        // ============================================
        //
        //   case 7: (SS, x, y) -> (x, y), when x,y in { SA, LA }
        Arguments.of(
          /* caseNumber */ 7,
          /* shortLimit */ 60,
          /* now */ Timestamp(60),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = STILL_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 0), type = ON_FOOT_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 0), type = STILL_START,  id = 2, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT_START, start = Timestamp( 0), durationSecs = 0),
            UserActivity(STILL_START, start = Timestamp( 0), durationSecs = 60)
          )
        ),
        //
        //   case 8: (x, SS, y) -> (x + SS, y), when x != y, AND x in { SA, LA }, y in { SA, LA }
        Arguments.of(
          /* caseNumber */ 8,
          /* shortLimit */ 60,
          /* now */ Timestamp(60),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 0), type = ON_FOOT_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 1), type = STILL_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 60), type = ON_BICYCLE_START,  id = 2, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT_START, start = Timestamp( 0), durationSecs = 60),
            UserActivity(ON_BICYCLE_START, start = Timestamp( 60), durationSecs = 0)
          )
        ),
        //   case 9: (x1, SS, x2) -> (x1 + SS + x2), when x1 == x2, AND x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 9,
          /* shortLimit */ 30,
          /* now */ Timestamp(140),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 100), type = ON_FOOT_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 101), type = STILL_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 130), type = ON_FOOT_START,  id = 2, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT_START, start = Timestamp( 100), durationSecs = 40)
          )
        ),
        //   case 10: (x, y, SS) -> (x, y + SS), when y != x AND x in { LS, SA, LA }, in { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 10,
          /* shortLimit */ 60,
          /* now */ Timestamp(71),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = ON_FOOT_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 69), type = RUNNING_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 70), type = STILL_START,  id = 2, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT_START, start = Timestamp( 10), durationSecs = 59),
            UserActivity(RUNNING_START, start = Timestamp( 69), durationSecs = 2)
          )
        ),
        //   case 11: (x, y, z) -> (x, y, z), when x != y AND y != z AND x,y,z = { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 11,
          /* shortLimit */ 60,
          /* now */ Timestamp(180),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = ON_FOOT_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 70), type = RUNNING_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 80), type = IN_VEHICLE_START,  id = 2, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT_START, start = Timestamp( 10), durationSecs = 60),
            UserActivity(RUNNING_START, start = Timestamp( 70), durationSecs = 10),
            UserActivity(IN_VEHICLE_START, start = Timestamp( 80), durationSecs = 100)
          )
        ),

        // Cases when the input contains 3 Events
        // ============================================
        //
        //   case 12: (SS1, x1, SS2, x2) -> (x1 + SS2 + x2), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 12,
          /* shortLimit */ 30,
          /* now */ Timestamp(139),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = STILL_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 39), type = RUNNING_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 40), type = STILL_START,  id = 2, status = Status.NEW),
            Event.Impl(time = Timestamp( 69), type = RUNNING_START,  id = 3, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(RUNNING_START, start = Timestamp( 39), durationSecs = 100)
          )
        ),
        //   case 13: (x1, SS1, x2, SS2) -> (x1 + SS1 + x2 + SS2), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 13,
          /* shortLimit */ 30,
          /* now */ Timestamp(89),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = WALKING_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 30), type = STILL_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 59), type = WALKING_START,  id = 2, status = Status.NEW),
            Event.Impl(time = Timestamp( 60), type = STILL_START,  id = 3, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(WALKING_START, start = Timestamp( 10), durationSecs = 79)
          )
        ),
        //   case 14: (x1, LS, x2, SS) -> (x1, LS, x2 + SS), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 14,
          /* shortLimit */ 30,
          /* now */ Timestamp(89),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = RUNNING_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 20), type = STILL_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 50), type = RUNNING_START,  id = 2, status = Status.NEW),
            Event.Impl(time = Timestamp( 79), type = STILL_START,  id = 3, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(RUNNING_START, start = Timestamp( 10), durationSecs = 10),
            UserActivity(STILL_START, start = Timestamp( 20), durationSecs = 30),
            UserActivity(RUNNING_START, start = Timestamp( 50), durationSecs = 39)
          )
        ),
        //   case 15: (x1, SS, x2, LS) -> (x1 + SS + x2, LS), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 14,
          /* shortLimit */ 30,
          /* now */ Timestamp(109),
          /* input */
          listOf(
            Event.Impl(time = Timestamp( 10), type = IN_VEHICLE_START,  id = 0, status = Status.NEW),
            Event.Impl(time = Timestamp( 20), type = STILL_START,  id = 1, status = Status.NEW),
            Event.Impl(time = Timestamp( 49), type = IN_VEHICLE_START,  id = 2, status = Status.NEW),
            Event.Impl(time = Timestamp( 79), type = STILL_START,  id = 3, status = Status.NEW)
          ),
          /* expected */
          listOf(
            UserActivity(IN_VEHICLE_START, start = Timestamp( 10), durationSecs = 69),
            UserActivity(STILL_START, start = Timestamp( 79), durationSecs = 30)
          )
        )
      )
    }

  }

  @ParameterizedTest
  @MethodSource("argumentsForfilterShortStillActivitiesTest")
  fun `test filterShortStillActivities function`(
    // caseNumber is used to set a conditional breakpoint when debugging a particular case
    caseNumber: Int,
    shortLimit: Long,
    now: Timestamp,
    events: List<Event>,
    activities: List<UserActivity>
  ) {

    // Arrange

    // Act
    val result = EventRepository.filterShortStillActivities(
      shortLimit = shortLimit,
      now = now,
      events = events
    )

    // Assert
    assertEquals(activities, result)
  }


  // ====== Utility functions ======

  // Not used for the time being

  private val zoneId = ZoneId.of("America/Los_Angeles")

  private val nowStartOfDay = ZonedDateTime.of(
    2019,
    7,
    20,
    0,
    0,
    0,
    0,
    zoneId
  )

  private val nowStartOfNextDay = ZonedDateTime.of(
    2019,
    7,
    21,
    0,
    0,
    0,
    0,
    zoneId
  )

  private fun List<Event>.createRepositoryFromList(): EventRepository {

    return createRepository {
      val query = mockk<Query<Event>>().also { every { it.executeAsList() } returns this }
      every {
        it.queries.selectRange(
          startInclusive = nowStartOfDay.timestamp,
          endExclusive = nowStartOfNextDay.timestamp
        )
      } returns query
    }
  }

  private fun createRepository(init: ((activityDb: LocalDb) -> Unit)?): EventRepository {
    val remoteDb = mockk<RemoteDb>()
    val db = mockk<LocalDb>().also {
      val queries = mockk<LocaldbQueries>()

      // Configure standard mock behaviors
      every { it.queries } returns queries
      every { queries.insert(any(), any()) } just Runs

      // Configure test specific behavior
      if (init != null) {
        init(it)
      }
    }
    return EventRepository(db, remoteDb)
  }

}