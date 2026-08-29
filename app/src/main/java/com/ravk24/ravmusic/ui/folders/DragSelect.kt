package com.ravk24.ravmusic.ui.folders

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Long-press-then-drag range selection for a `LazyColumn` of songs (spec F2 stretch, design D8
 * of `polish`). Watches the Initial pointer pass, so the rows' own `combinedClickable` (which
 * consumes in the Main pass) keeps working unchanged: a plain long-press still enters selection
 * through the row, and this modifier only takes over once the finger moves. Moves are consumed
 * from here on so the list does not scroll under the drag.
 *
 * [selected] is read once when the drag starts and every move recomputes the range from it, so
 * dragging back towards the anchor shrinks the selection (see [rangeSelection]).
 */
fun Modifier.dragSelect(
    listState: LazyListState,
    ids: List<Long>,
    selected: () -> Set<Long>,
    onSelection: (Set<Long>) -> Unit,
): Modifier = pointerInput(ids, listState) {
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        // Hold still until the long-press timeout; lifting or wandering first is not a drag-select.
        val cancelled = withTimeoutOrNull(longPressTimeout) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                if (!change.pressed) return@withTimeoutOrNull true
                if ((change.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE")
            true
        }
        if (cancelled != null) return@awaitEachGesture

        fun rows() = listState.layoutInfo.visibleItemsInfo.map { RowBounds(it.index, it.offset, it.size) }
        val anchor = rowIndexAt(down.position.y, rows()) ?: return@awaitEachGesture
        val base = selected()
        onSelection(rangeSelection(base, ids, anchor, anchor))

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            change.consume()
            val current = rowIndexAt(change.position.y, rows()) ?: continue
            onSelection(rangeSelection(base, ids, anchor, current))
        }
    }
}
