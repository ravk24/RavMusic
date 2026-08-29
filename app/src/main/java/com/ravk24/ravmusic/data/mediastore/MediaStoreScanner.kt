package com.ravk24.ravmusic.data.mediastore

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.normaliseArtist

/**
 * Queries MediaStore for music on external storage. Live query, no cache (decision D-05).
 *
 * Folder identity: on API 29+ the audio table has `BUCKET_ID` / `BUCKET_DISPLAY_NAME`; on
 * API 26–28 it does not, so the folder is derived from the file path with [folderFromPath]
 * using the same rule MediaStore applies, giving identical ids on every API level.
 */
class MediaStoreScanner(
    private val resolver: ContentResolver,
    private val minDurationMs: Long = MIN_SONG_DURATION_MS,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : MediaScanner {

    // DURATION resolves to MediaColumns.DURATION, which the SDK stubs mark as API 29, although
    // the audio column has existed since API 1 (it was AudioColumns.DURATION before).
    @SuppressLint("InlinedApi")
    override fun scan(): List<Song> {
        val hasBucketColumns = sdkInt >= Build.VERSION_CODES.Q
        val projection = if (hasBucketColumns) {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.BUCKET_ID,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
            )
        }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val cursor: Cursor? = try {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "MediaStore query refused; treating library as empty", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "MediaStore query rejected; treating library as empty", e)
            null
        }
        if (cursor == null) return emptyList()

        return cursor.use { c -> readSongs(c, hasBucketColumns) }
    }

    private fun readSongs(c: Cursor, hasBucketColumns: Boolean): List<Song> {
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val bucketIdCol = if (hasBucketColumns) c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID) else -1
        val bucketNameCol = if (hasBucketColumns) c.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME) else -1
        @Suppress("DEPRECATION")
        val dataCol = if (hasBucketColumns) -1 else c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        val songs = ArrayList<Song>(c.count)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            val displayName = c.getString(nameCol)
            val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: "Unknown title"
            val folder = if (hasBucketColumns) {
                FolderRef(
                    id = c.getLong(bucketIdCol).toString(),
                    name = c.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: UNKNOWN_FOLDER_NAME,
                )
            } else {
                folderFromPath(c.getString(dataCol).orEmpty())
            }
            songs += Song(
                id = id,
                uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                title = title,
                artist = normaliseArtist(c.getString(artistCol)),
                durationMs = c.getLong(durationCol),
                folderId = folder.id,
                folderName = folder.name,
            )
        }
        return songs
    }

    private companion object {
        const val TAG = "MediaStoreScanner"
    }
}
