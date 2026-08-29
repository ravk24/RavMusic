package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueueEntry
import com.ravk24.ravmusic.playback.RepeatMode
import com.ravk24.ravmusic.ui.nowplaying.NowPlayingScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playing = PlayerState(
        nowPlaying = NowPlaying(2L, "Copper Sky", "Nocturne Ave", "Late night"),
        isPlaying = true,
        positionMs = 118_000L,
        durationMs = 221_000L,
        queue = (1..5).map { QueueEntry(it.toLong(), "Song $it", if (it % 2 == 0) "Artist $it" else null, it - 1) },
        queueIndex = 1,
    )

    private class Calls {
        var playPause = 0; var next = 0; var prev = 0; var shuffle = 0; var repeat = 0
        var seek: Long? = null; var jump: Int? = null; var move: Pair<Int, Int>? = null; var refreshes = 0; var collapse = 0
    }

    private fun set(state: PlayerState): Calls {
        val c = Calls()
        val actions = PlayerActions.none().copy(
            onPlayPause = { c.playPause++ }, onNext = { c.next++ }, onPrevious = { c.prev++ },
            onToggleShuffle = { c.shuffle++ }, onCycleRepeat = { c.repeat++ }, onSeek = { c.seek = it },
            onJumpTo = { c.jump = it }, onMoveInQueue = { f, t -> c.move = f to t }, onRefreshPosition = { c.refreshes++ },
        )
        composeRule.setContent {
            RavMusicTheme { NowPlayingScreen(state = state, actions = actions, onCollapse = { c.collapse++ }) }
        }
        return c
    }

    @Test
    fun showsSongOriginTimesAndStateIcons() {
        set(playing.copy(shuffleEnabled = true, repeatMode = RepeatMode.ALL))
        composeRule.onNodeWithTag("np_title").assertTextEquals("Copper Sky")
        composeRule.onNodeWithTag("np_artist").assertTextEquals("Nocturne Ave")
        composeRule.onNodeWithTag("np_origin").assertTextEquals("Late night")
        composeRule.onNodeWithTag("np_elapsed").assertTextEquals("1:58")
        composeRule.onNodeWithTag("np_total").assertTextEquals("3:41")
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Shuffle on").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Repeat all").assertIsDisplayed()
        composeRule.onNodeWithText("Queue · 3 left").assertIsDisplayed()
    }

    @Test
    fun repeatOneAndPausedStates() {
        set(playing.copy(isPlaying = false, repeatMode = RepeatMode.ONE, nowPlaying = playing.nowPlaying!!.copy(artist = null)))
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Repeat one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Shuffle off").assertIsDisplayed()
        composeRule.onNodeWithTag("np_artist").assertTextEquals("Unknown artist")
    }

    @Test
    fun controlsCallBack() {
        val c = set(playing)
        composeRule.onNodeWithTag("np_play_pause").performClick()
        composeRule.onNodeWithTag("np_next").performClick()
        composeRule.onNodeWithTag("np_prev").performClick()
        composeRule.onNodeWithTag("np_shuffle").performClick()
        composeRule.onNodeWithTag("np_repeat").performClick()
        composeRule.onNodeWithTag("np_collapse").performClick()
        assertEquals(1, c.playPause); assertEquals(1, c.next); assertEquals(1, c.prev)
        assertEquals(1, c.shuffle); assertEquals(1, c.repeat); assertEquals(1, c.collapse)
        composeRule.onNodeWithTag("np_sleep_chip").performClick()
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
    }

    @Test
    fun seekBarReleaseSeeksIntoTheUpperHalf() {
        val c = set(playing)
        composeRule.onNodeWithTag("np_seek").performTouchInput { swipeRight() }
        composeRule.waitUntil(5_000) { c.seek != null }
        assertNotNull(c.seek)
        assertTrue("seek=${c.seek}", c.seek!! > playing.durationMs / 2)
    }

    @Test
    fun tickerRefreshesWhilePlaying() {
        val c = set(playing)
        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.waitUntil(3_000) { c.refreshes >= 2 }
        assertTrue(c.refreshes >= 2)
    }

    @Test
    fun queueChipOpensSheetTapJumps() {
        val c = set(playing)
        composeRule.onNodeWithTag("np_queue_chip").performClick()
        composeRule.onNodeWithTag("queue_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_header").assertTextEquals("Queue · 3 left")
        composeRule.onNodeWithTag("queue_row_1").assertIsSelected()
        composeRule.onNodeWithText("Song 4").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_row_3").performClick()
        assertEquals(3, c.jump)
    }

    @Test
    fun queueDragMoves() {
        val c = set(playing)
        composeRule.onNodeWithTag("np_queue_chip").performClick()
        composeRule.onNodeWithTag("queue_sheet").assertIsDisplayed()
        val rowHeight = composeRule.onNodeWithTag("queue_row_2").fetchSemanticsNode().size.height.toFloat()
        composeRule.onNodeWithTag("queue_handle_3", useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(androidx.compose.ui.geometry.Offset(0f, -rowHeight * 1.2f), delayMillis = 100)
            up()
        }
        composeRule.waitUntil(5_000) { c.move != null }
        assertEquals(3 to 2, c.move)
    }
}
