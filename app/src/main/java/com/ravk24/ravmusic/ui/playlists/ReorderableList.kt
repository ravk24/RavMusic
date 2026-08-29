package com.ravk24.ravmusic.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.SongRow

/**
 * The playlist's rows with the two gestures the spec asks for, kept in one place (design D6):
 * long-press-drag on the handle reorders ([onMove]), a swipe from end to start removes
 * ([onRemove]). Row taps play ([onRowClick]). Missing rows are dimmed, the current one highlighted.
 */
@Composable
fun ReorderableTrackList(
    tracks: List<PlaylistTrack>,
    missingIds: Set<Long>,
    nowPlayingId: Long?,
    listState: LazyListState,
    onRowClick: (index: Int) -> Unit,
    onRemove: (trackId: Long) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun targetIndexFor(from: Int, offset: Float): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.index == from } ?: return from
        val center = dragged.offset + dragged.size / 2f + offset
        val hit = visible.firstOrNull { center >= it.offset && center < it.offset + it.size } ?: return from
        return hit.index.coerceIn(0, tracks.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_tracks"),
    ) {
        itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "track" }) { index, track ->
            val isDragging = draggingIndex == index
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onRemove(track.id)
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 20.dp),
                        )
                    }
                },
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
            ) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    SongRow(
                        song = track.toSong(),
                        onClick = { onRowClick(index) },
                        isCurrent = nowPlayingId != null && track.mediaStoreId == nowPlayingId,
                        dimmed = track.id in missingIds,
                        modifier = Modifier.testTag("track_row_${track.id}"),
                        leading = {
                            Icon(
                                imageVector = AppIcons.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("drag_handle_${track.id}")
                                    .pointerInput(track.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = index
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                            },
                                            onDragEnd = {
                                                val from = draggingIndex
                                                if (from != null) {
                                                    val to = targetIndexFor(from, dragOffset)
                                                    if (to != from) onMove(from, to)
                                                }
                                                draggingIndex = null
                                                dragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                dragOffset = 0f
                                            },
                                        )
                                    },
                            )
                        },
                    )
                }
            }
        }
    }
}
