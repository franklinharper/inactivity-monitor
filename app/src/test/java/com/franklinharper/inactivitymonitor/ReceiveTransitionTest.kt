package com.franklinharper.inactivitymonitor

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import io.mockk.*
import org.junit.jupiter.api.Test

internal class ReceiveTransitionTest {

  data class Dependencies(
    val transitionProcessor: TransitionProcessor,
    val alarmScheduler: AlarmScheduler,
    val myVibrator: MyVibrator,
    val myNotificationManager: MyNotificationManager
  )

  @Test
  fun `Ignore null input list, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository(latestActivity = null)
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
    verify(exactly = 1) {
      myAlarmManager.replacePreviousAlarm(TransitionProcessor.DEFAULT_MAX_WAIT_SECS)
    }
  }

  @Test
  fun `Ignore empty input list, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository(latestActivity = null)
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
    verify(exactly = 1) {
      myAlarmManager.replacePreviousAlarm(TransitionProcessor.DEFAULT_MAX_WAIT_SECS)
    }
  }

  @Test
  fun `Ignore exit transition, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository(latestActivity = null)
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
          ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT, 1)
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
    verify(exactly = 1) {
      myAlarmManager.replacePreviousAlarm(TransitionProcessor.DEFAULT_MAX_WAIT_SECS)
    }
  }

  @Test
  fun `Insert the first ENTER transition, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createEventRepository(latestActivity = null)
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
          ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER, 1)
        )
      )
    )

    // Assert
    verify(exactly = 1) {
      emptyRepository.insert(EventType.STILL_START, Status.NEW)
      myAlarmManager.replacePreviousAlarm(TransitionProcessor.DEFAULT_MAX_WAIT_SECS)
      myVibrator.vibrate(TransitionProcessor.INFO_VIBRATION_MILLIS)
      myNotificationManager.sendCurrentActivityNotification(EventType.STILL_START)
    }
  }

  // ============================ Only Utility functions below ============================

  private fun createDependencies(
    activityRepository: EventRepository,
    alarmScheduler: AlarmScheduler = myAlarmManager(),
    myVibrator: MyVibrator = myVibrator(),
    myNotificationManager: MyNotificationManager = myNotificationManager()
  ): Dependencies {
    val tp = TransitionProcessor(activityRepository, alarmScheduler, myVibrator, myNotificationManager)
    return Dependencies(tp, alarmScheduler, myVibrator, myNotificationManager)
  }

  private fun createEventRepository(latestActivity:UserActivity?): EventRepository {
    return mockk<EventRepository>().apply {
      every { mostRecentActivity(any()) } returns latestActivity
      every { insert(any(), any()) } just Runs
    }
  }

  private fun myAlarmManager(): AlarmScheduler {
    return mockk<AlarmScheduler>().apply {
      every { replacePreviousAlarm(any()) } just Runs
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