package com.ravk24.ravmusic.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.ui.permission.AudioPermissionGate
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioPermissionGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private fun setGate(state: PermissionState, onRequest: () -> Unit = {}, onSettings: () -> Unit = {}) {
        composeRule.setContent {
            RavMusicTheme {
                AudioPermissionGate(
                    state = state,
                    onRequestPermission = onRequest,
                    onOpenAppSettings = onSettings,
                ) {
                    Text("GATED CONTENT", modifier = Modifier.testTag("gated_content"))
                }
            }
        }
    }

    @Test
    fun granted_showsContent() {
        setGate(PermissionState.Granted)
        composeRule.onNodeWithTag("gated_content").assertIsDisplayed()
        composeRule.onNodeWithTag("no_music_found").assertDoesNotExist()
    }

    @Test
    fun deniedRequestable_showsEmptyStateAndRequestsOnTap() {
        var requests = 0
        setGate(PermissionState.Denied(canRequest = true), onRequest = { requests++ })
        composeRule.onNodeWithTag("no_music_found").assertIsDisplayed()
        composeRule.onNodeWithText("No music found").assertIsDisplayed()
        composeRule.onNodeWithTag("gated_content").assertDoesNotExist()
        composeRule.onNodeWithTag("allow_access_button").performClick()
        assertEquals(1, requests)
    }

    @Test
    fun deniedPermanently_routesToSettings() {
        var settingsOpened = 0
        setGate(PermissionState.Denied(canRequest = false), onSettings = { settingsOpened++ })
        composeRule.onNodeWithTag("no_music_found").assertIsDisplayed()
        composeRule.onNodeWithTag("allow_access_button").assertDoesNotExist()
        composeRule.onNodeWithTag("open_settings_button").performClick()
        assertEquals(1, settingsOpened)
    }

    @Test
    fun unknown_isTreatedAsRequestable() {
        setGate(PermissionState.Unknown)
        composeRule.onNodeWithTag("allow_access_button").assertIsDisplayed()
    }
}
