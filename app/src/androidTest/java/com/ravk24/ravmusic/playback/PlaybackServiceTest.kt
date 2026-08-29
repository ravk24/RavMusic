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
        onMain {
            connection.stopAndClear()
            connection.release()
        }
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
