package com.ravk24.ravmusic.ui.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.model.PlaylistSearchHit
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.data.model.isFiltering
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.SearchEmpty
import com.ravk24.ravmusic.ui.components.SearchTopBar
import com.ravk24.ravmusic.ui.components.SongRow
import com.ravk24.ravmusic.ui.components.UNKNOWN_ARTIST_LABEL
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Search across every playlist (change `search`), pushed above the Playlists tab. Each hit is a
 * song row whose second line reads "Artist · Playlist"; tapping it plays that playlist from that
 * song, the trailing action opens the playlist. Missing files are dimmed, the current song is
 * highlighted, and a blank query shows a hint rather than every track on the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    hits: List<PlaylistSearchHit>,
    missingIds: Set<Long>,
    nowPlayingId: Long?,
    onBack: () -> Unit,
    onPlayHit: (PlaylistSearchHit) -> Unit,
    onOpenPlaylist: (playlistId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_playlist_search"),
        topBar = {
            SearchTopBar(
                query = query,
                onQueryChange = onQueryChange,
                onClose = onBack,
                placeholder = "Search all playlists",
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            !isFiltering(query) -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 40.dp)
                    .testTag("search_hint"),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "Search songs across all your playlists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            hits.isEmpty() -> SearchEmpty(query = query, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("search_results"),
            ) {
                items(hits, key = { it.track.id }, contentType = { "hit" }) { hit ->
                    val track = hit.track
                    SongRow(
                        song = track.toSong(),
                        onClick = { onPlayHit(hit) },
                        isCurrent = nowPlayingId != null && track.mediaStoreId == nowPlayingId,
                        dimmed = track.id in missingIds,
                        subtitle = "${track.artist ?: UNKNOWN_ARTIST_LABEL} · ${hit.playlistName}",
                        trailing = {
                            IconButton(
                                onClick = { onOpenPlaylist(track.playlistId) },
                                modifier = Modifier.testTag("hit_open_${track.id}"),
                            ) {
                                Icon(
                                    imageVector = AppIcons.QueueMusic,
                                    contentDescription = "Open playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        modifier = Modifier.testTag("hit_row_${track.id}"),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistSearchPreview() {
    RavMusicTheme {
        PlaylistSearchScreen(
            query = "rain",
            onQueryChange = {},
            hits = listOf(
                PlaylistSearchHit(PlaylistTrack(2, 1, "content://media/external/audio/media/2", "Glass Rain", "Hyaline", 245_000L, 1), "Late night"),
                PlaylistSearchHit(PlaylistTrack(5, 2, "content://media/external/audio/media/2", "Glass Rain", "Hyaline", 245_000L, 0), "Focus"),
                PlaylistSearchHit(PlaylistTrack(7, 2, "content://media/external/audio/media/9", "Rain Check", null, 184_000L, 3), "Focus"),
            ),
            missingIds = setOf(7L),
            nowPlayingId = 2L,
            onBack = {},
            onPlayHit = {},
            onOpenPlaylist = {},
        )
    }
}
