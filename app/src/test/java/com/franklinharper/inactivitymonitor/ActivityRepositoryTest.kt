package com.franklinharper.inactivitymonitor

import com.squareup.sqldelight.Query
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

internal class ActivityRepositoryTest {

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

  @Test
  fun `The List of todaysActivities is empty when the repository is empty`() {

    // Arrange
    val activities = emptyList<Transition>()

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      shortLimit = 60,
      now = now.toEpochSecond(),
      transitions = activities
    )

    // Assert
    assertEquals(emptyList<Transition>(), result)
  }

  // ====== Utility functions ======

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