package com.ravk24.ravmusic.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Empty / permission state (design canvas artboard 1h). Shown in place of tab content while
 * the audio-read permission is missing.
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 44.dp)
            .testTag("no_music_found"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp),
            )
        }
        Text(
            text = "No music found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "This app needs permission to read audio files on this device. " +
                "Nothing leaves your phone — there is no internet access at all.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (canRequest) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.testTag("allow_access_button"),
            ) {
                Text("Allow access to audio  ›")
            }
            Text(
                text = "Already granted? Try copying files into /Music and rescan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Button(
                onClick = onOpenAppSettings,
                modifier = Modifier.testTag("open_settings_button"),
            ) {
                Text("Open settings  ›")
            }
            Text(
                text = "Access was turned off. Enable it under Permissions in system settings, then come back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.Center,
            )
        }
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
