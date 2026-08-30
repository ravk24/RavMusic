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
 * The one long-press-drag reorder implementation (design D-35 / Phase 5 D8), shared by the
 * playlist detail and the queue sheet. [itemContent] receives a `handle` modifier that must be
 * put on the element the user drags; the row itself keeps its own taps and swipes. With
 * [enabled] false the handle is inert (a filtered list cannot be reordered meaningfully).
 */
@Composable
fun <T : Any> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemContent: @Composable (index: Int, item: T, handle: Modifier) -> Unit,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun targetIndexFor(from: Int, offset: Float): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.index == from } ?: return from
        val center = dragged.offset + dragged.size / 2f + offset
        val hit = visible.firstOrNull { center >= it.offset && center < it.offset + it.size } ?: return from
        return hit.index.coerceIn(0, items.lastIndex)
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val isDragging = draggingIndex == index
            val handle = if (!enabled) Modifier else Modifier.pointerInput(key(item)) {
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
            }
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
            ) {
                itemContent(index, item, handle)
            }
        }
    }
}

/**
 * The playlist's rows: [ReorderableList] plus a swipe-from-end-to-start remove on every row.
 * Missing rows are dimmed, the current one highlighted; row taps play ([onRowClick], the index
 * within [tracks]). With [reorderEnabled] false (a filtered list) the drag handle is not shown at
 * all; swipe-to-remove keeps working because it is keyed by track id, not position.
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
    reorderEnabled: Boolean = true,
) {
    ReorderableList(
        items = tracks,
        key = { it.id },
        listState = listState,
        onMove = onMove,
        modifier = modifier.testTag("playlist_tracks"),
        enabled = reorderEnabled,
    ) { index, track, handle ->
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
        ) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                SongRow(
                    song = track.toSong(),
                    onClick = { onRowClick(index) },
                    isCurrent = nowPlayingId != null && track.mediaStoreId == nowPlayingId,
                    dimmed = track.id in missingIds,
                    modifier = Modifier.testTag("track_row_${track.id}"),
                    leading = if (!reorderEnabled) {
                        null
                    } else {
                        {
                            Icon(
                                imageVector = AppIcons.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("drag_handle_${track.id}")
                                    .then(handle),
                            )
                        }
                    },
                )
            }
        }
    }
}
