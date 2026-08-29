package com.ravk24.ravmusic.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ravk24.ravmusic.permission.PermissionState

/**
 * Shows [content] only while the audio-read permission is granted; otherwise the
 * "No music found" state. One composable so both tabs share exactly one gate.
 */
@Composable
fun AudioPermissionGate(
    state: PermissionState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (state) {
        PermissionState.Granted -> content()
        is PermissionState.Denied -> NoMusicFoundScreen(
            canRequest = state.canRequest,
            onRequestPermission = onRequestPermission,
            onOpenAppSettings = onOpenAppSettings,
            modifier = modifier,
        )
        PermissionState.Unknown -> NoMusicFoundScreen(
            canRequest = true,
            onRequestPermission = onRequestPermission,
            onOpenAppSettings = onOpenAppSettings,
            modifier = modifier,
        )
    }
}
