package com.ravk24.ravmusic.playback

import android.os.Bundle

/**
 * The session-level contract for the equalizer (design D3 of `add-equalizer`): one custom command
 * asking the service what this device's effects can do. Settings themselves never travel over the
 * session — DataStore is the single source of truth (design D1).
 */
/** How the equalizer UI asks the session what this device can do; `PlayerConnection` implements it. */
fun interface EqualizerCapabilitiesSource {
    fun request(onResult: (EqCapabilities?) -> Unit)
}

object EqualizerCommands {
    const val GET_CAPABILITIES = "com.ravk24.ravmusic.eq.capabilities"

    private const val KEY_EQ_SUPPORTED = "eq_supported"
    private const val KEY_BAND_COUNT = "eq_band_count"
    private const val KEY_CENTER_FREQS = "eq_center_freqs"
    private const val KEY_MIN_LEVEL = "eq_min_level"
    private const val KEY_MAX_LEVEL = "eq_max_level"
    private const val KEY_PRESET_NAMES = "eq_preset_names"
    private const val KEY_PRESET_LEVELS = "eq_preset_levels"
    private const val KEY_BASS_BOOST = "eq_bass_boost_supported"
    private const val KEY_VIRTUALIZER = "eq_virtualizer_supported"

    fun toBundle(caps: EqCapabilities): Bundle = Bundle().apply {
        putBoolean(KEY_EQ_SUPPORTED, caps.equalizerSupported)
        putInt(KEY_BAND_COUNT, caps.bandCount)
        putIntArray(KEY_CENTER_FREQS, caps.centerFreqsMilliHz.toIntArray())
        putInt(KEY_MIN_LEVEL, caps.minLevelMb)
        putInt(KEY_MAX_LEVEL, caps.maxLevelMb)
        putStringArray(KEY_PRESET_NAMES, caps.presetNames.toTypedArray())
        // [preset][band] flattened row-major; band count recovers the shape.
        putIntArray(KEY_PRESET_LEVELS, caps.presetBandLevels.flatten().toIntArray())
        putBoolean(KEY_BASS_BOOST, caps.bassBoostSupported)
        putBoolean(KEY_VIRTUALIZER, caps.virtualizerSupported)
    }

    fun fromBundle(bundle: Bundle?): EqCapabilities? {
        if (bundle == null || !bundle.containsKey(KEY_BAND_COUNT)) return null
        val bandCount = bundle.getInt(KEY_BAND_COUNT)
        val flat = bundle.getIntArray(KEY_PRESET_LEVELS)?.toList().orEmpty()
        return EqCapabilities(
            equalizerSupported = bundle.getBoolean(KEY_EQ_SUPPORTED, false),
            bandCount = bandCount,
            centerFreqsMilliHz = bundle.getIntArray(KEY_CENTER_FREQS)?.toList().orEmpty(),
            minLevelMb = bundle.getInt(KEY_MIN_LEVEL),
            maxLevelMb = bundle.getInt(KEY_MAX_LEVEL),
            presetNames = bundle.getStringArray(KEY_PRESET_NAMES)?.toList().orEmpty(),
            presetBandLevels = if (bandCount > 0) flat.chunked(bandCount) else emptyList(),
            bassBoostSupported = bundle.getBoolean(KEY_BASS_BOOST, false),
            virtualizerSupported = bundle.getBoolean(KEY_VIRTUALIZER, false),
        )
    }
}
