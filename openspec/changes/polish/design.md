## Context

See proposal.md — Why. Current state after `sleep-timer`: `RavMusicTheme(darkTheme)` already takes an override but `MainActivity` never passes one and nothing is persisted (no DataStore, no SharedPreferences); `SettingsScreen` is a stub with a footer; `MediaStoreScanner(minDurationMs = MIN_SONG_DURATION_MS)` filters in the MediaStore selection but the value is fixed at construction in `AppContainer`; `LibrarySnapshot` already records `scannedAt` and `totalSongs`; playlists already grey out and skip missing tracks (`missingTrackIds`, `PlaybackService.MissingFileSkipper`) but the folder browser only notices deletions on a manual rescan and the skip is silent; `EmptyState` exists but the folder and playlist details use a plain `Text`; `NavDisplay` runs with default (instant) transitions; selection lives as `selectedIds` in `FolderDetailScreen` and `ReorderableList` is the only drag gesture. Requirements: `specs/settings/spec.md` plus the deltas in this change.

Constraints: manual DI (`AppContainer`), fixed palette, no `INTERNET`, Nav3 1.1.7, Compose BOM 2026.08.00, minSdk 26, both emulators (API 36 and API 26) must pass.

## Goals / Non-Goals

**Goals:**
- Settings are a tiny, boring, persisted key-value store; everything that reads them does so through a `Flow`.
- The threshold is applied where the query runs, so a change is one rescan away and the snapshot can say what it used.
- Deleted files stop needing a manual rescan; the one place that must still cope with them at runtime (the player) tells the user.
- Motion is declared, not hand-animated: Nav3 transition specs and one `AnimatedContent`.
- Drag-select is a pure range function plus one gesture modifier; the row long-press that exists today keeps working unchanged.

**Non-Goals:**
- Embedded album art (D-02 stands); a `ContentObserver` on MediaStore; per-folder thresholds; theming beyond light/dark; shared-element transitions; drag-select in the playlist detail.

## Decisions

### D1. Settings live in Preferences DataStore behind `SettingsRepository`
`data/settings/SettingsRepository(dataStore: DataStore<Preferences>)` exposes `themeMode: Flow<ThemeMode>` and `minDurationMs: Flow<Long>` (defaults `SYSTEM` / `MIN_SONG_DURATION_MS`) and `suspend fun setThemeMode / setMinDuration`. `AppContainer` owns `Context.dataStore by preferencesDataStore("settings")`. JVM tests build a store with `PreferenceDataStoreFactory.create(scope, produceFile = { tempFile })`. `ThemeMode { SYSTEM, LIGHT, DARK; fun resolve(systemDark: Boolean): Boolean }` is a pure enum in the same package.
*Alternatives:* a Room table — heavier than two keys; `SharedPreferences` — no Flow, and the deprecation path leads here anyway.

### D2. The threshold is read at scan time
`MediaScanner` becomes `fun scan(minDurationMs: Long): List<Song>`; `MediaStoreScanner` loses its constructor parameter. `LibraryRepository(scanner, minDurationMs: suspend () -> Long = { MIN_SONG_DURATION_MS })` calls the provider inside `refresh()`; `AppContainer` passes `{ settingsRepository.minDurationMs.first() }`. `LibrarySnapshot` gains `minDurationMs` so the Folders footer, the empty-library hint and Settings all describe the value that was actually applied. `SettingsViewModel.setMinDuration(ms)` persists, then calls `libraryRepository.refresh()` in the same coroutine — one rescan per change, never a race with the flow.

### D3. The theme override is resolved once, in `MainActivity`
`setContent { val mode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM); val dark = mode.resolve(isSystemInDarkTheme()); SystemBars(dark); RavMusicTheme(darkTheme = dark) { AppRoot() } }`. `SystemBars` is a `LaunchedEffect(dark)` that calls `enableEdgeToEdge(statusBarStyle, navigationBarStyle)` again with `SystemBarStyle.dark(TRANSPARENT)` or `.light(TRANSPARENT, TRANSPARENT)` — the call is documented as re-invocable and covers API 26 (light navigation-bar flag). `RavMusicTheme` and `ThemeTest` are untouched. The first frame after a cold start may use the system palette until DataStore emits (a few ms); accepted.

### D4. `SettingsHost` mirrors `PlaylistsHost`
`interface SettingsHost { val themeMode: StateFlow<ThemeMode>; val minDurationMs: StateFlow<Long>; fun setThemeMode(ThemeMode); fun setMinDuration(Long) }`, implemented by an Activity-scoped `SettingsViewModel(settingsRepository, libraryRepository)` built in `AppRoot`; `NoSettings` for previews, `FakeSettingsHost` for tests. `AppNavigation` takes `settings: SettingsHost` and hands the Settings entry `libraryState` + `onRefreshLibrary` it already has, so "Rescan" is the same `LibraryViewModel.refresh()` as pull-to-refresh.

### D5. Deleted files: re-query on resume, and a broadcast skip notice
- `AppRoot`'s existing `LifecycleEventEffect(ON_RESUME)` also calls `LibraryViewModel.refreshIfLoaded()` (no-op unless the library is `Loaded`). Files can only be deleted while the app is away, so resume is exactly when the snapshot may be stale; the query takes milliseconds and the old list stays visible (`refreshing = true`) — no `ContentObserver` lifecycle to manage.
- `MissingFileSkipper` gets the `MediaSession` and, before skipping, calls `session.broadcastCustomCommand(SessionCommand(PlaybackEvents.SKIPPED_MISSING, Bundle.EMPTY), bundleOf(ARG_TITLE to title))`. `PlayerConnection`'s `MediaController.Listener.onCustomCommand` maps it into `PlayerState.skipped: SkipNotice?` (`title`, `seq` — a counter so the same title twice notifies twice). `AppNavigation`'s `Scaffold` gains a `SnackbarHost`; a `LaunchedEffect(playerState.skipped?.seq)` shows "Couldn't play <title> — skipped". Only connected controllers hear the broadcast, which is what we want: no UI, no snackbar.
*Alternatives:* `Player.Listener.onPlayerError` on the controller — the session can coalesce error → prepare → play into one update and the UI would miss it.

### D6. `EmptyState` gets an optional action
`actionLabel: String? = null` (the button is omitted when null). Folder detail: icon `Folder`, "Nothing here yet", body about copying music into the folder, action "Back to folders" → `onBack`. Playlist detail: icon `QueueMusic`, "No songs yet", body "Open a folder, long-press songs and add them here.", action "Open Folders" → new `onOpenFolders` that the shell turns into a tab switch (`selectTab(Folders)` after popping the detail). Test tags `folder_detail_empty` / `playlist_empty` move onto the `EmptyState` root.

### D7. Motion is Nav3 transition metadata plus one `AnimatedContent`
`ui/navigation/NavTransitions.kt` holds the specs. `NavDisplay(transitionSpec = fade + slight horizontal slide, popTransitionSpec = mirrored, predictivePopTransitionSpec = mirrored)`. The `NowPlaying` entry carries `metadata = NavDisplay.transitionSpec { slideInVertically { it } togetherWith ExitTransition.KeepUntilTransitionsFinished; targetContentZIndex = 1f } + NavDisplay.popTransitionSpec { EnterTransition.None togetherWith slideOutVertically { it } } + NavDisplay.predictivePopTransitionSpec { … same … }` so the player slides up over the current screen and down off it. `<application android:enableOnBackInvokedCallback="true">` turns on system predictive back (API 33+; a no-op below). In `FolderDetailScreen` the `topBar` slot becomes `AnimatedContent(targetState = selecting)` with a vertical slide + fade. Durations 220–300 ms; nothing bounces.

### D8. Drag-select is a pure range plus a list-level gesture read in the Initial pass
`ui/folders/RangeSelection.kt`: `rangeSelection(base: Set<Long>, ids: List<Long>, anchor: Int, current: Int): Set<Long>` = `base ∪ ids[min..max]` — recomputed from `base` (the selection when the drag started) on every move, so dragging back shrinks the range; `rowIndexAt(y: Float, rows: List<RowBounds>): Int?` maps a pointer y to a visible row (`RowBounds(index, top, height)` from `LazyListState.layoutInfo.visibleItemsInfo`). The `LazyColumn` gets `Modifier.pointerInput(songs) { awaitEachGesture { val down = awaitFirstDown(requireUnconsumed = false); val lp = awaitLongPressOrCancellation(down.id) ?: return; anchor = rowIndexAt(lp.position.y); base = selectedIds; select anchor; while (true) { val e = awaitPointerEvent(PointerEventPass.Initial); … update via rangeSelection …; if all changes are up → break } } }`. Reading in the Initial pass means the row's own `combinedClickable` long-press still fires (so entering selection, semantics and the existing tests are unchanged) and its `consumeUntilUp` in the Main pass keeps the list from scrolling during the drag. Fallback if the two long-press timers misbehave on API 26: the list owns the long-press and rows get `onLongClick = null`.

### D9. File layout
```
gradle/libs.versions.toml, app/build.gradle.kts         + androidx.datastore:datastore-preferences 1.1.7
app/src/main/AndroidManifest.xml                         enableOnBackInvokedCallback
app/src/main/java/com/ravk24/ravmusic/
  data/settings/{ThemeMode,SettingsRepository}.kt         new
  data/mediastore/{MediaScanner,MediaStoreScanner}.kt     scan(minDurationMs)
  data/repo/LibraryRepository.kt, data/model/LibrarySnapshot.kt   threshold provider / recorded threshold
  LibraryViewModel.kt                                     refreshIfLoaded()
  SettingsViewModel.kt                                    SettingsHost, SettingsViewModel, NoSettings
  RavMusicApp.kt                                          dataStore, settingsRepository, provider wiring
  MainActivity.kt                                         theme resolution, SystemBars, SettingsViewModel, resume rescan
  playback/PlaybackEvents.kt                              SKIPPED_MISSING + ARG_TITLE
  playback/{PlaybackService,PlayerConnection,PlayerState}.kt   broadcast, listener, SkipNotice
  ui/settings/SettingsScreen.kt                           rewritten
  ui/components/{EmptyState,Format}.kt                    optional action; formatScanTime, thresholdLabel
  ui/navigation/{NavTransitions,AppNavigation}.kt         specs, metadata, SnackbarHost, SettingsHost, onOpenFolders
  ui/folders/{RangeSelection,FolderDetailScreen,FoldersScreen}.kt   drag-select, AnimatedContent, empty state, footer
  ui/playlists/PlaylistDetailScreen.kt                    empty state
```

## Risks / Trade-offs

- [DataStore pulls newer coroutines/stdlib] → check `:app:dependencies`; D-23 pins coroutines 1.9.0, Kotlin is 2.2.10 — 1.1.7 is built against older versions of both.
- [Rescan on every resume] → a MediaStore query on ~thousands of rows is tens of milliseconds on a background dispatcher and the UI keeps the previous snapshot; a `refreshing` flag already exists.
- [Two long-press detectors on the same touch] → both use the platform timeout; if ordering proves flaky on API 26, D8's fallback removes the row's detector.
- [Predictive back vs `BackHandler(enabled = selecting)`] → the handler wins while selecting (no preview), which is the desired behaviour.
- [`enableEdgeToEdge` re-invocation] → documented as safe to call repeatedly; verified visually on both APIs.
- [First frame in the system palette] → accepted; the alternative (blocking `runBlocking` read in `onCreate`) is worse.

## Migration Plan

No persisted-data migration: the DataStore file is created on first use with defaults equal to today's behaviour (System theme, 30 s threshold). Rollback is `git revert`; the leftover `settings.preferences_pb` is harmless.
