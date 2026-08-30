package com.ravk24.ravmusic.data.mediastore

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.buildLibrarySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UriSongResolverTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = UriSongResolver(context.contentResolver)
    private var inserted: Uri? = null

    @After
    fun cleanUp() {
        inserted?.let { context.contentResolver.delete(it, null, null) }
    }

    private fun toneBytes(): ByteArray = instrumentation.context.assets.open("test_tone.wav").use { it.readBytes() }

    @Test
    fun librarySongWithTheSameUriIsReused() = runBlocking {
        val library = Song(7L, "content://media/external/audio/media/7", "Glass Rain", "Hyaline", 245_000L, "m", "Music")
        val snapshot = buildLibrarySnapshot(listOf(library), scannedAt = 1L)
        val song = resolver.resolve(library.uri, snapshot)
        assertEquals(library, song)
    }

    @Test
    fun fileUriOutsideMediaStoreGetsTitleFromTheFileName() = runBlocking {
        val file = File(context.cacheDir, "resolver_tone.wav").apply { writeBytes(toneBytes()) }
        try {
            val uri = Uri.fromFile(file).toString()
            val song = resolver.resolve(uri, null)
            assertEquals("resolver_tone", song.title)
            assertNull(song.artist)
            assertEquals(uri, song.uri)
            assertTrue("synthetic id must be <= -2 but was ${song.id}", song.id <= -2L)
        } finally {
            file.delete()
        }
    }

    @Test
    fun unknownContentUriFallsBackToTheLastSegment() = runBlocking {
        val uri = "content://com.example.nothing/docs/voice%20note.m4a"
        val song = resolver.resolve(uri, null)
        assertEquals("voice note", song.title)
        assertTrue(song.id <= -2L)
    }

    @Test
    fun mediaStoreUriResolvesToTheRealRow() = runBlocking {
        // Contributing to MediaStore without storage permissions needs the scoped-storage API (29+).
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "resolver_probe.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/RavMusicTest")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val cr = context.contentResolver
        val uri = checkNotNull(cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)) { "insert failed" }
        inserted = uri
        cr.openOutputStream(uri)!!.use { it.write(toneBytes()) }
        cr.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)

        val song = resolver.resolve(uri.toString(), null)
        val id = ContentUris.parseId(uri)
        assertEquals(id, song.id)
        assertEquals(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(), song.uri)
        assertEquals("resolver_probe", song.title)
    }
}
