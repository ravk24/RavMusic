package com.ravk24.ravmusic.data.settings

/** Preset index meaning "no device preset: use the custom band levels". */
const val EQ_CUSTOM_PRESET = -1

/** The `audiofx` strength range shared by BassBoost and Virtualizer (0 = off, 1000 = max). */
const val EQ_MAX_STRENGTH = 1000

/**
 * The persisted equalizer state (design D4 of `add-equalizer`): everything the service needs to
 * recreate the sound on any device. Band levels are millibels in the device's band order; they
 * only mean something once fitted to a concrete device with [fitBandLevels].
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val presetIndex: Int = EQ_CUSTOM_PRESET,
    val bandLevels: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
)

/** Band levels as stored: comma-joined millibels, e.g. `"300,0,-200,0,150"`. */
fun encodeBandLevels(levels: List<Int>): String = levels.joinToString(",")

/** Inverse of [encodeBandLevels]; junk or an empty string decodes to no levels rather than failing. */
fun decodeBandLevels(stored: String): List<Int> =
    stored.split(',').mapNotNull { it.trim().toIntOrNull() }

/**
 * Fits stored levels onto a concrete device: a band-count mismatch (restored backup, ROM change)
 * resets to flat, and each level is clamped into the device's supported range (design D4).
 */
fun fitBandLevels(stored: List<Int>, bandCount: Int, minLevelMb: Int, maxLevelMb: Int): List<Int> =
    if (stored.size != bandCount) {
        List(bandCount) { 0 }
    } else {
        stored.map { it.coerceIn(minLevelMb, maxLevelMb) }
    }

/** A stored preset index the device does not offer falls back to Custom (design D4). */
fun fitPresetIndex(stored: Int, presetCount: Int): Int =
    if (stored in 0 until presetCount) stored else EQ_CUSTOM_PRESET

/** Bass boost / virtualizer strengths live in 0..[EQ_MAX_STRENGTH]. */
fun clampStrength(value: Int): Int = value.coerceIn(0, EQ_MAX_STRENGTH)
