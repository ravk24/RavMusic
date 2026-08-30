package com.ravk24.ravmusic.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravk24.ravmusic.NoPlaylists
import com.ravk24.ravmusic.PlaylistsHost
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.matching
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.EmptyState
import com.ravk24.ravmusic.ui.components.SearchEmpty
import com.ravk24.ravmusic.ui.components.SearchTopBar
import com.ravk24.ravmusic.ui.components.SongRow
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.playlists.AddToPlaylistSheet
import com.ravk24.ravmusic.ui.playlists.DuplicatesDialog
import com.ravk24.ravmusic.ui.playlists.NamePlaylistDialog
import com.ravk24.ravmusic.ui.theme.Cyan
import com.ravk24.ravmusic.ui.theme.Navy
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import com.ravk24.ravmusic.ui.theme.White
import kotlinx.coroutines.launch

private const val SELECTION_BAR_MS = 220

/** Where the add-to-playlist flow is (design D8). */
private sealed interface AddFlow {
    data object Idle : AddFlow
    data object Sheet : AddFlow
    data object Naming : AddFlow
    data class Duplicates(val playlist: Playlist, val count: Int) : AddFlow
}

/** Which bar sits at the top: selection wins over an open search, which wins over the title. */
private enum class TopBarMode { Title, Search, Selection }

/**
 * A folder's songs (design canvas artboard 1d). Browse mode: tap plays the folder from that song.
 * Long-press enters selection mode (the "VLC fix", spec F2): checkboxes, a contextual bar with the
 * count, "Select all", close, and "Add N to playlist ›" which runs the add flow against [playlists].
 * Selection is `rememberSaveable`, so it survives scrolling and rotation but not leaving the screen.
 * The search action (change `search`) filters the rows by title or artist; selection, drag-select
 * and "Select all" then work over the rows actually shown, and the query survives a selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    songs: List<Song>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {},
    nowPlayingId: Long? = null,
    playlists: PlaylistsHost = NoPlaylists,
) {
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val selecting = selectedIds.isNotEmpty()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var addFlow by remember { mutableStateOf<AddFlow>(AddFlow.Idle) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val playlistList by playlists.playlists.collectAsStateWithLifecycle()
    val shown = remember(songs, query) { songs.matching(query) }
    val mode = when {
        selecting -> TopBarMode.Selection
        searching -> TopBarMode.Search
        else -> TopBarMode.Title
    }

    fun closeSearch() {
        searching = false
        query = ""
    }

    // Back leaves selection first, then the search, then the screen.
    BackHandler(enabled = selecting) { selectedIds = emptySet() }
    BackHandler(enabled = searching && !selecting) { closeSearch() }

    fun selectedSongs(): List<Song> = songs.filter { it.id in selectedIds }

    fun finishAdd(playlist: Playlist, skipDuplicates: Boolean) {
        val chosen = selectedSongs()
        scope.launch {
            val added = playlists.addSongs(playlist.id, chosen, skipDuplicates)
            selectedIds = emptySet()
            addFlow = AddFlow.Idle
            snackbar.showSnackbar("Added $added to ${playlist.name}")
        }
    }

    fun choosePlaylist(playlist: Playlist) {
        val chosen = selectedSongs()
        scope.launch {
            val dupes = playlists.duplicateCount(playlist.id, chosen)
            if (dupes > 0) addFlow = AddFlow.Duplicates(playlist, dupes) else finishAdd(playlist, skipDuplicates = false)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_folder_detail"),
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.testTag("folder_snackbar")) },
        topBar = {
            // The contextual bar slides down over the title bar and back up (design D7 of `polish`).
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(SELECTION_BAR_MS)) + slideInVertically(tween(SELECTION_BAR_MS)) { -it / 2 }) togetherWith
                        (fadeOut(tween(SELECTION_BAR_MS)) + slideOutVertically(tween(SELECTION_BAR_MS)) { -it / 2 })
                },
                label = "top bar",
            ) { bar ->
            when (bar) {
                TopBarMode.Selection -> SelectionBar(
                    count = selectedIds.size,
                    total = shown.size,
                    onClose = { selectedIds = emptySet() },
                    onSelectAll = { selectedIds = shown.mapTo(HashSet()) { it.id } },
                )
                TopBarMode.Search -> SearchTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = ::closeSearch,
                    placeholder = "Search in folder",
                )
                TopBarMode.Title -> TopAppBar(
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
                                text = if (songs.isEmpty()) songCountLabel(0) else "${songCountLabel(songs.size)} · long-press to select",
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
                    actions = {
                        if (songs.isNotEmpty()) {
                            IconButton(onClick = { searching = true }, modifier = Modifier.testTag("folder_search")) {
                                Icon(imageVector = AppIcons.Search, contentDescription = "Search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
            }
        },
        bottomBar = {
            if (selecting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                ) {
                    Button(
                        onClick = { addFlow = AddFlow.Sheet },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_to_playlist"),
                    ) {
                        Text("Add ${selectedIds.size} to playlist  ›")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (songs.isEmpty()) {
            EmptyState(
                icon = AppIcons.Folder,
                title = "Nothing here yet",
                body = "This folder has no songs above the short-audio threshold. Copy music into it, or pick another folder.",
                actionLabel = "Back to folders",
                onAction = onBack,
                modifier = Modifier
                    .padding(padding)
                    .testTag("folder_detail_empty"),
                actionModifier = Modifier.testTag("folder_detail_empty_action"),
            )
        } else if (shown.isEmpty()) {
            SearchEmpty(query = query, modifier = Modifier.padding(padding))
        } else {
            val listState = rememberLazyListState()
            val songIds = remember(shown) { shown.map { it.id } }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .dragSelect(
                        listState = listState,
                        ids = songIds,
                        selected = { selectedIds },
                        onSelection = { selectedIds = it },
                    )
                    .testTag("songs_list"),
            ) {
                items(shown, key = { it.id }, contentType = { "song" }) { song ->
                    val checked = song.id in selectedIds
                    SongRow(
                        song = song,
                        onClick = {
                            if (selecting) {
                                selectedIds = if (checked) selectedIds - song.id else selectedIds + song.id
                            } else {
                                onSongClick(song)
                            }
                        },
                        onLongClick = { if (!selecting) selectedIds = setOf(song.id) },
                        modifier = Modifier.testTag("song_row_${song.id}"),
                        isCurrent = song.id == nowPlayingId,
                        leading = if (selecting) {
                            {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    modifier = Modifier.testTag("song_check_${song.id}"),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    when (val flow = addFlow) {
        AddFlow.Idle -> Unit
        AddFlow.Sheet -> AddToPlaylistSheet(
            playlists = playlistList,
            onChoose = { choosePlaylist(it) },
            onNew = { addFlow = AddFlow.Naming },
            onDismiss = { addFlow = AddFlow.Idle },
        )
        AddFlow.Naming -> NamePlaylistDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                scope.launch {
                    val id = playlists.create(name)
                    finishAdd(Playlist(id, name, 0, 0L, 0L), skipDuplicates = false)
                }
            },
            onDismiss = { addFlow = AddFlow.Idle },
        )
        is AddFlow.Duplicates -> DuplicatesDialog(
            count = flow.count,
            onAddAnyway = { finishAdd(flow.playlist, skipDuplicates = false) },
            onSkip = { finishAdd(flow.playlist, skipDuplicates = true) },
            onDismiss = { addFlow = AddFlow.Idle },
        )
    }
}

/** The navy contextual bar from artboard 1d: close, "N selected", "Select all N". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(count: Int, total: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "$count selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("selection_count"),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose, modifier = Modifier.testTag("selection_close")) {
                Icon(AppIcons.Close, contentDescription = "Close selection")
            }
        },
        actions = {
            TextButton(onClick = onSelectAll, modifier = Modifier.testTag("selection_all")) {
                Text("Select all $total", color = Cyan)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Navy,
            titleContentColor = White,
            navigationIconContentColor = White,
            actionIconContentColor = Cyan,
        ),
        modifier = Modifier.testTag("selection_bar"),
    )
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
