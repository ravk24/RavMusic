package com.ravk24.ravmusic.permission

import android.Manifest
import android.os.Build

/**
 * The single runtime permission the app needs, chosen by Android version:
 * READ_MEDIA_AUDIO on Android 13+ (API 33), READ_EXTERNAL_STORAGE on API 26–32.
 */
fun audioPermissionFor(sdkInt: Int = Build.VERSION.SDK_INT): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** Observable state of the audio-read permission. */
sealed interface PermissionState {
    /** Not evaluated yet (before the first refresh). Treated like [Denied] by the UI. */
    data object Unknown : PermissionState

    data object Granted : PermissionState

    /**
     * @param canRequest true when the system will still show the permission dialog;
     * false when the user has permanently denied it and system settings is the only way back.
     */
    data class Denied(val canRequest: Boolean) : PermissionState
}

/**
 * Abstraction over the Android permission checks so [com.ravk24.ravmusic.AppViewModel]
 * stays a plain JVM class and can be unit-tested without a device.
 */
interface PermissionChecker {
    fun isGranted(): Boolean
    fun shouldShowRationale(): Boolean
}
