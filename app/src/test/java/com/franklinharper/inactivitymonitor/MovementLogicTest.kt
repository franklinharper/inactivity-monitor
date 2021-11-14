package com.franklinharper.inactivitymonitor

import com.franklinharper.inactivitymonitor.testfakes.AppSettingsFake
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MovementLogicTest {

  @Test
  fun `test movement logic`() {
    val appSettingsFake = AppSettingsFake(maxStillMinutes = 1)
    val movementLogic = MovementLogic(appSettingsFake)

    // These tests have never been run!
    // TODO fix broken tests
    // None of the test can be run, the error message is "No tests found for given includes..."
    // 1) find commit that broke all the tests
    // 2) fix!
    assertThat(
      movementLogic.userStillForTooLong(
        UserMovement(
          type = MovementType.STILL_START,
          start = Timestamp(epochSecond = 0),
          durationSecs = 0
        )
      )
    )
      .isFalse()

    assertThat(
      movementLogic.userStillForTooLong(
        UserMovement(
          type = MovementType.STILL_START,
          start = Timestamp(epochSecond = 0),
          durationSecs = 60
        )
      )
    )
      .isFalse()

    assertThat(
      movementLogic.userStillForTooLong(
        UserMovement(
          type = MovementType.STILL_START,
          start = Timestamp(epochSecond = 0),
          durationSecs = 61
        )
      )
    )
      .isTrue()

    assertThat(
      movementLogic.userStillForTooLong(
        UserMovement(
          type = MovementType.WALKING_START,
          start = Timestamp(epochSecond = 0),
          durationSecs = 61
        )
      )
    )
      .isFalse()
  }
}