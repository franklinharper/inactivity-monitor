package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.EventType.START_STILL
import com.franklinharper.inactivitymonitor.EventType.START_WALKING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventRepositoryFilterShortStillActivities {

  @Test
  fun `Given an empty list, then the result is an empty list`() {

    // Arrange

    // Act
    val result = EventRepository.filterShortStillActivities(
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
    val result = EventRepository.filterShortStillActivities(
      shortLimit = 60,
      now = Timestamp(59),
      events = listOf(
        Event.Impl(time = Timestamp(0), type = START_STILL, id = 1, status = Status.NEW)
      )
    )

    // Assert
    assertEquals(emptyList<UserActivity>(), result)
  }

  @Test
  fun `STILL Activities that last longer than or equal to "shortLimit" are NOT filtered`() {

    // Arrange

    // Act
    val actual = EventRepository.filterShortStillActivities(
      shortLimit = 60,
      now = Timestamp(61),
      events = listOf(
        Event.Impl(id = 1, time = Timestamp(0), type = START_STILL, status = Status.NEW),
        Event.Impl(id = 2, time = Timestamp(60), type = START_WALKING, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(START_STILL, start = Timestamp(0), duration = 60),
      UserActivity(START_WALKING, start = Timestamp(60), duration = 1)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `When an Activity is STILL and short it IS filtered`() {

    // Arrange

    // Act
    val actual = EventRepository.filterShortStillActivities(
      shortLimit = 60,
      now = Timestamp(120),
      events = listOf(
        Event.Impl(id = 1, time = Timestamp(0), type = START_STILL, status = Status.NEW),
        Event.Impl(id = 2, time = Timestamp(59), type = START_WALKING, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(START_WALKING, start = Timestamp(59), duration = 61)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `Filter activities that are of type STILL and duration less than shortLimit`() {

    // Arrange

    // Act
    val result = EventRepository.filterShortStillActivities(
      now = Timestamp(120),
      shortLimit = 60,
      events = listOf(
        Event.Impl(time = Timestamp(0), type = START_STILL, id = 1, status = Status.NEW),
        Event.Impl(time = Timestamp(59), type = START_WALKING, id = 2, status = Status.NEW),
        Event.Impl(time = Timestamp(118), type = START_STILL, id = 3, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(START_WALKING, start = Timestamp(59), duration = 61)
    )

    assertEquals(expected, result)
  }

  @Test
  fun `Do NOT filter STILL Activities when their duration is greater than shortLimit`() {

    // Arrange

    // Act
    val result = EventRepository.filterShortStillActivities(
      now = Timestamp(62),
      shortLimit = 60,
      events = listOf(
        Event.Impl(time = Timestamp(0), type = START_STILL, id = 1, status = Status.NEW),
        Event.Impl(time = Timestamp(1), type = START_WALKING, id = 2, status = Status.NEW),
        Event.Impl(time = Timestamp(2), type = START_STILL, id = 3, status = Status.NEW)
      )
    )

    // Assert
    val expected = listOf(
      UserActivity(START_WALKING, start = Timestamp(1), duration = 1),
      UserActivity(START_STILL, start = Timestamp(2), duration = 60)
    )

    assertEquals(expected, result)
  }
}