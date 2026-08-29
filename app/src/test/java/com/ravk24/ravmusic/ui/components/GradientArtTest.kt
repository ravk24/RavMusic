package com.ravk24.ravmusic.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradientArtTest {

    @Test
    fun `same seed always maps to the same gradient`() {
        assertEquals(artGradientIndex(42L), artGradientIndex(42L))
        assertEquals(artGradientIndex(7L), artGradientIndex(7L + 6L))
    }

    @Test
    fun `negative seeds stay in range`() {
        for (seed in listOf(-1L, -6L, -7L, Long.MIN_VALUE)) {
            assertTrue(artGradientIndex(seed) in 0 until ArtGradientPairs.size)
        }
    }

    @Test
    fun `all six gradients are reachable`() {
        assertEquals((0 until 6).toSet(), (0L until 6L).map(::artGradientIndex).toSet())
        assertEquals(6, ArtGradientPairs.size)
    }
}
