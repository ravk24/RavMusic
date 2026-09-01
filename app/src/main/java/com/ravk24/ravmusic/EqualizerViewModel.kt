package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.settings.EQ_CUSTOM_PRESET
import com.ravk24.ravmusic.data.settings.EqualizerSettings
import com.ravk24.ravmusic.data.settings.EqualizerSettingsRepository
import com.ravk24.ravmusic.data.settings.clampStrength
import com.ravk24.ravmusic.data.settings.fitBandLevels
import com.ravk24.ravmusic.data.settings.fitPresetIndex
import com.ravk24.ravmusic.playback.EqCapabilities
import com.ravk24.ravmusic.playback.EqualizerCapabilitiesSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the equalizer sheet renders. [displayedBandLevels] is what the sliders show: the selected
 * preset's shape, or the stored custom levels fitted to this device. `capabilities` stays null
 * until the service answers; [loaded] gates the controls until the stored settings are read.
 */
data class EqualizerUiState(
    val loaded: Boolean = false,
    val capabilities: EqCapabilities? = null,
    val settings: EqualizerSettings = EqualizerSettings(),
    val displayedBandLevels: List<Int> = emptyList(),
) {
    /** The device preset shown as selected, or [EQ_CUSTOM_PRESET] for Custom. */
    val selectedPreset: Int
        get() = fitPresetIndex(settings.presetIndex, capabilities?.presetNames?.size ?: 0)
}

/** The band levels the sliders should show for [settings] on a device with [caps]. */
fun displayedBandLevels(settings: EqualizerSettings, caps: EqCapabilities): List<Int> {
    val preset = fitPresetIndex(settings.presetIndex, caps.presetNames.size)
    return if (preset != EQ_CUSTOM_PRESET && preset < caps.presetBandLevels.size) {
        caps.presetBandLevels[preset]
    } else {
        fitBandLevels(settings.bandLevels, caps.bandCount, caps.minLevelMb, caps.maxLevelMb)
    }
}

/**
 * Bridges the equalizer sheet to the settings store and the session (design D5). Edits land in a
 * draft immediately (so sliders track the finger) and are persisted latest-wins after a short
 * quiet period (design D6); the service hears every persisted value through DataStore.
 */
class EqualizerViewModel(
    private val repository: EqualizerSettingsRepository,
    capabilitiesSource: EqualizerCapabilitiesSource,
    private val persistDelayMs: Long = 100L,
) : ViewModel() {

    private val capabilities = MutableStateFlow<EqCapabilities?>(null)

    /** The live truth while this ViewModel exists; null until seeded from the store. */
    private val draft = MutableStateFlow<EqualizerSettings?>(null)

    val state: StateFlow<EqualizerUiState> = combine(capabilities, draft) { caps, settings ->
        EqualizerUiState(
            loaded = settings != null,
            capabilities = caps,
            settings = settings ?: EqualizerSettings(),
            displayedBandLevels = if (caps != null && settings != null) displayedBandLevels(settings, caps) else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EqualizerUiState())

    init {
        capabilitiesSource.request { capabilities.value = it }
        viewModelScope.launch { draft.value = repository.settings.first() }
        viewModelScope.launch {
            // Skip the seed itself (it is what the store already holds), then persist each edit
            // after a quiet period; collectLatest makes a drag one latest-wins write.
            draft.filterNotNull().drop(1).collectLatest { snapshot ->
                delay(persistDelayMs)
                repository.save(snapshot)
            }
        }
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun selectPreset(index: Int) = update { it.copy(presetIndex = index) }

    fun selectCustom() = update { it.copy(presetIndex = EQ_CUSTOM_PRESET) }

    /** Spec "Touching a band goes Custom": seeds the custom curve from what is on screen. */
    fun setBandLevel(band: Int, levelMb: Int) = update { settings ->
        val caps = capabilities.value ?: return@update settings
        val levels = displayedBandLevels(settings, caps).toMutableList()
        if (band !in levels.indices) return@update settings
        levels[band] = levelMb.coerceIn(caps.minLevelMb, caps.maxLevelMb)
        settings.copy(presetIndex = EQ_CUSTOM_PRESET, bandLevels = levels)
    }

    fun setBassBoost(strength: Int) = update { it.copy(bassBoost = clampStrength(strength)) }

    fun setVirtualizer(strength: Int) = update { it.copy(virtualizer = clampStrength(strength)) }

    private fun update(block: (EqualizerSettings) -> EqualizerSettings) {
        draft.value = draft.value?.let(block)
    }
}
