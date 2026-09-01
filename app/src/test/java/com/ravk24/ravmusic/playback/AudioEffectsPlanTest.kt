package com.ravk24.ravmusic.playback

import com.ravk24.ravmusic.data.settings.EQ_MAX_STRENGTH
import com.ravk24.ravmusic.data.settings.EqualizerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure half of [AudioEffects]: what one snapshot means on one device. */
class AudioEffectsPlanTest {

    private val caps = EqCapabilities(
        equalizerSupported = true,
        bandCount = 5,
        centerFreqsMilliHz = listOf(60_000, 230_000, 910_000, 3_600_000, 14_000_000),
        minLevelMb = -1500,
        maxLevelMb = 1500,
        presetNames = listOf("Normal", "Rock"),
        presetBandLevels = listOf(List(5) { 0 }, listOf(500, 300, -100, 300, 500)),
        bassBoostSupported = true,
        virtualizerSupported = true,
    )

    @Test
    fun `a valid preset is used as-is with no band levels`() {
        val plan = planEqualizerApply(EqualizerSettings(enabled = true, presetIndex = 1), caps)
        assertTrue(plan.equalizerOn)
        assertEquals(1, plan.presetIndex)
        assertNull(plan.bandLevels)
    }

    @Test
    fun `custom uses fitted band levels`() {
        val plan = planEqualizerApply(
            EqualizerSettings(enabled = true, bandLevels = listOf(9000, 0, -9000, 100, 200)),
            caps,
        )
        assertNull(plan.presetIndex)
        assertEquals(listOf(1500, 0, -1500, 100, 200), plan.bandLevels)
    }

    @Test
    fun `an out-of-range preset falls back to custom flat when nothing is stored`() {
        val plan = planEqualizerApply(EqualizerSettings(enabled = true, presetIndex = 7), caps)
        assertNull(plan.presetIndex)
        assertEquals(List(5) { 0 }, plan.bandLevels)
    }

    @Test
    fun `master off disables everything but keeps the stored strengths`() {
        val plan = planEqualizerApply(EqualizerSettings(enabled = false, bassBoost = 600, virtualizer = 400), caps)
        assertFalse(plan.equalizerOn)
        assertFalse(plan.bassBoostOn)
        assertFalse(plan.virtualizerOn)
        assertEquals(600, plan.bassBoostStrength)
    }

    @Test
    fun `zero strength leaves that effect off even with master on`() {
        val plan = planEqualizerApply(EqualizerSettings(enabled = true, bassBoost = 0, virtualizer = 9999), caps)
        assertFalse(plan.bassBoostOn)
        assertTrue(plan.virtualizerOn)
        assertEquals(EQ_MAX_STRENGTH, plan.virtualizerStrength)
    }
}
