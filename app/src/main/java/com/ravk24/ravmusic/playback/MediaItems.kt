package com.ravk24.ravmusic.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ravk24.ravmusic.data.model.Song

/** Key under which each queued item carries the queue's origin label ("Rock"). */
const val EXTRA_ORIGIN = "com.ravk24.ravmusic.origin"

/**
 * Android side of the queue: every item carries the song id, title, artist and origin so the
 * UI can rebuild [NowPlaying] from the controller alone after reconnecting to a running session.
 */
fun QueuePlan.toMediaItems(): List<MediaItem> = songs.map { song ->
    MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setExtras(Bundle().apply { putString(EXTRA_ORIGIN, origin) })
                .build(),
        )
        .build()
}

fun nowPlayingFrom(item: MediaItem): NowPlaying = NowPlaying(
    songId = item.mediaId.toLongOrNull() ?: -1L,
    title = item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown title",
    artist = item.mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() },
    origin = item.mediaMetadata.extras?.getString(EXTRA_ORIGIN).orEmpty(),
)
