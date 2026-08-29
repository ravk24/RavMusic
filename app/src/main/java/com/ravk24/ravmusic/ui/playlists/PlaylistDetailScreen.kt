package com.ravk24.ravmusic.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.EmptyState
import com.ravk24.ravmusic.ui.components.formatTotalDuration
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Playlist detail (design canvas artboard 1e): name, totals, Shuffle play / Play, the
 * reorderable track list, and Rename / Delete in the overflow. Pushed above the tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist?,
    tracks: List<PlaylistTrack>,
    missingIds: Set<Long>,
    nowPlayingId: Long?,
    onBack: () -> Unit,
    onPlay: (index: Int) -> Unit,
    onShufflePlay: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (trackId: Long) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onCleanUp: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFolders: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val name = playlist?.name ?: "Playlist"
    val playable = tracks.size - tracks.count { it.id in missingIds }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_playlist_detail"),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("playlist_back")) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("playlist_menu")) {
                            Icon(AppIcons.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuOpen = false
                                    renaming = true
                                },
                                modifier = Modifier.testTag("menu_rename"),
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuOpen = false
                                    confirmingDelete = true
                                },
                                modifier = Modifier.testTag("menu_delete"),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("playlist_title"),
                    )
                    Text(
                        text = "${songCountLabel(tracks.size)} · ${formatTotalDuration(tracks.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .testTag("playlist_subtitle"),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onShufflePlay,
                    enabled = playable > 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("playlist_shuffle"),
                ) {
                    Icon(AppIcons.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Shuffle play")
                }
                FilledTonalButton(
                    onClick = { onPlay(0) },
                    enabled = playable > 0,
                    modifier = Modifier.testTag("playlist_play"),
                ) {
                    Icon(AppIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Play")
                }
            }
            if (missingIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("missing_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val n = missingIds.size
                    Text(
                        text = if (n == 1) "1 song can't be found" else "$n songs can't be found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCleanUp, modifier = Modifier.testTag("clean_up")) { Text("Clean up") }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            if (tracks.isEmpty()) {
                EmptyState(
                    icon = AppIcons.QueueMusic,
                    title = "No songs yet",
                    body = "Open a folder, long-press songs to select them, then add them here.",
                    actionLabel = "Open Folders",
                    onAction = onOpenFolders,
                    modifier = Modifier.testTag("playlist_empty"),
                    actionModifier = Modifier.testTag("playlist_empty_action"),
                )
            } else {
                ReorderableTrackList(
                    tracks = tracks,
                    missingIds = missingIds,
                    nowPlayingId = nowPlayingId,
                    listState = rememberLazyListState(),
                    onRowClick = onPlay,
                    onRemove = onRemoveTrack,
                    onMove = onMove,
                )
            }
        }
    }

    if (renaming) {
        NamePlaylistDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = name,
            onConfirm = { newName ->
                renaming = false
                onRename(newName)
            },
            onDismiss = { renaming = false },
        )
    }
    if (confirmingDelete) {
        DeletePlaylistDialog(
            name = name,
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistDetailPreview() {
    RavMusicTheme {
        PlaylistDetailScreen(
            playlist = Playlist(1, "Late night", 3, 650_000L, 0),
            tracks = listOf(
                PlaylistTrack(1, 1, "content://media/external/audio/media/1", "Midnight Freeway", "Nocturne Ave", 221_000L, 0),
                PlaylistTrack(2, 1, "content://media/external/audio/media/2", "Glass Rain", "Hyaline", 245_000L, 1),
                PlaylistTrack(3, 1, "content://media/external/audio/media/3", "Paper Boats", null, 184_000L, 2),
            ),
            missingIds = setOf(3L),
            nowPlayingId = 2L,
            onBack = {}, onPlay = {}, onShufflePlay = {}, onRename = {}, onDelete = {},
            onRemoveTrack = {}, onMove = { _, _ -> }, onCleanUp = {},
        )
    }
}
