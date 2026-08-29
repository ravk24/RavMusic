package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    @Before
    fun setUp() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = LibraryState.Idle,
                    onRefreshLibrary = {},
                    playerState = PlayerState(),
                    player = PlayerActions.none(),
                    playlists = FakePlaylistsHost(),
                )
            }
        }
    }

    @Test
    fun coldStart_showsPlaylistsSelected() {
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_playlists").assertIsSelected()
        composeRule.onNodeWithTag("tab_folders").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
    }

    @Test
    fun tapFolders_showsFoldersSelected() {
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").assertIsSelected()
        composeRule.onNodeWithTag("tab_playlists").assertIsNotSelected()
    }

    @Test
    fun reTapSelectedTab_keepsScreen() {
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").assertIsSelected()
    }

    @Test
    fun backFromFolders_returnsToPlaylists() {
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_playlists").assertIsSelected()
    }

    @Test
    fun overflowSettings_pushesScreenAndHidesBottomBar() {
        composeRule.onNodeWithTag("overflow_menu").performClick()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_footer").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()
    }

    @Test
    fun backFromSettings_restoresPlaylistsAndBottomBar() {
        composeRule.onNodeWithTag("overflow_menu").performClick()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_back").performClick()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_playlists").assertIsSelected()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
    }

    @Test
    fun systemBackFromSettings_restoresPreviousTab() {
        composeRule.onNodeWithTag("overflow_menu").performClick()
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
    }

    @Test
    fun backFromPlaylists_finishesActivity() {
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        // Grab the reference first: once back finishes the activity, the rule can no longer hand it out.
        val activity = composeRule.activity
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }
}
