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
    val myAlarmManager: MyAlarmManager,
    val myVibrationManager: MyVibrationManager,
    val myNotificationManager: MyNotificationManager
  )

  @Test
  fun `Ignore null input list, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createActivityRepository(latestActivity = null)
    val (
      transitionProcessor,
      myAlarmManager,
      myVibrationManager,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.receiveTransition(null)

    // Assert
    verify {
      listOf(myVibrationManager, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
    verify(exactly = 1) {
      myAlarmManager.createNextAlarm(TransitionProcessor.ALARM_INTERVAL)
    }
  }

  @Test
  fun `Ignore empty input list, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createActivityRepository(latestActivity = null)
    val (
      transitionProcessor,
      myAlarmManager,
      myVibrationManager,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.receiveTransition(ActivityTransitionResult(listOf<ActivityTransitionEvent>()))

    // Assert
    verify {
      listOf(myVibrationManager, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
    verify(exactly = 1) {
      myAlarmManager.createNextAlarm(TransitionProcessor.ALARM_INTERVAL)
    }
  }

  @Test
  fun `Ignore exit transition, and schedule the next alarm`() {

    // Arrange
    val emptyRepository = createActivityRepository(latestActivity = null)
    val (
      transitionProcessor,
      myAlarmManager,
      myVibrationManager,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.receiveTransition(
      ActivityTransitionResult(
        listOf(
          ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT, 1)
        )
      )
    )

    // Assert
    verify {
      listOf(myVibrationManager, myNotificationManager) wasNot Called
    }
    verify(exactly = 0) {
      emptyRepository.insert(any(), any())
    }
    verify(exactly = 1) {
      myAlarmManager.createNextAlarm(TransitionProcessor.ALARM_INTERVAL)
    }
  }

  @Test
  fun `Insert the first ENTER transition, and schedule the next alaram`() {

    // Arrange
    val emptyRepository = createActivityRepository(latestActivity = null)
    val (
      transitionProcessor,
      myAlarmManager,
      myVibrationManager,
      myNotificationManager
    ) = createDependencies(emptyRepository)

    // Act
    transitionProcessor.receiveTransition(
      ActivityTransitionResult(
        listOf(
          ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER, 1)
        )
      )
    )

    // Assert
    verify(exactly = 1) {
      emptyRepository.insert(ActivityType.STILL, DetectedActivity.STILL)
      myAlarmManager.createNextAlarm(TransitionProcessor.ALARM_INTERVAL)
      myVibrationManager.vibrate(TransitionProcessor.INFORMATION_VIBRATION_LENGTH)
      myNotificationManager.sendCurrentActivityNotification(ActivityType.STILL)
    }
  }

  // ============================ Only Utility functions below ============================

  private fun createDependencies(
    activityRepository: ActivityRepository,
    myAlarmManager: MyAlarmManager = myAlarmManager(),
    myVibrationManager: MyVibrationManager = myVibrationManager(),
    myNotificationManager: MyNotificationManager = myNotificationManager()
  ): Dependencies {
    val tp = TransitionProcessor(activityRepository, myAlarmManager, myVibrationManager, myNotificationManager)
    return Dependencies(tp, myAlarmManager, myVibrationManager, myNotificationManager)
  }

  private fun createActivityRepository(latestActivity:UserActivity?): ActivityRepository {
    return mockk<ActivityRepository>().apply {
      every { selectLatestActivity() } returns latestActivity
      every { insert(any(), any()) } just Runs
    }
  }

  private fun myAlarmManager(): MyAlarmManager {
    return mockk<MyAlarmManager>().apply {
      every { createNextAlarm(any()) } just Runs
    }
  }

  private fun myVibrationManager(): MyVibrationManager {
    return mockk<MyVibrationManager>().apply {
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