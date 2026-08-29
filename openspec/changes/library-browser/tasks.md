## 1. Scaffold

- [x] 1.1 Add `kotlinxCoroutines = "1.9.0"` and `kotlinx-coroutines-test` to `gradle/libs.versions.toml` and `testImplementation(libs.kotlinx.coroutines.test)` to `app/build.gradle.kts`; verify `.\gradlew.bat assembleDebug` resolves it with no "Could not find"

## 2. Pure library model

- [x] 2.1 Create `data/model/Song.kt` (`Song` with `uri: String`) and `normaliseArtist()` (null/blank/`<unknown>` → null) plus `data/model/Folder.kt`; verify unit tests for `normaliseArtist`
- [x] 2.2 Create `data/model/LibrarySnapshot.kt` with `buildLibrarySnapshot(songs, scannedAt)` (group by folder id, folders by name case-insensitive then id, songs by title case-insensitive then id) and `songsIn()`/`totalSongs`; verify `LibrarySnapshotTest` covers grouping, order, counts, unknown folder → empty, empty input
- [x] 2.3 Create `data/mediastore/FolderPath.kt` with `folderFromPath(path)` = parent-dir name + `parentPath.lowercase().hashCode().toString()`; verify `FolderPathTest` for nested path, root-level file and a path without separator ("Unknown folder")
- [x] 2.4 Create `ui/components/Format.kt` with `formatDuration(ms)` ("0:00", "0:59", "3:41", "1:02:05", negative → "0:00") and `songCountLabel(n)` ("1 song"/"572 songs"); verify `FormatTest`

## 3. Scanner, repository, ViewModel

- [x] 3.1 Create `data/mediastore/MediaScanner.kt` (`fun interface MediaScanner { fun scan(): List<Song> }`, `MIN_SONG_DURATION_MS = 30_000L`) and `data/repo/LibraryState.kt` (`Idle`, `Loading`, `Loaded(snapshot, refreshing)`); verify they compile
- [x] 3.2 Create `data/mediastore/MediaStoreScanner.kt`: `EXTERNAL_CONTENT_URI`, selection `IS_MUSIC != 0 AND DURATION >= ?`, sort `TITLE COLLATE NOCASE`, projection with bucket columns on API ≥ 29 and `DATA` + `folderFromPath` below, content-URI string ids, title fallback to display name, artist normalised, null cursor/`SecurityException` → empty list; verify `assembleDebug` has no lint errors and `adb logcat` shows no exception from the scanner on API 26 and API 36
- [x] 3.3 Create `data/repo/LibraryRepository.kt` (`StateFlow<LibraryState>`, `ensureLoaded()`, `refresh()` keeping the old snapshot with `refreshing = true`, `clear()`, scan on an injectable IO dispatcher, mutex-coalesced); verify `LibraryRepositoryTest` with a fake scanner and `StandardTestDispatcher`: Idle → Loading → Loaded, second `ensureLoaded` does not rescan, refresh emits `Loaded(old, true)` then `Loaded(new)`, clear → Idle
- [x] 3.4 Create `LibraryViewModel.kt` (`state`, `onPermissionChanged(granted)`, `refresh()`) and a test `MainDispatcherRule`; verify `LibraryViewModelTest`: granted loads once, not-granted clears, refresh rescans
- [x] 3.5 Make `AppContainer(context)` build `LibraryRepository(MediaStoreScanner(contentResolver))`; in `AppRoot` create `LibraryViewModel` via `viewModelFactory`, add `LaunchedEffect(permissionState)` → `onPermissionChanged`, and pass `libraryState` + `onRefreshLibrary` to `AppNavigation`; verify the app launches and, with permission granted, logcat shows exactly one query on cold start

## 4. UI

- [x] 4.1 Create `ui/components/EmptyState.kt` and re-implement `NoMusicFoundScreen` on it with the same signature, strings and test tags; verify `AudioPermissionGateTest` is unchanged and green
- [x] 4.2 Add `AppIcons.ChevronRight`, create `ui/components/SongRow.kt` (title ellipsized, artist or "Unknown artist", duration) and `ui/folders/FolderRow.kt` (tile with Folder icon, name, count, chevron); verify both render in `@Preview`s
- [x] 4.3 Rewrite `ui/folders/FoldersScreen.kt`: `FoldersScreen(state, listState, onRefresh, onOpenFolder)` with loading indicator, empty state (Rescan), header total, `PullToRefreshBox` + `LazyColumn` (keys = folder id, tags `folder_row_<id>`), footer; verify `FoldersScreenTest`: rows in order, total text, footer, Rescan calls `onRefresh`, loading indicator, row click passes the folder
- [x] 4.4 Create `ui/folders/FolderDetailScreen.kt` (`TopAppBar` with back, title + subtitle count, `LazyColumn` of `SongRow` with inert click, empty-folder text); verify `FolderDetailScreenTest`: title/subtitle, "Unknown artist", "3:41", back callback
- [x] 4.5 Add `FolderDetail(folderId, name)` to `Routes.kt`; in `AppNavigation` add `libraryState`/`onRefreshLibrary` params, wire `entry<Folders>` to the new screen and `entry<FolderDetail>` inside `AudioPermissionGate`; verify `LibraryNavigationTest`: folder row → detail with no `bottom_bar`, system back and the back affordance return to Folders with the tab selected, `Denied` permission on detail shows `no_music_found`
- [x] 4.6 Update `AppNavigationTest` (new params) and `TabStateRetentionTest` (scroll a 30-folder fake snapshot instead of the placeholder) and drop the "Library" line from `UpcomingPhases`; verify both tests are green

## 5. Integration and docs

- [x] 5.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify all tasks report `BUILD SUCCESSFUL` and every test passes
- [x] 5.2 Manual walkthrough on both emulators with generated WAVs (35 s, 41 s, 65 s, 5 s) in `/sdcard/Music`, `/sdcard/Music/Rock`, `/sdcard/Download`: folder names/counts match `adb shell content query --uri content://media/external/audio/media`, the 5 s clip is hidden, pull-to-refresh picks up a newly pushed file, detail shows "Unknown artist" and durations without a bottom bar, revoke-and-return on detail shows "No music found"; verify each scenario in `specs/folder-browser/spec.md` and note deviations here
- [x] 5.3 Update `Build-plan/README.md` (row 2 ✅), `Build-plan/phases/02-library.md` (status, result, task progress) and `Build-plan/decisions.md` (D-15…D-23); commit on `main` and push; verify `git status` is clean and `git log -1` shows the commit
