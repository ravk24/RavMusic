package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.PlaylistSearchHit
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.ui.playlists.PlaylistSearchScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistSearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val glass = PlaylistSearchHit(
        PlaylistTrack(12, 1, "content://media/external/audio/media/2", "Glass Rain", "Hyaline", 245_000L, 1),
        "Late night",
    )
    private val rainCheck = PlaylistSearchHit(
        PlaylistTrack(22, 2, "content://media/external/audio/media/9", "Rain Check", null, 184_000L, 0),
        "Focus",
    )

    private class Calls {
        var played: PlaylistSearchHit? = null
        var opened: Long? = null
        val queries = mutableListOf<String>()
        var backs = 0
    }

    private fun set(
        query: String,
        hits: List<PlaylistSearchHit>,
        missing: Set<Long> = emptySet(),
        nowPlayingId: Long? = null,
    ): Calls {
        val calls = Calls()
        composeRule.setContent {
            RavMusicTheme {
                PlaylistSearchScreen(
                    query = query,
                    onQueryChange = { calls.queries += it },
                    hits = hits,
                    missingIds = missing,
                    nowPlayingId = nowPlayingId,
                    onBack = { calls.backs++ },
                    onPlayHit = { calls.played = it },
                    onOpenPlaylist = { calls.opened = it },
                )
            }
        }
        return calls
    }

    @Test
    fun blankQueryShowsTheHintAndTypingReportsTheQuery() {
        val calls = set(query = "", hits = emptyList())
        composeRule.onNodeWithTag("search_hint").assertIsDisplayed()
        composeRule.onNodeWithTag("search_results").assertDoesNotExist()
        composeRule.onNodeWithTag("search_empty").assertDoesNotExist()
        composeRule.onNodeWithTag("search_field").performTextInput("ra")
        assertEquals("ra", calls.queries.last())
        composeRule.onNodeWithTag("search_close").performClick()
        assertEquals(1, calls.backs)
    }

    @Test
    fun hitsShowArtistAndPlaylistAndReportTapsAndOpens() {
        val calls = set(query = "rain", hits = listOf(glass, rainCheck), missing = setOf(22L), nowPlayingId = 2L)
        composeRule.onNodeWithText("Hyaline · Late night").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist · Focus").assertIsDisplayed()
        composeRule.onNodeWithTag("hit_row_12").assertIsSelected()

        composeRule.onNodeWithTag("hit_row_22").performClick()
        assertEquals(rainCheck, calls.played)
        composeRule.onNodeWithTag("hit_open_12", useUnmergedTree = true).performClick()
        assertEquals(1L, calls.opened)
    }

    @Test
    fun noMatchShowsTheEmptyText() {
        set(query = "zzz", hits = emptyList())
        composeRule.onNodeWithTag("search_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No songs match “zzz”").assertIsDisplayed()
        composeRule.onNodeWithTag("search_hint").assertDoesNotExist()
    }
}
