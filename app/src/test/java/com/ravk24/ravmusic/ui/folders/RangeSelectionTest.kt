package com.ravk24.ravmusic.ui.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeSelectionTest {

    private val ids = listOf(10L, 20L, 30L, 40L, 50L, 60L)

    @Test
    fun `range grows downwards and upwards from the anchor`() {
        assertEquals(setOf(10L, 20L, 30L), rangeSelection(emptySet(), ids, anchor = 0, current = 2))
        assertEquals(setOf(20L, 30L, 40L), rangeSelection(emptySet(), ids, anchor = 3, current = 1))
        assertEquals(setOf(40L), rangeSelection(emptySet(), ids, anchor = 3, current = 3))
    }

    @Test
    fun `retreating towards the anchor shrinks the range because base is reused`() {
        val base = emptySet<Long>()
        val wide = rangeSelection(base, ids, anchor = 0, current = 3)
        val narrow = rangeSelection(base, ids, anchor = 0, current = 1)
        assertEquals(setOf(10L, 20L, 30L, 40L), wide)
        assertEquals(setOf(10L, 20L), narrow)
    }

    @Test
    fun `rows selected before the drag stay selected`() {
        assertEquals(setOf(60L, 10L, 20L), rangeSelection(setOf(60L), ids, anchor = 0, current = 1))
    }

    @Test
    fun `indices are clamped and an empty list returns the base`() {
        assertEquals(setOf(40L, 50L, 60L), rangeSelection(emptySet(), ids, anchor = 3, current = 99))
        assertEquals(setOf(10L, 20L), rangeSelection(emptySet(), ids, anchor = -5, current = 1))
        assertEquals(setOf(7L), rangeSelection(setOf(7L), emptyList(), anchor = 0, current = 0))
    }

    @Test
    fun `rowIndexAt maps inside, gaps, and beyond the edges`() {
        val rows = listOf(RowBounds(4, 0, 100), RowBounds(5, 108, 100), RowBounds(6, 216, 100))
        assertEquals(4, rowIndexAt(50f, rows))
        assertEquals(5, rowIndexAt(108f, rows))
        assertEquals(5, rowIndexAt(104f, rows)) // in the gap: next row down
        assertEquals(4, rowIndexAt(-30f, rows)) // above the first visible row
        assertEquals(6, rowIndexAt(999f, rows)) // below the last visible row: clamp
        assertNull(rowIndexAt(10f, emptyList()))
    }
}
