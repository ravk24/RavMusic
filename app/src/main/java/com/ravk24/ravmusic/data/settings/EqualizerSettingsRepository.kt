package com.ravk24.ravmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The persisted equalizer state, backed by the same Preferences DataStore as [SettingsRepository]
 * (design D1 of `add-equalizer`). The UI writes whole snapshots; `PlaybackService` collects
 * [settings] and applies each one to the live effects. Defaults mean "effects off", so a fresh
 * install sounds exactly as before.
 */
class EqualizerSettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<EqualizerSettings> = prefs.map { p ->
        EqualizerSettings(
            enabled = p[KEY_EQ_ENABLED] ?: false,
            presetIndex = p[KEY_EQ_PRESET] ?: EQ_CUSTOM_PRESET,
            bandLevels = decodeBandLevels(p[KEY_EQ_BAND_LEVELS] ?: ""),
            bassBoost = clampStrength(p[KEY_EQ_BASS_BOOST] ?: 0),
            virtualizer = clampStrength(p[KEY_EQ_VIRTUALIZER] ?: 0),
        )
    }.distinctUntilChanged()

    /**
     * Persists the whole snapshot. The UI edits a draft and saves it conflated (design D6), so a
     * single latest-wins write is simpler and race-free compared to five per-field setters.
     */
    suspend fun save(settings: EqualizerSettings) {
        dataStore.edit {
            it[KEY_EQ_ENABLED] = settings.enabled
            it[KEY_EQ_PRESET] = settings.presetIndex
            it[KEY_EQ_BAND_LEVELS] = encodeBandLevels(settings.bandLevels)
            it[KEY_EQ_BASS_BOOST] = clampStrength(settings.bassBoost)
            it[KEY_EQ_VIRTUALIZER] = clampStrength(settings.virtualizer)
        }
    }

    companion object {
        val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val KEY_EQ_PRESET = intPreferencesKey("eq_preset")
        val KEY_EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val KEY_EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val KEY_EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
    }
}
