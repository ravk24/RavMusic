package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.repo.LibraryRepository
import com.ravk24.ravmusic.data.settings.SettingsRepository
import com.ravk24.ravmusic.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the Settings screen needs from the app (design D4 of `polish`); mirrors [PlaylistsHost].
 * Implemented by [SettingsViewModel]; previews use [NoSettings], tests a fake.
 */
interface SettingsHost {
    val themeMode: StateFlow<ThemeMode>
    val minDurationMs: StateFlow<Long>
    fun setThemeMode(mode: ThemeMode)

    /** Persists the threshold and re-queries the library once so lists reflect it. */
    fun setMinDuration(ms: Long)
}

/**
 * Activity-scoped: the theme override is read by `MainActivity` above the navigation graph, so
 * it cannot live in the Settings entry's own scope.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
) : ViewModel(), SettingsHost {

    override val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    override val minDurationMs: StateFlow<Long> =
        settings.minDurationMs.stateIn(viewModelScope, SharingStarted.Eagerly, MIN_SONG_DURATION_MS)

    override fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    override fun setMinDuration(ms: Long) {
        viewModelScope.launch {
            settings.setMinDuration(ms)
            library.refresh()
        }
    }
}

/** Inert host for previews. */
object NoSettings : SettingsHost {
    override val themeMode: StateFlow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override val minDurationMs: StateFlow<Long> = MutableStateFlow(MIN_SONG_DURATION_MS)
    override fun setThemeMode(mode: ThemeMode) = Unit
    override fun setMinDuration(ms: Long) = Unit
}
