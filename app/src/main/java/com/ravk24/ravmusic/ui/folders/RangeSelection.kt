package com.ravk24.ravmusic.ui.folders

/** A visible list row: its item index and vertical extent in the list's coordinates. */
data class RowBounds(val index: Int, val top: Int, val height: Int) {
    val bottom: Int get() = top + height
}

/**
 * The row under a pointer at [y]. A pointer in the gap between two rows, or above the first
 * visible row, resolves to the next row down; below the last visible row it clamps to that row,
 * so dragging past the edge keeps the range at the visible edge. Null only when nothing is visible.
 */
fun rowIndexAt(y: Float, rows: List<RowBounds>): Int? =
    rows.firstOrNull { y < it.bottom }?.index ?: rows.lastOrNull()?.index

/**
 * Drag-select (spec F2 stretch): everything in [base] plus the contiguous run of [ids] between
 * [anchor] and [current], inclusive, in either direction. Always computed from [base] (the
 * selection when the drag started), so moving back towards the anchor shrinks the range.
 */
fun rangeSelection(base: Set<Long>, ids: List<Long>, anchor: Int, current: Int): Set<Long> {
    if (ids.isEmpty()) return base
    val a = anchor.coerceIn(ids.indices)
    val c = current.coerceIn(ids.indices)
    return base + ids.subList(minOf(a, c), maxOf(a, c) + 1)
}
