package com.ravk24.ravmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    private val now = NowPlaying(7L, "Copper Sky", "Nocturne Ave", "Music")
    private val queue = (0..4).map { QueueEntry(it.toLong(), "S$it", null, it) }

    @Test
    fun `no queue means nothing to show`() {
        assertFalse(PlayerState().hasQueue)
        assertTrue(PlayerState(nowPlaying = now).hasQueue)
    }

    @Test
    fun `progress is a clamped fraction and zero without a duration`() {
        assertEquals(0f, PlayerState(now, positionMs = 5_000L, durationMs = 0L).progress, 0.0001f)
        assertEquals(0.5f, PlayerState(now, positionMs = 50_000L, durationMs = 100_000L).progress, 0.0001f)
        assertEquals(1f, PlayerState(now, positionMs = 500_000L, durationMs = 100_000L).progress, 0.0001f)
        assertEquals(0f, PlayerState(now, positionMs = -1L, durationMs = 100_000L).progress, 0.0001f)
    }

    @Test
    fun `remaining hasNext hasPrevious follow the queue index`() {
        val second = PlayerState(now, queue = queue, queueIndex = 1)
        assertEquals(3, second.remaining)
        assertTrue(second.hasNext)
        assertTrue(second.hasPrevious)

        val first = second.copy(queueIndex = 0)
        assertFalse(first.hasPrevious)
        assertTrue(first.hasNext)

        val last = second.copy(queueIndex = 4)
        assertEquals(0, last.remaining)
        assertFalse(last.hasNext)

        val none = PlayerState()
        assertEquals(0, none.remaining)
        assertFalse(none.hasNext)
        assertFalse(none.hasPrevious)
    }

    @Test
    fun `repeat cycles off all one off`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }
}
