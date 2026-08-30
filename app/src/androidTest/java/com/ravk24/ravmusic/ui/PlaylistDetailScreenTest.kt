package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.ui.playlists.PlaylistDetailScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playlist = Playlist(1, "Late night", 3, 650_000L, 0)
    private val tracks = listOf(
        PlaylistTrack(11, 1, "content://media/external/audio/media/1", "Midnight Freeway", "Nocturne Ave", 221_000L, 0),
        PlaylistTrack(12, 1, "content://media/external/audio/media/2", "Glass Rain", "Hyaline", 245_000L, 1),
        PlaylistTrack(13, 1, "content://media/external/audio/media/3", "Paper Boats", null, 184_000L, 2),
    )

    private class Calls {
        var played: Int? = null
        var shuffled = 0
        var renamed: String? = null
        var deleted = 0
        var removed: Long? = null
        var moved: Pair<Int, Int>? = null
        var cleaned = 0
        var openedFolders = 0
    }

    private fun set(
        missing: Set<Long> = emptySet(),
        nowPlayingId: Long? = null,
        list: List<PlaylistTrack> = tracks,
    ): Calls {
        val calls = Calls()
        composeRule.setContent {
            RavMusicTheme {
                PlaylistDetailScreen(
                    playlist = playlist,
                    tracks = list,
                    missingIds = missing,
                    nowPlayingId = nowPlayingId,
                    onBack = {},
                    onPlay = { calls.played = it },
                    onShufflePlay = { calls.shuffled++ },
                    onRename = { calls.renamed = it },
                    onDelete = { calls.deleted++ },
                    onRemoveTrack = { calls.removed = it },
                    onMove = { from, to -> calls.moved = from to to },
                    onOpenFolders = { calls.openedFolders++ },
                    onCleanUp = { calls.cleaned++ },
                )
            }
        }
        return calls
    }

    @Test
    fun headerButtonsAndHighlight() {
        val calls = set(nowPlayingId = 2L)
        composeRule.onNodeWithTag("playlist_title").assertTextEquals("Late night")
        composeRule.onNodeWithTag("playlist_subtitle").assertTextEquals("3 songs · 10m")
        composeRule.onNodeWithTag("track_row_12").assertIsSelected()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()

        composeRule.onNodeWithTag("playlist_shuffle").performClick()
        assertEquals(1, calls.shuffled)
        composeRule.onNodeWithTag("playlist_play").performClick()
        assertEquals(0, calls.played)
        composeRule.onNodeWithTag("track_row_13").performClick()
        assertEquals(2, calls.played)
    }

    @Test
    fun missingBannerAndCleanUp() {
        val calls = set(missing = setOf(13L))
        composeRule.onNodeWithTag("missing_banner").assertIsDisplayed()
        composeRule.onNodeWithText("1 song can't be found").assertIsDisplayed()
        composeRule.onNodeWithTag("clean_up").performClick()
        assertEquals(1, calls.cleaned)
    }

    @Test
    fun emptyPlaylistDisablesPlay() {
        val calls = set(list = emptyList())
        composeRule.onNodeWithTag("playlist_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No songs yet").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_play").assertIsNotEnabled()
        composeRule.onNodeWithTag("playlist_shuffle").assertIsNotEnabled()
        composeRule.onNodeWithTag("playlist_subtitle").assertTextEquals("0 songs · 0m")
        composeRule.onNodeWithTag("playlist_empty_action").performClick()
        assertEquals(1, calls.openedFolders)
    }

    @Test
    fun renameAndDeleteFlows() {
        val calls = set()
        composeRule.onNodeWithTag("playlist_menu").performClick()
        composeRule.onNodeWithTag("menu_rename").performClick()
        composeRule.onNodeWithTag("playlist_name_field").performTextReplacement("Late nights")
        composeRule.onNodeWithTag("playlist_name_confirm").performClick()
        assertEquals("Late nights", calls.renamed)

        composeRule.onNodeWithTag("playlist_menu").performClick()
        composeRule.onNodeWithTag("menu_delete").performClick()
        composeRule.onNodeWithTag("delete_cancel").performClick()
        assertEquals(0, calls.deleted)
        composeRule.onNodeWithTag("playlist_menu").performClick()
        composeRule.onNodeWithTag("menu_delete").performClick()
        composeRule.onNodeWithTag("delete_confirm").performClick()
        assertEquals(1, calls.deleted)
    }

    @Test
    fun swipeRemovesRow() {
        val calls = set()
        composeRule.onNodeWithTag("track_row_12").performTouchInput { swipeLeft() }
        composeRule.waitUntil(5_000) { calls.removed != null }
        assertEquals(12L, calls.removed)
    }

    @Test
    fun searchFiltersRowsMapsTheIndexAndKeepsSwipe() {
        val calls = set()
        composeRule.onNodeWithTag("playlist_search").performClick()
        composeRule.onNodeWithTag("search_field").performTextInput("glass")
        composeRule.onNodeWithTag("track_row_11").assertDoesNotExist()
        composeRule.onNodeWithTag("track_row_13").assertDoesNotExist()
        composeRule.onNodeWithTag("track_row_12").assertIsDisplayed()
        // No reordering while filtering: the handle is gone.
        composeRule.onNodeWithTag("drag_handle_12", useUnmergedTree = true).assertDoesNotExist()

        // The tap reports the index in the full playlist, not in the filtered list.
        composeRule.onNodeWithTag("track_row_12").performClick()
        assertEquals(1, calls.played)

        composeRule.onNodeWithTag("track_row_12").performTouchInput { swipeLeft() }
        composeRule.waitUntil(5_000) { calls.removed != null }
        assertEquals(12L, calls.removed)
    }

    @Test
    fun searchMatchesArtistShowsNoMatchAndClearsOrCloses() {
        set()
        composeRule.onNodeWithTag("playlist_search").performClick()
        composeRule.onNodeWithTag("search_field").performTextInput("nocturne")
        composeRule.onNodeWithTag("track_row_11").assertIsDisplayed()
        composeRule.onNodeWithTag("track_row_12").assertDoesNotExist()

        composeRule.onNodeWithTag("search_clear").performClick()
        composeRule.onNodeWithTag("track_row_12").assertIsDisplayed()
        composeRule.onNodeWithTag("drag_handle_12", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("search_field").performTextInput("zzz")
        composeRule.onNodeWithTag("search_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No songs match “zzz”").assertIsDisplayed()

        composeRule.onNodeWithTag("search_close").performClick()
        composeRule.onNodeWithTag("search_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("playlist_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("track_row_11").assertIsDisplayed()
        composeRule.onNodeWithTag("track_row_13").assertIsDisplayed()
    }

    @Test
    fun dragHandleReorders() {
        val calls = set()
        val rowHeight = composeRule.onNodeWithTag("track_row_11").fetchSemanticsNode().size.height.toFloat()
        composeRule.onNodeWithTag("drag_handle_12", useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(androidx.compose.ui.geometry.Offset(0f, -rowHeight * 1.2f), delayMillis = 100)
            up()
        }
        composeRule.waitUntil(5_000) { calls.moved != null }
        assertEquals(1 to 0, calls.moved)
    }
}
