package com.franklinharper.inactivitymonitor

import io.mockk.*
import org.junit.jupiter.api.Test

internal class ReminderTest {

  data class Dependencies(
    val reminder: Reminder,
    val alarmScheduler: AlarmScheduler,
    val appVibrations: AppVibrations,
    val notificationSender: NotificationSender
  )

  @Test
  fun `Ignore null input list`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val dependencies = createDependencies(emptyRepository)

    // Act
    dependencies.reminder.update()

    // Assert
    verify {
      listOf(dependencies.appVibrations, dependencies.notificationSender) wasNot Called
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
      emptyRepository.insert(MovementType.WALKING_START, Status.NEW)
    }
  }

  // ============================ Only Utility functions below ============================

  private fun createDependencies(
    eventRepository: EventRepository,
    alarmScheduler: AlarmScheduler = mockAlarmManager(),
    appVibrations: AppVibrations = myVibrator(),
    notificationSender: NotificationSender = mockNotificationSender(),

    ): Dependencies {
    val reminder = Reminder(
      eventRepository = eventRepository,
      appVibrator = appVibrations,
      notificationSender = notificationSender,
      snooze = mockk(),
      appSettings = mockk(),
      phoneCall = mockk(),
      movementLogic = mockk()
    )
    return Dependencies(reminder, alarmScheduler, appVibrations, notificationSender)
  }

  private fun createEventRepository(
    mostRecentMovement: UserMovement = UserMovement(
      MovementType.STILL_START,
      Timestamp(0),
      0
    )
  ): EventRepository {
    return mockk<EventRepository>().apply {
      every { mostRecentMovement(any()) } returns mostRecentMovement
      every { insert(any(), any()) } just Runs
    }
  }

  private fun mockAlarmManager(): AlarmScheduler {
    return mockk<AlarmScheduler>().apply {
      every { update() } just Runs
    }
  }

  private fun myVibrator() = mockk<AppVibrations>()
//    return mockk<AppVibrator>().apply {
//      every { vibrate(any()) } just Runs
//    }
//  }

  private fun mockNotificationSender() = mockk<NotificationSender>()
}