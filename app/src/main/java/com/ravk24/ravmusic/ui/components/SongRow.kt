package com.ravk24.ravmusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

const val UNKNOWN_ARTIST_LABEL = "Unknown artist"

/**
 * One song line (design canvas artboard 1d): title, artist or "Unknown artist", duration.
 * [isCurrent] highlights the song the player is on (exposed as `selected` for tests);
 * [dimmed] greys a song whose file is missing; [leading] hosts a checkbox or drag handle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    dimmed: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { selected = isCurrent }
            .alpha(if (dimmed) 0.45f else 1f)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist ?: UNKNOWN_ARTIST_LABEL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SongRowPreview() {
    RavMusicTheme {
        Column {
            SongRow(
                song = Song(1, "content://media/1", "Midnight Freeway", "Nocturne Ave", 221_000L, "m", "Music"),
                onClick = {},
            )
            SongRow(
                song = Song(2, "content://media/2", "A very long song title that will not fit on one line at all", null, 3_725_000L, "m", "Music"),
                onClick = {},
                isCurrent = true,
            )
            SongRow(
                song = Song(3, "content://media/3", "Deleted file", "Hyaline", 200_000L, "m", "Music"),
                onClick = {},
                dimmed = true,
            )
        }
    }
}
