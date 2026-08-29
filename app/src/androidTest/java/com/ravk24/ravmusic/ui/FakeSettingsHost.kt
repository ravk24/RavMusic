package com.ravk24.ravmusic.ui

import com.ravk24.ravmusic.SettingsHost
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsHost]: applies writes immediately and records them. */
class FakeSettingsHost(
    theme: ThemeMode = ThemeMode.SYSTEM,
    minDuration: Long = MIN_SONG_DURATION_MS,
) : SettingsHost {
    override val themeMode = MutableStateFlow(theme)
    override val minDurationMs = MutableStateFlow(minDuration)
    val themeWrites = mutableListOf<ThemeMode>()
    val thresholdWrites = mutableListOf<Long>()

    override fun setThemeMode(mode: ThemeMode) {
        themeWrites += mode
        themeMode.value = mode
    }

    override fun setMinDuration(ms: Long) {
        thresholdWrites += ms
        minDurationMs.value = ms
    }
}
