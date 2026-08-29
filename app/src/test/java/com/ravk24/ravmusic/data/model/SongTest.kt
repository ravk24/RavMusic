package com.ravk24.ravmusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongTest {

    @Test
    fun `normaliseArtist drops null blank and unknown`() {
        assertNull(normaliseArtist(null))
        assertNull(normaliseArtist(""))
        assertNull(normaliseArtist("   "))
        assertNull(normaliseArtist("<unknown>"))
        assertNull(normaliseArtist(" <unknown> "))
    }

    @Test
    fun `normaliseArtist keeps real names trimmed`() {
        assertEquals("Nocturne Ave", normaliseArtist("Nocturne Ave"))
        assertEquals("Hyaline", normaliseArtist("  Hyaline "))
    }
}
