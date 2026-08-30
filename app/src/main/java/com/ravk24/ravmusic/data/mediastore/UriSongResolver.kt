package com.ravk24.ravmusic.data.mediastore

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.ravk24.ravmusic.data.model.LibrarySnapshot
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.normaliseArtist
import com.ravk24.ravmusic.data.model.openedSong
import com.ravk24.ravmusic.data.model.titleFromFileName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns the URI of an audio file opened from outside the app into a [Song] the queue can play.
 *
 * Resolution order, each step falling through when it yields nothing:
 * 1. the loaded library already has a song with exactly this URI — reuse it (keeps its folder and
 *    lets the folder / playlist rows highlight);
 * 2. a MediaStore `content://media/...` URI — query it for the real id and tags, and hand back the
 *    canonical `audio/media/<id>` URI so it equals what the library and playlists store;
 * 3. a `file://` URI — look the path up in MediaStore, which yields a content URI the player can
 *    open under scoped storage;
 * 4. any other `content://` URI (a file manager's or messenger's provider) — the provider's
 *    `DISPLAY_NAME` becomes the title;
 * 5. otherwise the last path segment.
 * Steps 2–5 produce a synthetic negative id (see `openedSong`). Nothing here throws: a provider
 * that refuses the query just moves resolution to the next step.
 */
class UriSongResolver(
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun resolve(uriString: String, library: LibrarySnapshot?): Song = withContext(ioDispatcher) {
        library?.songs?.firstOrNull { it.uri == uriString }?.let { return@withContext it }
        val uri = Uri.parse(uriString)
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                if (uri.authority == MediaStore.AUTHORITY) {
                    queryMediaStore(uri, null, null)?.let { return@withContext it }
                }
                displayName(uri)?.let { return@withContext openedSong(uriString, titleFromFileName(it)) }
            }
            ContentResolver.SCHEME_FILE -> {
                @Suppress("DEPRECATION")
                val byPath = uri.path?.let { path ->
                    queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Audio.Media.DATA} = ?", arrayOf(path))
                }
                byPath?.let { return@withContext it }
            }
        }
        openedSong(uriString, titleFromFileName(uri.lastPathSegment))
    }

    // DURATION resolves to MediaColumns.DURATION (stubbed as API 29; the audio column is API 1).
    @SuppressLint("InlinedApi")
    private fun queryMediaStore(uri: Uri, selection: String?, args: Array<String>?): Song? = safely {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        resolver.query(uri, projection, selection, args, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            val displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
            val title = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                ?.takeIf { it.isNotBlank() }
                ?: titleFromFileName(displayName)
            Song(
                id = id,
                uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                title = title,
                artist = normaliseArtist(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))),
                durationMs = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)).coerceAtLeast(0L),
                folderId = "",
                folderName = "",
            )
        }
    }

    private fun displayName(uri: Uri): String? = safely {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col < 0) null else c.getString(col)?.takeIf { it.isNotBlank() }
        }
    }

    /** Providers may refuse (no grant), reject (unknown column) or not support a query; all mean "no answer". */
    private inline fun <T> safely(block: () -> T?): T? = try {
        block()
    } catch (e: SecurityException) {
        Log.w(TAG, "Provider refused the query", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Provider rejected the query", e)
        null
    } catch (e: UnsupportedOperationException) {
        Log.w(TAG, "Provider does not support queries", e)
        null
    }

    private companion object {
        const val TAG = "UriSongResolver"
    }
}
