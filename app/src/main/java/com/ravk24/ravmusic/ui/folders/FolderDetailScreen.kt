package com.ravk24.ravmusic.ui.folders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.SongRow
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * A folder's songs (design canvas artboard 1d, browse mode). Pushed above the tabs, so the
 * shell hides the bottom bar. Song taps are inert until the playback change adds a handler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    songs: List<Song>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {},
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_folder_detail"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("folder_detail_title"),
                        )
                        Text(
                            text = songCountLabel(songs.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("folder_detail_subtitle"),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("folder_detail_back")) {
                        Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs in this folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("folder_detail_empty"),
                )
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("songs_list"),
            ) {
                items(songs, key = { it.id }, contentType = { "song" }) { song ->
                    SongRow(
                        song = song,
                        onClick = { onSongClick(song) },
                        modifier = Modifier.testTag("song_row_${song.id}"),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderDetailScreenPreview() {
    RavMusicTheme {
        FolderDetailScreen(
            folderName = "Downloads",
            songs = listOf(
                Song(1, "content://media/1", "Midnight Freeway", "Nocturne Ave", 221_000L, "d", "Downloads"),
                Song(2, "content://media/2", "Paper Boats", null, 284_000L, "d", "Downloads"),
                Song(3, "content://media/3", "Static Bloom", "Hyaline", 3_725_000L, "d", "Downloads"),
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderDetailScreenEmptyPreview() {
    RavMusicTheme {
        FolderDetailScreen(folderName = "Old folder", songs = emptyList(), onBack = {})
    }
}
