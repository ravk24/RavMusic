package com.ravk24.ravmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The app's two persisted preferences, backed by Preferences DataStore (design D1). Reads are
 * flows so every consumer follows changes; writes are suspend functions that return once the
 * value is on disk. A corrupt or unreadable store reads as the defaults rather than failing.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    /** Theme override; [ThemeMode.SYSTEM] until the user chooses otherwise. */
    val themeMode: Flow<ThemeMode> = prefs.map { ThemeMode.fromStored(it[KEY_THEME_MODE]) }.distinctUntilChanged()

    /** "Skip short audio" threshold in millis; 0 means nothing is hidden. Defaults to [MIN_SONG_DURATION_MS]. */
    val minDurationMs: Flow<Long> = prefs.map { it[KEY_MIN_DURATION_MS] ?: MIN_SONG_DURATION_MS }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setMinDuration(ms: Long) {
        dataStore.edit { it[KEY_MIN_DURATION_MS] = ms.coerceAtLeast(0L) }
    }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_MIN_DURATION_MS = longPreferencesKey("min_duration_ms")

        /** The threshold choices Settings offers, in millis: Off, 15 s, 30 s, 1 min, 2 min. */
        val THRESHOLDS_MS: List<Long> = listOf(0L, 15_000L, 30_000L, 60_000L, 120_000L)
    }
}
