package com.ravk24.ravmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayOrderTest {

    private fun linearNext(count: Int): (Int) -> Int? = { i -> if (i + 1 < count) i + 1 else null }

    @Test
    fun `linear order follows indices and finds the current position`() {
        val order = playOrder(count = 4, currentIndex = 2, first = 0, next = linearNext(4))
        assertEquals(listOf(0, 1, 2, 3), order.mediaIndices)
        assertEquals(2, order.currentPosition)
    }

    @Test
    fun `shuffled order follows the next function`() {
        val shuffled = mapOf(2 to 0, 0 to 3, 3 to 1)
        val order = playOrder(count = 4, currentIndex = 3, first = 2) { shuffled[it] }
        assertEquals(listOf(2, 0, 3, 1), order.mediaIndices)
        assertEquals(2, order.currentPosition)
    }

    @Test
    fun `empty or unset first gives an empty order`() {
        assertEquals(PlayOrder(emptyList(), -1), playOrder(0, -1, null) { null })
        assertEquals(PlayOrder(emptyList(), -1), playOrder(3, 0, null) { null })
        assertEquals(PlayOrder(emptyList(), -1), playOrder(3, 0, 7) { null })
    }

    @Test
    fun `a looping next never repeats an index`() {
        val order = playOrder(count = 3, currentIndex = 0, first = 0) { (it + 1) % 3 }
        assertEquals(listOf(0, 1, 2), order.mediaIndices)
    }

    @Test
    fun `current index outside the order reports -1`() {
        val order = playOrder(count = 3, currentIndex = 9, first = 0, next = linearNext(3))
        assertEquals(-1, order.currentPosition)
    }
}
