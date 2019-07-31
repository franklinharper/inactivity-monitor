package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.ActivityType.*
import com.franklinharper.inactivitymonitor.TransitionType.ENTER
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

class ActivityRepositoryTest {

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
      // Transitions must be in ascending chronological order
      // => transition[n].time <= transition[n+1].time
      //
      // The activity_type of a transition must be different from the activity type of the previous transition
      // => transition[n].activity_type != transition[n+1].activity_type
      //
      // Test cases
      // ==========
      //
      // Empty Input
      // ===========
      //   case 1: () -> ))
      //
      //   Input contains 1 Transition
      //   case 2: (SS) -> ()
      //   case 3: (x) -> (x) when x in { LS, SA, LA }
      //
      // Input contains 2 Transitions
      // ============================
      //
      //   Short Still cases
      //   case 4: (SS, x) -> (x), when x in { SA, LA }
      //   case 5: (x, SS) -> (x + SS), when x in { SA, LA }
      //
      //   General case
      //   case 6: (x, y) -> (x, y), when x != y AND x,y in { LS, SA, LA }
      //
      // Input contains 3 Transitions
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
      // Input contains 4 Transitions
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
          /* now */ 0,
          /* input */
          emptyList<Transition.Impl>(),
          /* expected */
          emptyList<UserActivity>()
        ),

        // Cases when the input contains 1 Transition
        // ==========================================
        //
        // case 2: (SS) -> ()
        Arguments.of(
          /* caseNumber */ 2,
          /* shortLimit */ 60,
          /* now */ 59,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 0)
          ),
          /* expected */
          emptyList<UserActivity>()
        ),

        // case 3: (x) -> (x) when x == SA
        Arguments.of(
          /* caseNumber */ 3,
          /* shortLimit */ 60,
          /* now */ 59,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = WALKING, transition_type = ENTER, id = 0)
          ),
          /* expected */
          listOf(UserActivity(WALKING, start = 0, duration = 59))
        ),

        // Cases when the input contains 2 Transitions
        // ===========================================
        //
        //   case 4: (SS, x) -> (x), when x == Short ON_BICYCLE
        Arguments.of(
          /* caseNumber */ 4,
          /* shortLimit */ 60,
          /* now */ 59,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 0),
            Transition.Impl(time = 59, activity_type = ON_BICYCLE, transition_type = ENTER, id = 1)
          ),
          /* expected */
          listOf(UserActivity(ON_BICYCLE, start = 59, duration = 0))
        ),
        //   case 5: (x, SS) -> (x + SS), when x in { SA, LA }
        Arguments.of(
          /* caseNumber */ 5,
          /* shortLimit */ 60,
          /* now */ 159,
          /* input */
          listOf(
            Transition.Impl(time = 5, activity_type = WALKING, transition_type = ENTER, id = 0),
            Transition.Impl(time = 100, activity_type = STILL, transition_type = ENTER, id = 1)
          ),
          /* expected */
          listOf(UserActivity(WALKING, start = 5, duration = 154))
        ),
        //   case 6: (x, y) -> (x, y), when x != y AND x,y in { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 6,
          /* shortLimit */ 60,
          /* now */ 121,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 0),
            Transition.Impl(time = 60, activity_type = RUNNING, transition_type = ENTER, id = 1)
          ),
          /* expected */
          listOf(
            UserActivity(STILL, start = 0, duration = 60),
            UserActivity(RUNNING, start = 60, duration = 61)
          )
        ),

        // Cases when the input contains 3 Transitions
        // ============================================
        //
        //   case 7: (SS, x, y) -> (x, y), when x,y in { SA, LA }
        Arguments.of(
          /* caseNumber */ 7,
          /* shortLimit */ 60,
          /* now */ 60,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 0),
            Transition.Impl(time = 0, activity_type = ON_FOOT, transition_type = ENTER, id = 1),
            Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 2)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT, start = 0, duration = 0),
            UserActivity(STILL, start = 0, duration = 60)
          )
        ),
        //
        //   case 8: (x, SS, y) -> (x + SS, y), when x != y, AND x in { SA, LA }, y in { SA, LA }
        Arguments.of(
          /* caseNumber */ 8,
          /* shortLimit */ 60,
          /* now */ 60,
          /* input */
          listOf(
            Transition.Impl(time = 0, activity_type = ON_FOOT, transition_type = ENTER, id = 0),
            Transition.Impl(time = 1, activity_type = STILL, transition_type = ENTER, id = 1),
            Transition.Impl(time = 60, activity_type = ON_BICYCLE, transition_type = ENTER, id = 2)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT, start = 0, duration = 60),
            UserActivity(ON_BICYCLE, start = 60, duration = 0)
          )
        ),
        //   case 9: (x1, SS, x2) -> (x1 + SS + x2), when x1 == x2, AND x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 9,
          /* shortLimit */ 30,
          /* now */ 140,
          /* input */
          listOf(
            Transition.Impl(time = 100, activity_type = ON_FOOT, transition_type = ENTER, id = 0),
            Transition.Impl(time = 101, activity_type = STILL, transition_type = ENTER, id = 1),
            Transition.Impl(time = 130, activity_type = ON_FOOT, transition_type = ENTER, id = 2)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT, start = 100, duration = 40)
          )
        ),
        //   case 10: (x, y, SS) -> (x, y + SS), when y != x AND x in { LS, SA, LA }, in { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 10,
          /* shortLimit */ 60,
          /* now */ 71,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = ON_FOOT, transition_type = ENTER, id = 0),
            Transition.Impl(time = 69, activity_type = RUNNING, transition_type = ENTER, id = 1),
            Transition.Impl(time = 70, activity_type = STILL, transition_type = ENTER, id = 2)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT, start = 10, duration = 59),
            UserActivity(RUNNING, start = 69, duration = 2)
          )
        ),
        //   case 11: (x, y, z) -> (x, y, z), when x != y AND y != z AND x,y,z = { LS, SA, LA }
        Arguments.of(
          /* caseNumber */ 11,
          /* shortLimit */ 60,
          /* now */ 180,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = ON_FOOT, transition_type = ENTER, id = 0),
            Transition.Impl(time = 70, activity_type = RUNNING, transition_type = ENTER, id = 1),
            Transition.Impl(time = 80, activity_type = IN_VEHICLE, transition_type = ENTER, id = 2)
          ),
          /* expected */
          listOf(
            UserActivity(ON_FOOT, start = 10, duration = 60),
            UserActivity(RUNNING, start = 70, duration = 10),
            UserActivity(IN_VEHICLE, start = 80, duration = 100)
          )
        ),

        // Cases when the input contains 3 Transitions
        // ============================================
        //
        //   case 12: (SS1, x1, SS2, x2) -> (x1 + SS2 + x2), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 12,
          /* shortLimit */ 30,
          /* now */ 139,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = STILL, transition_type = ENTER, id = 0),
            Transition.Impl(time = 39, activity_type = RUNNING, transition_type = ENTER, id = 1),
            Transition.Impl(time = 40, activity_type = STILL, transition_type = ENTER, id = 2),
            Transition.Impl(time = 69, activity_type = RUNNING, transition_type = ENTER, id = 3)
          ),
          /* expected */
          listOf(
            UserActivity(RUNNING, start = 39, duration = 100)
          )
        ),
        //   case 13: (x1, SS1, x2, SS2) -> (x1 + SS1 + x2 + SS2), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 13,
          /* shortLimit */ 30,
          /* now */ 89,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = WALKING, transition_type = ENTER, id = 0),
            Transition.Impl(time = 30, activity_type = STILL, transition_type = ENTER, id = 1),
            Transition.Impl(time = 59, activity_type = WALKING, transition_type = ENTER, id = 2),
            Transition.Impl(time = 60, activity_type = STILL, transition_type = ENTER, id = 3)
          ),
          /* expected */
          listOf(
            UserActivity(WALKING, start = 10, duration = 79)
          )
        ),
        //   case 14: (x1, LS, x2, SS) -> (x1, LS, x2 + SS), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 14,
          /* shortLimit */ 30,
          /* now */ 89,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = RUNNING, transition_type = ENTER, id = 0),
            Transition.Impl(time = 20, activity_type = STILL, transition_type = ENTER, id = 1),
            Transition.Impl(time = 50, activity_type = RUNNING, transition_type = ENTER, id = 2),
            Transition.Impl(time = 79, activity_type = STILL, transition_type = ENTER, id = 3)
          ),
          /* expected */
          listOf(
            UserActivity(RUNNING, start = 10, duration = 10),
            UserActivity(STILL, start = 20, duration = 30),
            UserActivity(RUNNING, start = 50, duration = 39)
          )
        ),
        //   case 15: (x1, SS, x2, LS) -> (x1 + SS + x2, LS), when x1, x2 in { SA, LA }
        Arguments.of(
          /* caseNumber */ 14,
          /* shortLimit */ 30,
          /* now */ 109,
          /* input */
          listOf(
            Transition.Impl(time = 10, activity_type = IN_VEHICLE, transition_type = ENTER, id = 0),
            Transition.Impl(time = 20, activity_type = STILL, transition_type = ENTER, id = 1),
            Transition.Impl(time = 49, activity_type = IN_VEHICLE, transition_type = ENTER, id = 2),
            Transition.Impl(time = 79, activity_type = STILL, transition_type = ENTER, id = 3)
          ),
          /* expected */
          listOf(
            UserActivity(IN_VEHICLE, start = 10, duration = 69),
            UserActivity(STILL, start = 79, duration = 30)
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
    now: Long,
    transitions: List<Transition>,
    activities: List<UserActivity>
  ) {

    // Arrange

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      shortLimit = shortLimit,
      now = now,
      transitions = transitions
    )

    // Assert
    assertEquals(activities, result)
  }


  // ====== Utility functions ======

  // Not used for the time being

  private val zoneId = ZoneId.of("America/Los_Angeles")
  private val now = ZonedDateTime.of(
    2019,
    7,
    20,
    8,
    23,
    33,
    0,
    zoneId
  )
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

  private fun List<Transition>.createRepositoryFromList(): ActivityRepository {

    return createRepository {
      val query = mockk<Query<Transition>>().also { every { it.executeAsList() } returns this }
      every {
        it.queries.selectRange(
          startInclusive = nowStartOfDay.toEpochSecond(),
          endExclusive = nowStartOfNextDay.toEpochSecond()
        )
      } returns query
    }
  }

  private fun createRepository(init: ((activityDb: ActivityDb) -> Unit)?): ActivityRepository {
    val db = mockk<ActivityDb>().also {
      val queries = mockk<TransitionQueries>()

      // Configure standard mock behaviors
      every { it.queries } returns queries
      every { queries.insert(any(), any()) } just Runs

      // Configure test specific behavior
      if (init != null) {
        init(it)
      }
    }
    return ActivityRepository(db)
  }

}