package com.ravk24.ravmusic.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.UNKNOWN_ARTIST_LABEL
import com.ravk24.ravmusic.ui.playlists.ReorderableList

/**
 * The queue sheet: the current song (highlighted) and everything after it in play order.
 * Tap a row to jump, drag its handle to reorder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerState,
    onJump: (position: Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("queue_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Queue · ${state.remaining} left",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("queue_header"),
            )
            ReorderableList(
                items = state.queue,
                key = { it.mediaIndex },
                listState = rememberLazyListState(),
                onMove = onMove,
                modifier = Modifier.testTag("queue_list"),
            ) { position, entry, handle ->
                val isCurrent = position == state.queueIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJump(position) }
                        .semantics { selected = isCurrent }
                        .padding(start = 16.dp, end = 20.dp, top = 11.dp, bottom = 11.dp)
                        .testTag("queue_row_$position"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("queue_handle_$position")
                            .then(handle),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = entry.artist ?: UNKNOWN_ARTIST_LABEL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isCurrent) {
                        Icon(
                            imageVector = if (state.isPlaying) AppIcons.Pause else AppIcons.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
