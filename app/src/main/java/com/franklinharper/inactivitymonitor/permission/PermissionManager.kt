package com.franklinharper.inactivitymonitor.permission

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

interface PermissionListener {
  fun onGranted(permission: Permission)
  fun onDenied(permission: Permission)
}

enum class PermissionStatus {
  DENIED,
  GRANTED,
  NEVER_ASK_AGAIN
}

class PermissionManager(
  private val activity: Activity
) {

  @get:VisibleForTesting
  internal var listener: PermissionListener? = null
    private set

  /**
   * Return the current status of this permission

   * @param permission The permission we want to check
   * *
   * @return
   */
  fun checkStatus(permission: Permission): PermissionStatus {
    val granted = systemCheckSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    if (granted) {
      return PermissionStatus.GRANTED
    } else {
//      if (preferences.isNeverAskAgain(permission)) {
//        return PermissionStatus.NEVER_ASK_AGAIN
//      }
      return PermissionStatus.DENIED
    }
  }

  /**
   * Request a specific permission with a custom rationale message.

   * @param permission A specific permission we want to request
   * *
   * @param listener   Listener to react to user action, either granted or denied
   */
  @JvmOverloads
  fun request(
    permission: Permission,
    listener: PermissionListener,
    @StringRes rationale: Int = permission.rationale
  ) {
    this.listener = listener
    val flag = systemCheckSelfPermission(permission)
    if (flag != PackageManager.PERMISSION_GRANTED) {
      if (systemShouldShowRequestPermissionRationale(permission)) {
        showRationaleDialog(rationale,
          DialogInterface.OnClickListener { _, _ -> request(permission) },
          DialogInterface.OnClickListener { _, _ -> dispatchOnDenied(permission) }
        )
      } else {
        request(permission)
      }
    } else {
      // We don't want to call dispatchOnGranted() here because it could be lower target SDK
      // that always has permission granted.
      listener.onGranted(permission)
    }
  }

  /**
   * Request a specific permission. Note that, the rationale message to explain for user is defined
   * in [Permission], if we want to have custom message, use [.request]
   * instead.

   * @param permission The permission we want to request
   * *
   */
  internal fun request(permission: Permission) {
    systemRequestPermission(permission)
  }

  /**
   * Must be called in the activity which instantiated permission manager, otherwise [PermissionListener]
   * will not be called.

   * @param requestCode
   * *
   * @param permissions
   * *
   * @param grantResults
   */
  fun onRequestPermissionsResult(
    requestCode: Int,
    grantResults: IntArray
  ) {
    val permission = PERMISSION_MAP[requestCode]
    val granted = isGranted(grantResults)
    if (granted) {
      dispatchOnGranted(permission!!)
    } else {
      if (!systemShouldShowRequestPermissionRationale(permission!!)) {
//        preferences.setNeverAskAgain(permission)
      }
      dispatchOnDenied(permission)
    }
  }

  internal fun dispatchOnDenied(permission: Permission) {
    if (listener != null) {
      listener!!.onDenied(permission)
    }
  }

  internal fun dispatchOnGranted(permission: Permission) {
    if (listener != null) {
      listener!!.onGranted(permission)
    }
  }

  internal fun showRationaleDialog(
    @StringRes message: Int,
    okListener: DialogInterface.OnClickListener,
    cancelListener: DialogInterface.OnClickListener
  ) {
    if (!activity.isFinishing) {
      AlertDialog.Builder(activity)
        .setMessage(message)
        .setPositiveButton("OK", okListener)
        .setNegativeButton("Cancel", cancelListener)
        .create()
        .show()
    }
  }

  internal fun isGranted(results: IntArray): Boolean {
    return results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Use the android system checkSelfPermission mechanism. This wrapper allows for easier unit
   * testing.
   */
  @VisibleForTesting
  internal fun systemCheckSelfPermission(permission: Permission): Int {
    return ActivityCompat.checkSelfPermission(activity, permission.permission)
  }

  /**
   * Use the android system systemShouldShowRequestPermissionRationale mechanism. This wrapper
   * allows for easier unit testing.
   */
  @VisibleForTesting
  internal fun systemShouldShowRequestPermissionRationale(permission: Permission): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.permission)
  }

  /**
   *  Use the android system requestPermissions mechanism. This wrapper allows for easier unit
   *  testing.
   */
  @VisibleForTesting
  internal fun systemRequestPermission(permission: Permission) {
    ActivityCompat.requestPermissions(
      activity,
      arrayOf(permission.permission),
      permission.requestCode
    )
  }

  companion object {

    private val PERMISSION_MAP = mutableMapOf<Int, Permission>()

    init {
      for (permission in Permission.values()) {
        PERMISSION_MAP.put(permission.requestCode, permission)
      }
    }
  }
}

