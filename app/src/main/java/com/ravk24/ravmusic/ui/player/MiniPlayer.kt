package com.ravk24.ravmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.UNKNOWN_ARTIST_LABEL
import com.ravk24.ravmusic.ui.components.artGradient
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * The docked mini player (design canvas artboard 1c, bottom bar). Rendered only while a queue is
 * loaded; the caller decides where it docks. Tapping the body is reserved for Now Playing
 * ([onExpand]); swiping it away stops playback ([onDismiss]).
 */
@Composable
fun MiniPlayer(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = state.nowPlaying ?: return
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) onDismiss()
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        },
        modifier = modifier.testTag("mini_player"),
    ) {
        MiniPlayerContent(now = now, state = state, onPlayPause = onPlayPause, onExpand = onExpand)
    }
}

@Composable
private fun MiniPlayerContent(
    now: NowPlaying,
    state: PlayerState,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Hairline plus the thin progress line drawn over it (mockup: 2 px primary line at the top edge).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .progressSemantics(state.progress)
                    .testTag("mini_player_progress"),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
                .testTag("mini_player_body"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(artGradient(now.songId), RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = now.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("mini_player_title"),
                )
                Text(
                    text = now.artist ?: UNKNOWN_ARTIST_LABEL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("mini_player_artist"),
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    .testTag("mini_player_toggle"),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) AppIcons.Pause else AppIcons.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    RavMusicTheme {
        MiniPlayer(
            state = PlayerState(
                nowPlaying = NowPlaying(4L, "Copper Sky", "Nocturne Ave", "Music"),
                isPlaying = true,
                positionMs = 84_000L,
                durationMs = 221_000L,
            ),
            onPlayPause = {},
            onExpand = {},
            onDismiss = {},
        )
    }
}
