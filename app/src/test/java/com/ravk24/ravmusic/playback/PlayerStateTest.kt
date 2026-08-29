package com.ravk24.ravmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    private val now = NowPlaying(7L, "Copper Sky", "Nocturne Ave", "Music")

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
}
