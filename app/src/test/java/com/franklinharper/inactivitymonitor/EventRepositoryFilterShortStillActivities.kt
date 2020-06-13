package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.MovementType.STILL_START
import com.franklinharper.inactivitymonitor.MovementType.WALKING_START
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventRepositoryFilterShortStillActivities {

  @Test
  fun `Given an empty list, then the result is an empty list`() {

    // Arrange

    // Act
    val result = DbEventRepository.filterShortStillMovements(
      shortLimit = 60,
      now = Timestamp(0),
      events = emptyList()
    )

    // Assert
    assertEquals(emptyList<Event>(), result)
  }

  @Test
  fun `Given a list that contains only a short STILL Activity, then the result is an empty list`() {

    // Arrange

    // Act
    val result = DbEventRepository.filterShortStillMovements(
      shortLimit = 60,
      now = Timestamp(59),
      events = listOf(
        Event.Impl(occurred = Timestamp(0), type = STILL_START, id = 1, status = Status.NEW)
      )
    )

    // Assert
    assertEquals(emptyList<UserMovement>(), result)
  }

  @Test
  fun `STILL Activities that last longer than or equal to "shortLimit" are NOT filtered`() {

    // Arrange

    // Act
    val actual = DbEventRepository.filterShortStillMovements(
      shortLimit = 60,
      now = Timestamp(61),
      events = listOf(
        Event.Impl(id = 1, occurred = Timestamp(0), type = STILL_START, status = Status.NEW),
        Event.Impl(id = 2, occurred = Timestamp(60), type = WALKING_START, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserMovement(STILL_START, start = Timestamp(0), durationSecs = 60),
      UserMovement(WALKING_START, start = Timestamp(60), durationSecs = 1)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `When an Activity is STILL and short it IS filtered`() {

    // Arrange

    // Act
    val actual = DbEventRepository.filterShortStillMovements(
      shortLimit = 60,
      now = Timestamp(120),
      events = listOf(
        Event.Impl(id = 1, occurred = Timestamp(0), type = STILL_START, status = Status.NEW),
        Event.Impl(id = 2, occurred = Timestamp(59), type = WALKING_START, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserMovement(WALKING_START, start = Timestamp(59), durationSecs = 61)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `Filter activities that are of type STILL and duration less than shortLimit`() {

    // Arrange

    // Act
    val result = DbEventRepository.filterShortStillMovements(
      now = Timestamp(120),
      shortLimit = 60,
      events = listOf(
        Event.Impl(occurred = Timestamp(0), type = STILL_START, id = 1, status = Status.NEW),
        Event.Impl(occurred = Timestamp(59), type = WALKING_START, id = 2, status = Status.NEW),
        Event.Impl(occurred = Timestamp(118), type = STILL_START, id = 3, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserMovement(WALKING_START, start = Timestamp(59), durationSecs = 61)
    )

    assertEquals(expected, result)
  }

  @Test
  fun `Do NOT filter STILL Activities when their duration is greater than shortLimit`() {

    // Arrange

    // Act
    val result = DbEventRepository.filterShortStillMovements(
      now = Timestamp(62),
      shortLimit = 60,
      events = listOf(
        Event.Impl(occurred = Timestamp(0), type = STILL_START, id = 1, status = Status.NEW),
        Event.Impl(occurred = Timestamp(1), type = WALKING_START, id = 2, status = Status.NEW),
        Event.Impl(occurred = Timestamp(2), type = STILL_START, id = 3, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserMovement(WALKING_START, start = Timestamp(1), durationSecs = 1),
      UserMovement(STILL_START, start = Timestamp(2), durationSecs = 60)
    )

    assertEquals(expected, result)
  }
}