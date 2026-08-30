package com.ravk24.ravmusic.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ravk24.ravmusic.NoSettings
import com.ravk24.ravmusic.PlaylistsHost
import com.ravk24.ravmusic.SettingsHost
import com.ravk24.ravmusic.data.model.OPENED_FILE_ORIGIN
import com.ravk24.ravmusic.data.model.OpenedFile
import com.ravk24.ravmusic.data.model.missingTrackIds
import com.ravk24.ravmusic.data.model.planPlaylistPlay
import com.ravk24.ravmusic.data.model.searchPlaylists
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.folders.FolderDetailScreen
import com.ravk24.ravmusic.ui.folders.FoldersScreen
import com.ravk24.ravmusic.ui.nowplaying.NowPlayingScreen
import com.ravk24.ravmusic.ui.permission.AudioPermissionGate
import com.ravk24.ravmusic.ui.player.MiniPlayer
import com.ravk24.ravmusic.ui.playlists.PlaylistDetailScreen
import com.ravk24.ravmusic.ui.playlists.PlaylistSearchScreen
import com.ravk24.ravmusic.ui.playlists.PlaylistsScreen
import com.ravk24.ravmusic.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * The application shell: a Navigation 3 back stack, the two-tab bottom bar, the docked mini
 * player, and the audio permission gate around every library-backed screen.
 *
 * Back-stack model: the stack is always `[Playlists]` or `[Playlists, Folders]`, optionally with
 * one detail screen on top — `Settings` above either tab, `PlaylistDetail` above Playlists,
 * `FolderDetail` above Folders, `Search` above Playlists with `PlaylistDetail` allowed above it —
 * and `NowPlaying` above any of those (directly above `Playlists`
 * when a file is opened from outside, see [openedFile]). That gives
 * the spec'd back behaviour for free: back from a detail returns to its tab, back from Folders
 * lands on Playlists, back from Playlists (stack size 1) leaves the app. Only tab roots show the
 * bottom bar; the mini player sits above it on every route while a queue is loaded.
 *
 * Per-tab scroll state is hoisted here rather than left to the Nav3 saveable decorator,
 * because that decorator discards an entry's state when the entry is popped — and switching
 * tabs pops. Library and player state are likewise passed in as values: they live in
 * Activity-scoped ViewModels.
 *
 * [openedFile] is a one-shot event: the shell plays it as a single-song queue, reports it
 * handled through [onOpenedFileHandled], and opens Now Playing once the session confirms it is
 * the current song.
 */
@Composable
fun AppNavigation(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    libraryState: LibraryState,
    onRefreshLibrary: () -> Unit,
    playerState: PlayerState,
    player: PlayerActions,
    playlists: PlaylistsHost,
    modifier: Modifier = Modifier,
    settings: SettingsHost = NoSettings,
    openedFile: OpenedFile? = null,
    onOpenedFileHandled: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(Playlists)
    val playlistsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val foldersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // A skipped file is announced once per notice (design D5 of `polish`); the guard survives rotation.
    var shownSkipSeq by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(playerState.skipped?.seq) {
        val notice = playerState.skipped ?: return@LaunchedEffect
        if (notice.seq <= shownSkipSeq) return@LaunchedEffect
        shownSkipSeq = notice.seq
        snackbar.showSnackbar("Couldn't play ${notice.title} — skipped")
    }

    // Open-with: play the file, then wait for the session to report it as the current song before
    // showing Now Playing — that screen closes itself while no queue is loaded, so pushing it
    // before the controller has caught up would pop it straight away. A file that cannot be
    // played never becomes current: the queue is cleared, the skip notice above shows, and the
    // pending id is simply overwritten by the next request.
    var awaitingOpenId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(openedFile?.seq) {
        val opened = openedFile ?: return@LaunchedEffect
        awaitingOpenId = opened.song.id
        player.onPlaySongs(listOf(opened.song), 0, OPENED_FILE_ORIGIN)
        onOpenedFileHandled()
    }
    LaunchedEffect(playerState.nowPlaying?.songId, awaitingOpenId) {
        val id = awaitingOpenId ?: return@LaunchedEffect
        if (playerState.nowPlaying?.songId == id && playerState.hasQueue) {
            if (backStack.lastOrNull() != NowPlaying) backStack.add(NowPlaying)
            awaitingOpenId = null
        }
    }

    val current: NavKey = backStack.lastOrNull() ?: Playlists
    val showBottomBar = current in TabRoutes
    val onNowPlaying = current == NowPlaying

    fun selectTab(tab: NavKey) {
        if (current == tab) return
        backStack.clear()
        backStack.add(Playlists)
        if (tab == Folders) backStack.add(Folders)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.testTag("shell_snackbar")) },
        bottomBar = {
            // Mini player above the navigation bar on tabs; alone at the bottom on detail screens,
            // where it takes over the navigation-bar inset the NavigationBar would otherwise consume.
            // Nothing at all while the full-screen player is open.
            if (!onNowPlaying) Column(
                modifier = if (showBottomBar) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                if (playerState.hasQueue) {
                    MiniPlayer(
                        state = playerState,
                        onPlayPause = player.onPlayPause,
                        onExpand = { backStack.add(NowPlaying) },
                        onDismiss = player.onDismiss,
                    )
                }
                if (showBottomBar) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NavigationBarItem(
                            selected = current == Playlists,
                            onClick = { selectTab(Playlists) },
                            icon = { Icon(AppIcons.QueueMusic, contentDescription = null) },
                            label = { Text("Playlists") },
                            colors = itemColors,
                            modifier = Modifier.testTag("tab_playlists"),
                        )
                        NavigationBarItem(
                            selected = current == Folders,
                            onClick = { selectTab(Folders) },
                            icon = { Icon(AppIcons.Folder, contentDescription = null) },
                            label = { Text("Folders") },
                            colors = itemColors,
                            modifier = Modifier.testTag("tab_folders"),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = NavTransitions.push,
                popTransitionSpec = NavTransitions.pop,
                predictivePopTransitionSpec = NavTransitions.predictivePop,
                entryProvider = entryProvider {
                    entry<Playlists> {
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            val list by playlists.playlists.collectAsStateWithLifecycle()
                            PlaylistsScreen(
                                playlists = list,
                                gridState = playlistsGridState,
                                onOpenSettings = { backStack.add(Settings) },
                                onOpenPlaylist = { backStack.add(PlaylistDetail(it.id)) },
                                onCreate = { name -> scope.launch { playlists.create(name) } },
                                onOpenSearch = { backStack.add(Search) },
                            )
                        }
                    }
                    entry<Search> {
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            val list by playlists.playlists.collectAsStateWithLifecycle()
                            val all by playlists.allTracks.collectAsStateWithLifecycle()
                            // Entry-scoped saved state: the query survives rotation and a detail pushed above.
                            var query by rememberSaveable { mutableStateOf("") }
                            val hits = remember(list, all, query) { searchPlaylists(list, all, query) }
                            val missing = remember(all, libraryState) { missingTrackIds(all, libraryState) }
                            PlaylistSearchScreen(
                                query = query,
                                onQueryChange = { query = it },
                                hits = hits,
                                missingIds = missing,
                                nowPlayingId = playerState.nowPlaying?.songId,
                                onBack = { backStack.removeLastOrNull() },
                                onPlayHit = { hit ->
                                    val own = all.filter { it.playlistId == hit.track.playlistId }
                                    planPlaylistPlay(own, missing, hit.track.uri)?.let { plan ->
                                        player.onPlaySongs(plan.songs, plan.startIndex, hit.playlistName)
                                    }
                                },
                                onOpenPlaylist = { backStack.add(PlaylistDetail(it)) },
                            )
                        }
                    }
                    entry<Folders> {
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            FoldersScreen(
                                state = libraryState,
                                listState = foldersListState,
                                onRefresh = onRefreshLibrary,
                                onOpenFolder = { folder -> backStack.add(FolderDetail(folder.id, folder.name)) },
                            )
                        }
                    }
                    entry<FolderDetail> { key ->
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            val songs = (libraryState as? LibraryState.Loaded)
                                ?.snapshot
                                ?.songsIn(key.folderId)
                                .orEmpty()
                            FolderDetailScreen(
                                folderName = key.name,
                                songs = songs,
                                onBack = { backStack.removeLastOrNull() },
                                onSongClick = { song -> player.onPlaySongs(songs, songs.indexOf(song), key.name) },
                                nowPlayingId = playerState.nowPlaying?.songId,
                                playlists = playlists,
                            )
                        }
                    }
                    entry<PlaylistDetail> { key ->
                        val list by playlists.playlists.collectAsStateWithLifecycle()
                        val tracks by playlists.tracks(key.playlistId).collectAsStateWithLifecycle()
                        val playlist = list.firstOrNull { it.id == key.playlistId }
                        val missing = remember(tracks, libraryState) { missingTrackIds(tracks, libraryState) }
                        val origin = playlist?.name ?: "Playlist"
                        PlaylistDetailScreen(
                            playlist = playlist,
                            tracks = tracks,
                            missingIds = missing,
                            nowPlayingId = playerState.nowPlaying?.songId,
                            onBack = { backStack.removeLastOrNull() },
                            onPlay = { index ->
                                planPlaylistPlay(tracks, missing, tracks.getOrNull(index)?.uri)?.let { plan ->
                                    player.onPlaySongs(plan.songs, plan.startIndex, origin)
                                }
                            },
                            onShufflePlay = {
                                planPlaylistPlay(tracks, missing, null)?.let { plan -> player.onShufflePlay(plan.songs, origin) }
                            },
                            onRename = { playlists.rename(key.playlistId, it) },
                            onDelete = {
                                playlists.delete(key.playlistId)
                                backStack.removeLastOrNull()
                            },
                            onRemoveTrack = playlists::removeTrack,
                            onMove = { from, to -> playlists.move(key.playlistId, from, to) },
                            onCleanUp = { playlists.cleanUp(key.playlistId, missing) },
                            onOpenFolders = {
                                backStack.removeLastOrNull()
                                selectTab(Folders)
                            },
                        )
                    }
                    entry<NowPlaying>(metadata = NavTransitions.playerMetadata) {
                        // The screen has nothing to show once the queue is gone: close it.
                        LaunchedEffect(playerState.hasQueue) {
                            if (!playerState.hasQueue && backStack.lastOrNull() == NowPlaying) backStack.removeLastOrNull()
                        }
                        NowPlayingScreen(
                            state = playerState,
                            actions = player,
                            onCollapse = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Settings> {
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            settings = settings,
                            libraryState = libraryState,
                            onRescan = onRefreshLibrary,
                        )
                    }
                },
            )
        }
    }
}
