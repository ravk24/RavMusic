package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.data.settings.ThemeMode
import com.ravk24.ravmusic.ui.settings.SettingsScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private fun setScreen(
        host: FakeSettingsHost = FakeSettingsHost(),
        libraryState: LibraryState = LibraryState.Idle,
        onRescan: () -> Unit = {},
        now: Long = 2L,
    ) {
        composeRule.setContent {
            RavMusicTheme {
                SettingsScreen(
                    onBack = {},
                    settings = host,
                    libraryState = libraryState,
                    onRescan = onRescan,
                    now = { now },
                )
            }
        }
    }

    @Test
    fun selectionsReflectTheHost() {
        setScreen(FakeSettingsHost(theme = ThemeMode.DARK, minDuration = 15_000L))
        composeRule.onNodeWithTag("theme_dark").assertIsSelected()
        composeRule.onNodeWithTag("theme_system").assertIsNotSelected()
        composeRule.onNodeWithTag("min_duration_15000").assertIsSelected()
        composeRule.onNodeWithTag("min_duration_30000").assertIsNotSelected()
        composeRule.onNodeWithText("Off").assertIsDisplayed()
        composeRule.onNodeWithText("2 min").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_footer").assertIsDisplayed()
    }

    @Test
    fun tapsWriteThroughTheHost() {
        val host = FakeSettingsHost()
        setScreen(host)
        composeRule.onNodeWithTag("theme_light").performClick()
        composeRule.onNodeWithTag("min_duration_0").performClick()
        assertEquals(listOf(ThemeMode.LIGHT), host.themeWrites)
        assertEquals(listOf(0L), host.thresholdWrites)
        composeRule.onNodeWithTag("theme_light").assertIsSelected()
        composeRule.onNodeWithTag("min_duration_0").assertIsSelected()
    }

    @Test
    fun notScannedYet_disablesRescan() {
        setScreen(libraryState = LibraryState.Idle)
        composeRule.onNodeWithTag("settings_last_scan").assertTextContains("Library not scanned yet")
        composeRule.onNodeWithTag("settings_rescan").assertIsNotEnabled()
    }

    @Test
    fun loaded_showsScanInfoAndRescanCallsBack() {
        var rescans = 0
        setScreen(libraryState = FakeLibrary.loaded(), onRescan = { rescans++ }, now = 1L + 5_000L)
        composeRule.onNodeWithTag("settings_last_scan").assertTextContains("Last scan · just now · 4 songs")
        composeRule.onNodeWithTag("settings_rescan").assertIsEnabled().performClick()
        assertEquals(1, rescans)
        composeRule.onNodeWithTag("settings_rescan_progress").assertDoesNotExist()
    }

    @Test
    fun refreshing_showsProgressAndDisablesRescan() {
        setScreen(libraryState = FakeLibrary.loaded(refreshing = true))
        composeRule.onNodeWithTag("settings_rescan_progress").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_rescan").assertIsNotEnabled()
    }
}
