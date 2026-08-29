package com.ravk24.ravmusic.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.EmptyState
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Permission state (design canvas artboard 1h). Shown in place of tab content while the
 * audio-read permission is missing.
 *
 * @param canRequest whether the system dialog can still be shown. When false, the action opens
 * the app's system Settings page instead.
 */
@Composable
fun NoMusicFoundScreen(
    canRequest: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val body = "This app needs permission to read audio files on this device. " +
        "Nothing leaves your phone — there is no internet access at all."
    if (canRequest) {
        EmptyState(
            icon = AppIcons.MusicNote,
            title = "No music found",
            body = body,
            actionLabel = "Allow access to audio",
            onAction = onRequestPermission,
            hint = "Already granted? Try copying files into /Music and rescan.",
            modifier = modifier.testTag("no_music_found"),
            actionModifier = Modifier.testTag("allow_access_button"),
        )
    } else {
        EmptyState(
            icon = AppIcons.MusicNote,
            title = "No music found",
            body = body,
            actionLabel = "Open settings",
            onAction = onOpenAppSettings,
            hint = "Access was turned off. Enable it under Permissions in system settings, then come back.",
            modifier = modifier.testTag("no_music_found"),
            actionModifier = Modifier.testTag("open_settings_button"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoMusicFoundPreview() {
    RavMusicTheme {
        NoMusicFoundScreen(canRequest = true, onRequestPermission = {}, onOpenAppSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun NoMusicFoundPermanentlyDeniedPreview() {
    RavMusicTheme {
        NoMusicFoundScreen(canRequest = false, onRequestPermission = {}, onOpenAppSettings = {})
    }
}
