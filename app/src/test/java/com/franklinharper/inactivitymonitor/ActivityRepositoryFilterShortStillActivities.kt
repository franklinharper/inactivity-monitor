package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.ActivityType.STILL
import com.franklinharper.inactivitymonitor.ActivityType.WALKING
import com.franklinharper.inactivitymonitor.TransitionType.ENTER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActivityRepositoryFilterShortStillActivities {

  // TODO write a parameterized test that has
  //   input: List<Transition>
  //   expected output: List<UserActivity>
  //   See https://blog.codefx.org/libraries/junit-5-parameterized-tests/ for implementation details

  @Test
  fun `Given an empty list, then the result is an empty list`() {

    // Arrange

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      shortLimit = 60,
      now = 0L,
      transitions = emptyList()
    )

    // Assert
    assertEquals(emptyList<Transition>(), result)
  }

  @Test
  fun `Given a list that contains only a short STILL Activity, then the result is an empty list`() {

    // Arrange

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      shortLimit = 60,
      now = 59,
      transitions = listOf(
        Transition.Impl(time = 0, activity_type = STILL, transition_type = ENTER, id = 1)
      )
    )

    // Assert
    assertEquals(emptyList<UserActivity>(), result)
  }

  @Test
  fun `STILL Activities that last longer than or equal to "shortLimit" are NOT filtered`() {

    // Arrange

    // Act
    val actual = ActivityRepository.filterShortStillActivities(
      shortLimit = 60,
      now = 61L,
      transitions = listOf(
        Transition.Impl(id = 1, time = 0, activity_type = STILL, transition_type = ENTER),
        Transition.Impl(id = 2, time = 60, activity_type = WALKING, transition_type = ENTER)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(STILL, start = 0, duration = 60),
      UserActivity(WALKING, start = 60, duration = 1)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `When an Activity is STILL and short it IS filtered`() {

    // Arrange

    // Act
    val actual = ActivityRepository.filterShortStillActivities(
      shortLimit = 60,
      now = 120L,
      transitions = listOf(
        Transition.Impl(id = 1, time = 0, activity_type = STILL, transition_type = ENTER),
        Transition.Impl(id = 2, time = 59, activity_type = WALKING, transition_type = ENTER)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(WALKING, start = 0, duration = 120)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `Filter activities that are of type STILL and duration less than shortLimit`() {

    // Arrange

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      now = 120,
      shortLimit = 60,
      transitions = listOf(
        // The Transitions must be sorted by "id" in ascending order.
        // Except for extreme edge cases, the transition "time" is monotonically ascending
        Transition.Impl(id = 1, time = 0, activity_type = STILL, transition_type = ENTER),
        Transition.Impl(id = 2, time = 59, activity_type = WALKING, transition_type = ENTER),
        Transition.Impl(id = 3, time = 118, activity_type = STILL, transition_type = ENTER)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(WALKING, start = 0, duration = 120)
    )

    assertEquals(expected, result)
  }

  @Test
  fun `Do NOT filter STILL Activities when their duration is greater than shortLimit`() {

    // Arrange

    // Act
    val result = ActivityRepository.filterShortStillActivities(
      now = 62,
      shortLimit = 60,
      transitions = listOf(
        // The Transitions must be sorted by "id" in ascending order.
        // Except for extreme edge cases, the transition "time" is monotonically ascending
        Transition.Impl(id = 1, time = 0, activity_type = STILL, transition_type = ENTER),
        Transition.Impl(id = 2, time = 1, activity_type = WALKING, transition_type = ENTER),
        Transition.Impl(id = 3, time = 2, activity_type = STILL, transition_type = ENTER)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(WALKING, start = 0, duration = 2),
      UserActivity(STILL, start = 2, duration = 60)
    )

    assertEquals(expected, result)
  }
}