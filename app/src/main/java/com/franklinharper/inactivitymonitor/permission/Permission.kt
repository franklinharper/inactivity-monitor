package com.franklinharper.inactivitymonitor.permission

import android.Manifest
import androidx.annotation.StringRes
import com.franklinharper.inactivitymonitor.R


/**
 * All "dangerous" permissions that we need to ask user for when targeting SDK 23+.

 * @see [](https://docs.google.com/document/d/1wFRlCJ0cM7dy5VNXXEDke7KkAH0GQg15IaSijMAa9TA/edit?ts=57ed7f72.></a>
) */
enum class Permission constructor(
  /**
   * Request code permission, similar to activity request code, must be unique
   */
  val requestCode: Int,
  /**
   * The permission string from Android manifest. We can’t use `name` because it is an enum, and we
   * considered alternatives (e.g. `id`), but didn't find anything better, so we decided to stick
   * with permission. https://ziprecruiter.slack.com/archives/C950J84FQ/p1580766797125200
   */
  val permission: String,
  /**
   * The rationale message to explain why the app need this permission.
   */
  @param:StringRes val rationale: Int
) {
  ACTIVITY_RECOGNITION(
    778,
    Manifest.permission.ACTIVITY_RECOGNITION,
    R.string.permission_rationale_activity
  )
}
