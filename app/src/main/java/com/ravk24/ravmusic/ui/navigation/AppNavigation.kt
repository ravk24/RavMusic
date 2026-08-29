package com.ravk24.ravmusic.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.folders.FoldersScreen
import com.ravk24.ravmusic.ui.permission.AudioPermissionGate
import com.ravk24.ravmusic.ui.playlists.PlaylistsScreen
import com.ravk24.ravmusic.ui.settings.SettingsScreen

/**
 * The application shell: a Navigation 3 back stack, the two-tab bottom bar, and the audio
 * permission gate around the tabs.
 *
 * Back-stack model: the stack is always `[Playlists]` or `[Playlists, Folders]`, with
 * `Settings` optionally pushed on top. That gives the spec'd back behaviour for free —
 * back from Folders lands on Playlists, back from Playlists (stack size 1) leaves the app.
 *
 * Per-tab scroll state is hoisted here rather than left to the Nav3 saveable decorator,
 * because that decorator discards an entry's state when the entry is popped — and switching
 * tabs pops. Hoisting keeps each tab exactly as it was left, across tab switches and rotation.
 */
@Composable
fun AppNavigation(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Playlists)
    val playlistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val foldersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val current: NavKey = backStack.lastOrNull() ?: Playlists
    val showBottomBar = current in TabRoutes

    fun selectTab(tab: NavKey) {
        if (current == tab) return
        backStack.clear()
        backStack.add(Playlists)
        if (tab == Folders) backStack.add(Folders)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
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
                entryProvider = entryProvider {
                    entry<Playlists> {
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            PlaylistsScreen(
                                listState = playlistsListState,
                                onOpenSettings = { backStack.add(Settings) },
                            )
                        }
                    }
                    entry<Folders> {
                        AudioPermissionGate(
                            state = permissionState,
                            onRequestPermission = onRequestPermission,
                            onOpenAppSettings = onOpenAppSettings,
                        ) {
                            FoldersScreen(listState = foldersListState)
                        }
                    }
                    entry<Settings> {
                        SettingsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                },
            )
        }
    }
}
