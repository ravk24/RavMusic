package com.ravk24.ravmusic.data.settings

/** The theme override chosen in Settings (spec F8). [SYSTEM] follows the device dark-mode setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Whether the dark palette should be shown given the device's current dark-mode state. */
    fun resolve(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Lenient parse of a stored name; anything unknown (or null) is [SYSTEM]. */
        fun fromStored(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
