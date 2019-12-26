package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import io.mockk.*
import org.junit.jupiter.api.Test

internal class TransitionProcessorTest {

  data class Dependencies(
    val transitionProcessor: TransitionProcessor,
    val alarmScheduler: AlarmScheduler,
    val myVibrator: MyVibrator,
    val myNotificationManager: MyNotificationManager
  )

  @Test
  fun `Ignore null input list`() {

    // Arrange
    val emptyRepository = createEventRepository()
    val (
      transitionProcessor,
      myAlarmManager,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.processTransitionResult(null)

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
      myAlarmManager,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.processTransitionResult(ActivityTransitionResult(listOf<ActivityTransitionEvent>()))

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
      myAlarmManager,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.processTransitionResult(
      ActivityTransitionResult(
        listOf(
          ActivityTransitionEvent(
            DetectedActivity.STILL,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT,
            1
          )
        )
      )
    )

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
      transitionProcessor,
      myAlarmManager,
      myVibrator,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.processTransitionResult(
      ActivityTransitionResult(
        listOf(
          ActivityTransitionEvent(
            DetectedActivity.WALKING,
            ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            1
          )
        )
      )
    )

    // Assert
    verify(exactly = 1) {
      emptyRepository.insert(EventType.WALKING_START, Status.NEW)
      myAlarmManager.removePreviousAlarm()
    }
  }

  // ============================ Only Utility functions below ============================

  private fun createDependencies(
    activityRepository: EventRepository,
    alarmScheduler: AlarmScheduler = myAlarmManager(),
    myVibrator: MyVibrator = myVibrator(),
    myNotificationManager: MyNotificationManager = myNotificationManager()
  ): Dependencies {
    val tp =
      TransitionProcessor(activityRepository, alarmScheduler, myVibrator, myNotificationManager)
    return Dependencies(tp, alarmScheduler, myVibrator, myNotificationManager)
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
      every { replacePreviousAlarm(any()) } just Runs
      every { removePreviousAlarm() } just Runs
    }
  }

  private fun myVibrator(): MyVibrator {
    return mockk<MyVibrator>().apply {
      every { vibrate(any()) } just Runs
    }
  }

  private fun myNotificationManager(): MyNotificationManager {
    return mockk<MyNotificationManager>().apply {
      every { doNotDisturbOff } returns true
      every { sendCurrentActivityNotification(any()) } just Runs
    }
  }
}