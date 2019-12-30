package com.franklinharper.inactivitymonitor

import io.mockk.*
import org.junit.jupiter.api.Test

internal class ReminderTest {

  data class Dependencies(
    val reminder: Reminder,
    val alarmScheduler: AlarmScheduler,
    val vibratorWrapper: VibratorWrapper,
    val notificationSender: NotificationSender
  )

  @Test
  fun `Ignore null input list`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val (
      transitionProcessor,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.update()

    // Assert
    verify {
      listOf(myVibrator, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
  }

  @Test
  fun `Ignore empty input list, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val (
      transitionProcessor,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.update()

    // Assert
    verify {
      listOf(myVibrator, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
  }

  @Test
  fun `Do not insert exit transition`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val (
      transitionProcessor,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.update()

    // Assert
    verify {
      listOf(myVibrator, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
  }

  @Test
  fun `Insert the first Event, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val (
      transitionProcessor
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.update()

    // Assert
    verify(exactly = 1) {
      emptyRepository.insert(EventType.WALKING_START, Status.NEW)
    }
  }

  // ============================ Only Utility functions below ============================

  private fun createDependencies(
    eventRepository: EventRepository,
    alarmScheduler: AlarmScheduler = myAlarmManager(),
    vibratorWrapper: VibratorWrapper = myVibrator(),
    notificationSender: NotificationSender = myNotificationManager()
  ): Dependencies {
    val tp =
      Reminder(eventRepository, vibratorWrapper, notificationSender)
    return Dependencies(tp, alarmScheduler, vibratorWrapper, notificationSender)
  }

  private fun createEventRepository(
    mostRecentActivity: UserActivity = UserActivity(
      EventType.STILL_START,
      Timestamp(0),
      0
    )
  ): EventRepository {
    return mockk<EventRepository>().apply {
      every { mostRecentActivity(any()) } returns mostRecentActivity
      every { insert(any(), any()) } just Runs
    }
  }

  private fun myAlarmManager(): AlarmScheduler {
    return mockk<AlarmScheduler>().apply {
      every { update() } just Runs
    }
  }

  private fun myVibrator(): VibratorWrapper {
    return mockk<VibratorWrapper>().apply {
      every { vibrate(any()) } just Runs
    }
  }

  private fun myNotificationManager(): NotificationSender {
    return mockk<NotificationSender>().apply {
      every { doNotDisturbOff } returns true
    }
  }
}