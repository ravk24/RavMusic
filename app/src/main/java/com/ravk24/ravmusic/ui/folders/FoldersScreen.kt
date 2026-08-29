package com.ravk24.ravmusic.ui.folders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.model.Folder
import com.ravk24.ravmusic.data.model.LibrarySnapshot
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.buildLibrarySnapshot
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.EmptyState
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

private val MIN_SONG_SECONDS = MIN_SONG_DURATION_MS / 1000

/**
 * Folders tab (design canvas artboard 1c): the library grouped by storage folder, with
 * pull-to-refresh, an empty state, and a loading indicator for the first query.
 */
@Composable
fun FoldersScreen(
    state: LibraryState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("screen_folders"),
    ) {
        when (state) {
            LibraryState.Idle, LibraryState.Loading -> {
                Header(total = null)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("folders_loading"))
                }
            }

            is LibraryState.Loaded -> if (state.snapshot.totalSongs == 0) {
                EmptyState(
                    icon = AppIcons.MusicNote,
                    title = "No music found",
                    body = "No audio files were found on this device. Copy music into a folder such as /Music, then rescan.",
                    actionLabel = "Rescan",
                    onAction = onRefresh,
                    hint = "Audio under $MIN_SONG_SECONDS s is hidden.",
                    modifier = Modifier.testTag("library_empty"),
                    actionModifier = Modifier.testTag("rescan_button"),
                )
            } else {
                Header(total = state.snapshot.totalSongs)
                FolderList(
                    snapshot = state.snapshot,
                    refreshing = state.refreshing,
                    listState = listState,
                    onRefresh = onRefresh,
                    onOpenFolder = onOpenFolder,
                )
            }
        }
    }
}

@Composable
private fun Header(total: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Folders",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (total != null) {
            Text(
                text = songCountLabel(total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag("folders_total"),
            )
        }
    }
}

@Composable
private fun FolderList(
    snapshot: LibrarySnapshot,
    refreshing: Boolean,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .testTag("folders_refresh"),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("folders_list"),
        ) {
            items(snapshot.folders, key = { it.id }, contentType = { "folder" }) { folder ->
                FolderRow(
                    folder = folder,
                    onClick = { onOpenFolder(folder) },
                    modifier = Modifier.testTag("folder_row_${folder.id}"),
                )
            }
            item(key = "footer", contentType = "footer") {
                Text(
                    text = "Pull to refresh · audio under ${MIN_SONG_SECONDS}s hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .testTag("folders_footer"),
                )
            }
        }
    }
}

private fun previewSnapshot(): LibrarySnapshot {
    fun song(id: Long, title: String, folderId: String, folderName: String) =
        Song(id, "content://media/$id", title, null, 200_000L, folderId, folderName)
    return buildLibrarySnapshot(
        listOf(
            song(1, "Midnight Freeway", "1", "Music"),
            song(2, "Glass Rain", "1", "Music"),
            song(3, "Copper Sky", "2", "Downloads"),
            song(4, "Slow Orbit", "3", "Bluetooth"),
        ),
        scannedAt = 0L,
    )
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenPreview() {
    RavMusicTheme {
        FoldersScreen(
            state = LibraryState.Loaded(previewSnapshot()),
            listState = rememberLazyListState(),
            onRefresh = {},
            onOpenFolder = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenEmptyPreview() {
    RavMusicTheme {
        FoldersScreen(
            state = LibraryState.Loaded(LibrarySnapshot.EMPTY),
            listState = rememberLazyListState(),
            onRefresh = {},
            onOpenFolder = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenLoadingPreview() {
    RavMusicTheme {
        FoldersScreen(
            state = LibraryState.Loading,
            listState = rememberLazyListState(),
            onRefresh = {},
            onOpenFolder = {},
        )
    }
}
