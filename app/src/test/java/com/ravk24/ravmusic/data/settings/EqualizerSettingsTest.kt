package com.ravk24.ravmusic.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** The pure fitting rules of design D4: encode/decode, clamp, reset-to-flat, preset fallback. */
class EqualizerSettingsTest {

    @Test
    fun `band levels round-trip through the stored string`() {
        val levels = listOf(300, 0, -200, 0, 150)
        assertEquals("300,0,-200,0,150", encodeBandLevels(levels))
        assertEquals(levels, decodeBandLevels(encodeBandLevels(levels)))
    }

    @Test
    fun `empty and junk strings decode to no levels`() {
        assertEquals(emptyList<Int>(), decodeBandLevels(""))
        assertEquals(listOf(5), decodeBandLevels("abc,5,"))
    }

    @Test
    fun `matching band count clamps each level into the device range`() {
        assertEquals(
            listOf(1500, -1500, 0),
            fitBandLevels(listOf(2400, -9999, 0), bandCount = 3, minLevelMb = -1500, maxLevelMb = 1500),
        )
    }

    @Test
    fun `band count mismatch resets to flat`() {
        assertEquals(
            listOf(0, 0, 0, 0, 0),
            fitBandLevels(listOf(300, -300), bandCount = 5, minLevelMb = -1500, maxLevelMb = 1500),
        )
        assertEquals(listOf(0, 0, 0), fitBandLevels(emptyList(), bandCount = 3, minLevelMb = -1500, maxLevelMb = 1500))
    }

    @Test
    fun `preset index outside the device's presets falls back to Custom`() {
        assertEquals(2, fitPresetIndex(2, presetCount = 10))
        assertEquals(EQ_CUSTOM_PRESET, fitPresetIndex(10, presetCount = 10))
        assertEquals(EQ_CUSTOM_PRESET, fitPresetIndex(EQ_CUSTOM_PRESET, presetCount = 10))
        assertEquals(EQ_CUSTOM_PRESET, fitPresetIndex(0, presetCount = 0))
    }

    @Test
    fun `strengths clamp into the audiofx range`() {
        assertEquals(0, clampStrength(-5))
        assertEquals(600, clampStrength(600))
        assertEquals(EQ_MAX_STRENGTH, clampStrength(4000))
    }
}
