package com.ravk24.ravmusic.playback

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravk24.ravmusic.data.model.Song
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File

/**
 * Drives the real [PlaybackService] through [PlayerConnection] with a bundled 3 s tone. Runs in
 * the app process, so the service, notification and audio focus are all the real thing.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    @get:Rule
    val timeout: Timeout = Timeout.seconds(120)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var connection: PlayerConnection
    private lateinit var toneUri: String

    @Before
    fun setUp() {
        val file = File(context.cacheDir, "test_tone.wav")
        instrumentation.context.assets.open("test_tone.wav").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        toneUri = Uri.fromFile(file).toString()
        onMain {
            connection = PlayerConnection(context)
            connection.connect()
        }
    }

    @After
    fun tearDown() {
        // The service outlives each test: reset the session modes so nothing leaks into the next one.
        onMain {
            connection.cancelSleepTimer()
            connection.setRepeat(RepeatMode.OFF)
            connection.setShuffle(false)
            connection.stopAndClear()
        }
        // Releasing right away can drop the cancel before the session handles it, and a timer
        // left running pauses the next test's playback: wait until the service confirms.
        runCatching { await("session reset", 5_000L) { it.sleepTimer == SleepTimerState.Off && !it.hasQueue } }
        onMain { connection.release() }
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun tone(id: Long, title: String) = Song(id, toneUri, title, null, 3_000L, "t", "Test")
    private fun missing(id: Long) =
        Song(id, Uri.fromFile(File(context.cacheDir, "missing_$id.wav")).toString(), "Missing $id", null, 3_000L, "t", "Test")

    private fun await(what: String, timeoutMs: Long = 20_000L, condition: (PlayerState) -> Boolean): PlayerState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val s = connection.state.value
            if (condition(s)) return s
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for $what; last state = ${connection.state.value}")
    }

    @Test
    fun playsPausesAndClears() {
        onMain { connection.play(planQueue(listOf(tone(1, "Tone")), 0, "Test")!!) }

        val playing = await("playback to start") { it.isPlaying }
        assertEquals(NowPlaying(1L, "Tone", null, "Test"), playing.nowPlaying)

        await("position to advance") {
            onMain { connection.refreshPosition() }
            it.positionMs > 200L
        }

        onMain { connection.togglePlayPause() }
        val paused = await("pause") { !it.isPlaying }
        assertTrue(paused.hasQueue)

        onMain { connection.stopAndClear() }
        val cleared = await("clear") { !it.hasQueue }
        assertNull(cleared.nowPlaying)
        assertFalse(cleared.isPlaying)
    }

    @Test
    fun shuffleFlagFollowsThePlan() {
        onMain { connection.play(planQueue(listOf(tone(1, "A"), tone(2, "B")), 0, "Test")!!, shuffle = true) }
        await("shuffled playback") { it.isPlaying }
        var shuffled: Boolean? = null
        onMain { shuffled = connection.shuffleModeEnabledForTest() }
        assertTrue(shuffled == true)

        onMain { connection.play(planQueue(listOf(tone(1, "A")), 0, "Test")!!, shuffle = false) }
        await("unshuffled playback") { it.isPlaying && it.nowPlaying?.songId == 1L }
        onMain { shuffled = connection.shuffleModeEnabledForTest() }
        assertFalse(shuffled == true)
    }

    private fun queueIds() = connection.state.value.queue.map { it.songId }

    @Test
    fun missingFileSkipIsReportedToTheController() {
        onMain { connection.play(planQueue(listOf(tone(1, "A"), missing(2), tone(3, "C")), 0, "Test")!!) }
        await("playback to start") { it.isPlaying }

        val reported = await("skip notice", 30_000L) { it.skipped != null }
        assertEquals("Missing 2", reported.skipped?.title)
        assertEquals(1, reported.skipped?.seq)

        val third = await("third item", 30_000L) { it.nowPlaying?.songId == 3L }
        assertEquals("Missing 2", third.skipped?.title)
    }

    @Test
    fun seekNextAndPrevious() {
        onMain { connection.play(planQueue(listOf(tone(1, "A"), tone(2, "B"), tone(3, "C")), 0, "Test")!!) }
        await("playback to start") { it.isPlaying }
        onMain { connection.seekTo(2_000L) }
        await("seek to land") {
            onMain { connection.refreshPosition() }
            it.positionMs >= 1_800L
        }
        onMain { connection.next() }
        await("second item") { it.nowPlaying?.songId == 2L }
        onMain { connection.previous() }
        await("first item again") { it.nowPlaying?.songId == 1L }
    }

    @Test
    fun repeatOneLoopsAndRepeatAllWraps() {
        onMain {
            connection.play(planQueue(listOf(tone(1, "A")), 0, "Test")!!)
            connection.setRepeat(RepeatMode.ONE)
        }
        await("repeat one reported") { it.isPlaying && it.repeatMode == RepeatMode.ONE }
        Thread.sleep(4_000)
        val still = connection.state.value
        assertTrue("should still be looping item 1: $still", still.isPlaying && still.nowPlaying?.songId == 1L)

        onMain {
            connection.play(planQueue(listOf(tone(1, "A"), tone(2, "B")), 1, "Test")!!)
            connection.setRepeat(RepeatMode.ALL)
        }
        await("B playing under repeat all") { it.isPlaying && it.nowPlaying?.songId == 2L && it.repeatMode == RepeatMode.ALL }
        await("wrap to A", timeoutMs = 15_000L) { it.nowPlaying?.songId == 1L }
        onMain { connection.setRepeat(RepeatMode.OFF) }
    }

    @Test
    fun shuffleToggleKeepsCurrentAndReportsQueue() {
        val songs = (1..6).map { tone(it.toLong(), "S$it") }
        onMain { connection.play(planQueue(songs, 0, "Test")!!) }
        val linear = await("linear queue") { it.isPlaying && it.queue.size == 6 && !it.shuffleEnabled }
        assertEquals((1L..6L).toList(), linear.queue.map { it.songId })
        assertEquals(0, linear.queueIndex)
        assertEquals(5, linear.remaining)

        onMain { connection.setShuffle(true) }
        val shuffled = await("shuffle on") { it.shuffleEnabled && it.queue.size == 6 }
        assertEquals(1L, shuffled.nowPlaying?.songId)
        assertEquals((1L..6L).toSet(), shuffled.queue.map { it.songId }.toSet())
        assertEquals(1L, shuffled.queue[shuffled.queueIndex].songId)
        onMain { connection.setShuffle(false) }
        await("shuffle off") { !it.shuffleEnabled && it.queue.map { q -> q.songId } == (1L..6L).toList() }
    }

    @Test
    fun jumpAndMoveWithShuffleOff() {
        onMain { connection.play(planQueue((1..4).map { tone(it.toLong(), "S$it") }, 0, "Test")!!) }
        await("queue") { it.isPlaying && it.queue.size == 4 }
        onMain { connection.jumpToQueuePosition(2) }
        await("jumped to S3") { it.nowPlaying?.songId == 3L && it.queueIndex == 2 }
        onMain { connection.moveInQueue(3, 1) }
        await("moved") { queueIds() == listOf(1L, 4L, 2L, 3L) }
        assertEquals(3L, connection.state.value.nowPlaying?.songId)
    }

    @Test
    fun moveWithShuffleOnFreezesOrderAndTurnsShuffleOff() {
        onMain { connection.play(planQueue((1..5).map { tone(it.toLong(), "S$it") }, 0, "Test")!!, shuffle = true) }
        val before = await("shuffled queue") { it.isPlaying && it.shuffleEnabled && it.queue.size == 5 }
        val shown = before.queue.map { it.songId }
        val currentId = before.nowPlaying?.songId
        onMain { connection.moveInQueue(4, 1) }
        val after = await("shuffle off after move") { !it.shuffleEnabled && it.queue.size == 5 }
        val expected = shown.toMutableList().also { val moved = it.removeAt(4); it.add(1, moved) }
        assertEquals(expected, after.queue.map { it.songId })
        assertEquals(currentId, after.nowPlaying?.songId)
        await("still playing") { it.isPlaying }
    }

    @Test
    fun sleepTimerFadesPausesAndRestoresVolume() {
        onMain {
            connection.play(planQueue(listOf(tone(1, "A"), tone(2, "B"), tone(3, "C")), 0, "Test")!!)
            connection.setRepeat(RepeatMode.ALL)
            connection.setSleepTimer(4_000L)
        }
        val armed = await("countdown reported") { it.isPlaying && it.sleepTimer is SleepTimerState.Countdown }
        val endAt = (armed.sleepTimer as SleepTimerState.Countdown).endAtElapsedMs
        assertTrue(endAt - android.os.SystemClock.elapsedRealtime() in 1_000L..4_500L)

        val paused = await("timer to pause playback", timeoutMs = 10_000L) { !it.isPlaying && it.sleepTimer == SleepTimerState.Off }
        assertTrue(paused.hasQueue)
        var volume = 0f
        onMain { volume = connection.volumeForTest() ?: -1f }
        assertEquals(1f, volume, 0.001f)
    }

    @Test
    fun sleepTimerEndOfTrackPausesAtTheTransition() {
        onMain {
            connection.play(planQueue(listOf(tone(1, "A"), tone(2, "B")), 0, "Test")!!)
            connection.setSleepTimerEndOfTrack()
        }
        await("end of track armed") { it.isPlaying && it.sleepTimer == SleepTimerState.EndOfTrack }
        val paused = await("pause at the transition", timeoutMs = 10_000L) { !it.isPlaying && it.nowPlaying?.songId == 2L }
        onMain { connection.refreshPosition() }
        assertTrue("position should be at the start, was ${connection.state.value.positionMs}", connection.state.value.positionMs < 500L)
        assertEquals(SleepTimerState.Off, paused.sleepTimer)
    }

    @Test
    fun sleepTimerExtendCancelAndReconnect() {
        onMain {
            connection.play(planQueue(listOf(tone(1, "A")), 0, "Test")!!)
            connection.setRepeat(RepeatMode.ONE)
            connection.setSleepTimer(60_000L)
        }
        val first = await("countdown") { it.sleepTimer is SleepTimerState.Countdown }
        val firstEnd = (first.sleepTimer as SleepTimerState.Countdown).endAtElapsedMs
        onMain { connection.extendSleepTimer(30_000L) }
        await("extended") { (it.sleepTimer as? SleepTimerState.Countdown)?.endAtElapsedMs == firstEnd + 30_000L }

        // A fresh controller sees the running timer.
        var other: PlayerConnection? = null
        onMain { other = PlayerConnection(context).also { it.connect() } }
        val deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline && other!!.state.value.sleepTimer !is SleepTimerState.Countdown) Thread.sleep(50)
        assertEquals(firstEnd + 30_000L, (other!!.state.value.sleepTimer as SleepTimerState.Countdown).endAtElapsedMs)
        onMain { other!!.release() }

        onMain { connection.cancelSleepTimer() }
        val cancelled = await("cancelled") { it.sleepTimer == SleepTimerState.Off }
        assertTrue(cancelled.isPlaying)
    }

    @Test
    fun missingFileMidQueueIsSkipped() {
        onMain { connection.play(planQueue(listOf(tone(1, "First"), missing(2), tone(3, "Third")), 0, "Test")!!) }
        val third = await("third item after skipping the missing one", timeoutMs = 30_000L) {
            it.nowPlaying?.songId == 3L && it.isPlaying
        }
        assertEquals("Third", third.nowPlaying?.title)
    }

    @Test
    fun missingLastFileEndsTheQueue() {
        onMain { connection.play(planQueue(listOf(tone(1, "Only"), missing(2)), 0, "Test")!!) }
        await("playback to start") { it.isPlaying }
        val ended = await("queue to end after the missing last item", timeoutMs = 30_000L) { !it.hasQueue }
        assertFalse(ended.isPlaying)
    }
}
