package com.ravk24.ravmusic.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system follows the device, light and dark override it`() {
        assertFalse(ThemeMode.SYSTEM.resolve(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.resolve(systemDark = true))
        assertFalse(ThemeMode.LIGHT.resolve(systemDark = false))
        assertFalse(ThemeMode.LIGHT.resolve(systemDark = true))
        assertTrue(ThemeMode.DARK.resolve(systemDark = false))
        assertTrue(ThemeMode.DARK.resolve(systemDark = true))
    }

    @Test
    fun `stored names parse leniently`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStored("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("purple"))
    }
}
