package com.ravk24.ravmusic.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.EmptyState
import com.ravk24.ravmusic.ui.components.formatTotalDuration
import com.ravk24.ravmusic.ui.components.playlistCountLabel
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Playlists home (design canvas artboard 1a): a two-column grid of playlist cards, a "+"
 * FAB, and the overflow menu that is the app's only route to Settings.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    gridState: LazyGridState,
    onOpenSettings: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
) {
    var showNewDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("screen_playlists"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (playlists.isNotEmpty()) {
                    Text(
                        text = playlistCountLabel(playlists.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("playlists_total"),
                    )
                }
                IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("playlists_search")) {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("overflow_menu"),
                    ) {
                        Icon(
                            imageVector = AppIcons.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            },
                            modifier = Modifier.testTag("menu_settings"),
                        )
                    }
                }
            }
            if (playlists.isEmpty()) {
                EmptyState(
                    icon = AppIcons.QueueMusic,
                    title = "No playlists yet",
                    body = "Open a folder, long-press songs to select them, then add them to a playlist.",
                    actionLabel = "New playlist",
                    onAction = { showNewDialog = true },
                    hint = "Playlists stay on this phone.",
                    modifier = Modifier.testTag("playlists_empty"),
                    actionModifier = Modifier.testTag("empty_new_playlist"),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("playlists_list"),
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist) },
                            modifier = Modifier.testTag("playlist_card_${playlist.id}"),
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showNewDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_playlist_fab"),
        ) {
            Icon(AppIcons.Add, contentDescription = "New playlist")
        }
    }

    if (showNewDialog) {
        NamePlaylistDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                showNewDialog = false
                onCreate(name)
            },
            onDismiss = { showNewDialog = false },
        )
    }
}

/** One grid tile: name and "N songs · 2h 58m" on a bordered surface. */
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${songCountLabel(playlist.songCount)} · ${formatTotalDuration(playlist.totalDurationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsScreenPreview() {
    RavMusicTheme {
        PlaylistsScreen(
            playlists = listOf(
                Playlist(1, "Late night", 42, (2 * 60 + 58) * 60_000L, 0),
                Playlist(2, "Focus", 28, (60 + 51) * 60_000L, 0),
                Playlist(3, "Road trip", 64, (4 * 60 + 12) * 60_000L, 0),
            ),
            gridState = rememberLazyGridState(),
            onOpenSettings = {},
            onOpenPlaylist = {},
            onCreate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsScreenEmptyPreview() {
    RavMusicTheme {
        PlaylistsScreen(
            playlists = emptyList(),
            gridState = rememberLazyGridState(),
            onOpenSettings = {},
            onOpenPlaylist = {},
            onCreate = {},
        )
    }
}
