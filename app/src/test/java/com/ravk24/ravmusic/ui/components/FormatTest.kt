package com.ravk24.ravmusic.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `formatDuration minutes and seconds`() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:59", formatDuration(59_999L))
        assertEquals("3:41", formatDuration(221_000L))
        assertEquals("10:05", formatDuration(605_000L))
    }

    @Test
    fun `formatDuration with hours`() {
        assertEquals("1:02:05", formatDuration(3_725_000L))
        assertEquals("1:00:00", formatDuration(3_600_000L))
    }

    @Test
    fun `formatDuration clamps negatives`() {
        assertEquals("0:00", formatDuration(-5_000L))
    }

    @Test
    fun `songCountLabel pluralises`() {
        assertEquals("0 songs", songCountLabel(0))
        assertEquals("1 song", songCountLabel(1))
        assertEquals("572 songs", songCountLabel(572))
    }
}
