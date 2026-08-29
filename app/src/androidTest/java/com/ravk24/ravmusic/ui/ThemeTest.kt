package com.ravk24.ravmusic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val blurple = Color(0xFF635BFF)
    private val navy = Color(0xFF0A2540)
    private val navySurface = Color(0xFF0C2E4E)

    @Test
    fun lightTheme_usesFixedBrandPalette() {
        var primary = Color.Unspecified
        var background = Color.Unspecified
        var onBackground = Color.Unspecified
        composeRule.setContent {
            RavMusicTheme(darkTheme = false) {
                primary = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
                onBackground = MaterialTheme.colorScheme.onBackground
            }
        }
        assertEquals(blurple, primary)
        assertEquals(Color.White, background)
        assertEquals(navy, onBackground)
    }

    @Test
    fun darkTheme_usesNavySurfacesAndSameAccent() {
        var primary = Color.Unspecified
        var background = Color.Unspecified
        var surface = Color.Unspecified
        composeRule.setContent {
            RavMusicTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
                surface = MaterialTheme.colorScheme.surface
            }
        }
        assertEquals(blurple, primary)
        assertEquals(navy, background)
        assertEquals(navySurface, surface)
    }
}
