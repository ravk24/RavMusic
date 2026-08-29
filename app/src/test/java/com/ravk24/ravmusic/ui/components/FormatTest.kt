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
    fun `formatTotalDuration drops seconds and shows hours when needed`() {
        assertEquals("0m", formatTotalDuration(0L))
        assertEquals("0m", formatTotalDuration(59_000L))
        assertEquals("51m", formatTotalDuration(51 * 60_000L + 30_000L))
        assertEquals("2h 58m", formatTotalDuration((2 * 60 + 58) * 60_000L))
        assertEquals("1h 0m", formatTotalDuration(3_600_000L))
        assertEquals("0m", formatTotalDuration(-5L))
    }

    @Test
    fun `thresholdLabel covers off seconds and minutes`() {
        assertEquals("Off", thresholdLabel(0L))
        assertEquals("Off", thresholdLabel(-1L))
        assertEquals("15s", thresholdLabel(15_000L))
        assertEquals("30s", thresholdLabel(30_000L))
        assertEquals("1 min", thresholdLabel(60_000L))
        assertEquals("2 min", thresholdLabel(120_000L))
    }

    @Test
    fun `formatScanTime is relative under a day and a date after`() {
        val zone = java.time.ZoneId.of("UTC")
        val scannedAt = 1_756_500_000_000L // 2025-08-29T21:20:00Z
        assertEquals("just now", formatScanTime(scannedAt, scannedAt, zone))
        assertEquals("just now", formatScanTime(scannedAt, scannedAt + 59_999L, zone))
        assertEquals("1 min ago", formatScanTime(scannedAt, scannedAt + 60_000L, zone))
        assertEquals("59 min ago", formatScanTime(scannedAt, scannedAt + 3_599_000L, zone))
        assertEquals("1 h ago", formatScanTime(scannedAt, scannedAt + 3_600_000L, zone))
        assertEquals("23 h ago", formatScanTime(scannedAt, scannedAt + 86_399_000L, zone))
        assertEquals("29 Aug", formatScanTime(scannedAt, scannedAt + 86_400_000L, zone))
        assertEquals("just now", formatScanTime(scannedAt, scannedAt - 5_000L, zone))
    }

    @Test
    fun `formatRemaining pads minutes rounds up and shows hours`() {
        assertEquals("32:14", formatRemaining(32 * 60_000L + 14_000L))
        assertEquals("02:10", formatRemaining(130_000L))
        assertEquals("15:00", formatRemaining(15 * 60_000L))
        assertEquals("15:00", formatRemaining(15 * 60_000L - 1L))
        assertEquals("14:59", formatRemaining(15 * 60_000L - 1_000L))
        assertEquals("1:02:14", formatRemaining(3_734_000L))
        assertEquals("00:00", formatRemaining(0L))
        assertEquals("00:00", formatRemaining(-5L))
    }

    @Test
    fun `playlistCountLabel pluralises`() {
        assertEquals("1 playlist", playlistCountLabel(1))
        assertEquals("6 playlists", playlistCountLabel(6))
    }

    @Test
    fun `songCountLabel pluralises`() {
        assertEquals("0 songs", songCountLabel(0))
        assertEquals("1 song", songCountLabel(1))
        assertEquals("572 songs", songCountLabel(572))
    }
}
