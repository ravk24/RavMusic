package com.ravk24.ravmusic.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.ravk24.ravmusic.data.settings.EQ_CUSTOM_PRESET
import com.ravk24.ravmusic.data.settings.EqualizerSettings
import com.ravk24.ravmusic.data.settings.clampStrength
import com.ravk24.ravmusic.data.settings.fitBandLevels
import com.ravk24.ravmusic.data.settings.fitPresetIndex

/**
 * What one concrete device's effects can do, read once from live `audiofx` instances at creation
 * (design D3). `presetBandLevels[preset][band]` lets the UI draw a preset's shape and seed Custom
 * from it without touching the live effect.
 */
data class EqCapabilities(
    val equalizerSupported: Boolean = false,
    val bandCount: Int = 0,
    val centerFreqsMilliHz: List<Int> = emptyList(),
    val minLevelMb: Int = 0,
    val maxLevelMb: Int = 0,
    val presetNames: List<String> = emptyList(),
    val presetBandLevels: List<List<Int>> = emptyList(),
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
) {
    val anySupported: Boolean get() = equalizerSupported || bassBoostSupported || virtualizerSupported
}

/** What [AudioEffects.apply] should do for one settings snapshot on one device — pure, JVM-testable. */
data class EqualizerApplyPlan(
    val equalizerOn: Boolean,
    val presetIndex: Int?,
    val bandLevels: List<Int>?,
    val bassBoostOn: Boolean,
    val bassBoostStrength: Int,
    val virtualizerOn: Boolean,
    val virtualizerStrength: Int,
)

/**
 * Resolves a stored snapshot against a device (design D4): a valid preset wins, anything else is
 * Custom with the levels fitted to the device; a zero strength leaves that effect off entirely so
 * nothing processes audio for no audible reason.
 */
fun planEqualizerApply(settings: EqualizerSettings, caps: EqCapabilities): EqualizerApplyPlan {
    val preset = fitPresetIndex(settings.presetIndex, caps.presetNames.size)
    val bassBoost = clampStrength(settings.bassBoost)
    val virtualizer = clampStrength(settings.virtualizer)
    return EqualizerApplyPlan(
        equalizerOn = settings.enabled,
        presetIndex = preset.takeIf { it != EQ_CUSTOM_PRESET },
        bandLevels = if (preset == EQ_CUSTOM_PRESET) {
            fitBandLevels(settings.bandLevels, caps.bandCount, caps.minLevelMb, caps.maxLevelMb)
        } else {
            null
        },
        bassBoostOn = settings.enabled && bassBoost > 0,
        bassBoostStrength = bassBoost,
        virtualizerOn = settings.enabled && virtualizer > 0,
        virtualizerStrength = virtualizer,
    )
}

/**
 * The service's handle on the `audiofx` effects for one audio session (design D2). Every call to
 * the flaky effect HAL goes through [safely]: a throw logs, marks nothing broken globally, and
 * never crashes the service. Created disabled; [apply] turns things on.
 */
class AudioEffects private constructor(
    private val equalizer: Equalizer?,
    private val bassBoost: BassBoost?,
    private val virtualizer: Virtualizer?,
    val capabilities: EqCapabilities,
) {

    /** Applies one settings snapshot; safe to call on every DataStore emission. */
    fun apply(settings: EqualizerSettings) {
        val plan = planEqualizerApply(settings, capabilities)
        equalizer?.let { eq ->
            safely("equalizer apply") {
                if (plan.equalizerOn) {
                    plan.presetIndex?.let { eq.usePreset(it.toShort()) }
                    plan.bandLevels?.forEachIndexed { band, level -> eq.setBandLevel(band.toShort(), level.toShort()) }
                }
                eq.enabled = plan.equalizerOn
            }
        }
        bassBoost?.let { bb ->
            safely("bass boost apply") {
                if (plan.bassBoostOn) bb.setStrength(plan.bassBoostStrength.toShort())
                bb.enabled = plan.bassBoostOn
            }
        }
        virtualizer?.let { v ->
            safely("virtualizer apply") {
                if (plan.virtualizerOn) v.setStrength(plan.virtualizerStrength.toShort())
                v.enabled = plan.virtualizerOn
            }
        }
    }

    fun release() {
        safely("equalizer release") { equalizer?.release() }
        safely("bass boost release") { bassBoost?.release() }
        safely("virtualizer release") { virtualizer?.release() }
    }

    companion object {
        private const val TAG = "AudioEffects"
        private const val PRIORITY = 0

        /** Creates whatever effects this device supports for [audioSessionId]; missing ones stay null. */
        fun create(audioSessionId: Int): AudioEffects {
            val equalizer = safelyOrNull("equalizer create") { Equalizer(PRIORITY, audioSessionId).apply { enabled = false } }
            val bassBoost = safelyOrNull("bass boost create") { BassBoost(PRIORITY, audioSessionId).apply { enabled = false } }
            val virtualizer = safelyOrNull("virtualizer create") { Virtualizer(PRIORITY, audioSessionId).apply { enabled = false } }
            return AudioEffects(
                equalizer = equalizer,
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                capabilities = readCapabilities(equalizer, bassBoost != null, virtualizer != null),
            )
        }

        /**
         * Reads the device's equalizer shape. Preset levels come from briefly selecting each
         * preset while the effect is still disabled, so nothing is audible.
         */
        private fun readCapabilities(eq: Equalizer?, bassBoost: Boolean, virtualizer: Boolean): EqCapabilities {
            if (eq == null) return EqCapabilities(bassBoostSupported = bassBoost, virtualizerSupported = virtualizer)
            return safelyOrNull("equalizer capabilities") {
                val bands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val presets = eq.numberOfPresets.toInt()
                val presetNames = (0 until presets).map { eq.getPresetName(it.toShort()) }
                val presetLevels = (0 until presets).map { preset ->
                    eq.usePreset(preset.toShort())
                    (0 until bands).map { band -> eq.getBandLevel(band.toShort()).toInt() }
                }
                EqCapabilities(
                    equalizerSupported = true,
                    bandCount = bands,
                    centerFreqsMilliHz = (0 until bands).map { eq.getCenterFreq(it.toShort()) },
                    minLevelMb = range[0].toInt(),
                    maxLevelMb = range[1].toInt(),
                    presetNames = presetNames,
                    presetBandLevels = presetLevels,
                    bassBoostSupported = bassBoost,
                    virtualizerSupported = virtualizer,
                )
            } ?: EqCapabilities(bassBoostSupported = bassBoost, virtualizerSupported = virtualizer)
        }

        private inline fun safely(what: String, block: () -> Unit) {
            try {
                block()
            } catch (e: RuntimeException) {
                Log.w(TAG, "Audio effect failed: $what", e)
            }
        }

        private inline fun <T> safelyOrNull(what: String, block: () -> T): T? = try {
            block()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Audio effect failed: $what", e)
            null
        }
    }
}
