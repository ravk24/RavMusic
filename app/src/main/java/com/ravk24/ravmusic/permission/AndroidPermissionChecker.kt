package com.ravk24.ravmusic.permission

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Real [PermissionChecker] backed by the Android framework. */
class AndroidPermissionChecker(
    private val activity: Activity,
    private val permission: String = audioPermissionFor(),
) : PermissionChecker {

    override fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    override fun shouldShowRationale(): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}
