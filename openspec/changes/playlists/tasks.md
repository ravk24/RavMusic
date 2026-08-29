## 1. Build

- [ ] 1.1 Add `room = "2.8.4"` and `ksp = "2.3.11"` to `gradle/libs.versions.toml` with `androidx-room-runtime`, `androidx-room-compiler`, `androidx-room-testing` and the `ksp` / `room` plugins; apply both plugins in `app/build.gradle.kts` with `room { schemaDirectory("$projectDir/schemas") }`, `implementation(room.runtime)`, `ksp(room.compiler)`, `androidTestImplementation(room.testing)`; verify `.\gradlew.bat assembleDebug` is green (if KSP 2.3.11 fails to apply with built-in Kotlin, pin `2.2.10-2.0.2` and record it in `Build-plan/decisions.md`)

## 2. Data

- [ ] 2.1 Create `data/db/PlaylistEntity.kt`, `PlaylistTrackEntity.kt` (FK cascade, index on playlistId+position), `PlaylistDao.kt` (observe playlists with count/total, observe tracks, insert/rename/delete playlist, existing URIs, max position, insert tracks, delete track(s), transactional position update) and `RavMusicDatabase.kt` (version 1, exportSchema); verify the build generates `app/schemas/com.ravk24.ravmusic.data.db.RavMusicDatabase/1.json`
- [ ] 2.2 Create `data/model/PlaylistModels.kt`: `Playlist`, `PlaylistTrack`, `PlaylistTrack.toSong()`, `missingTrackIds(tracks, libraryState)`, `partitionDuplicates(songs, existingUris)`, `moveItem(list, from, to)`, and `formatTotalDuration(ms)` in `ui/components/Format.kt`; verify JVM tests `PlaylistModelsTest`, `MissingTracksTest`, `PartitionDuplicatesTest`, `MoveItemTest`, `FormatTest` additions
- [ ] 2.3 Create `data/repo/PlaylistRepository.kt` over the DAO (`playlists`, `tracks(id)`, `create`, `rename`, `delete`, `duplicateCount`, `addSongs(skipDuplicates)`, `removeTrack`, `move`, `cleanUp`) and build the database + repository in `AppContainer`; verify the app launches with no Room exception in logcat
- [ ] 2.4 Write the instrumented `PlaylistDaoTest` on `Room.inMemoryDatabaseBuilder`: create/rename/delete cascade, positions append, duplicate lookup, reorder persists, flows emit updates; verify it passes on the API 36 emulator

## 3. Player

- [ ] 3.1 Add `shuffle` to `PlayerBridge.play`, set `shuffleModeEnabled` in `PlayerConnection`, add `PlayerViewModel.playSongs(…, shuffle)` and `shufflePlay(songs, origin)` with a random start index; verify `PlayerViewModelTest` covers shuffle flag and random index within range, and `PlaybackServiceTest` gains a check that `shuffleModeEnabled` follows the flag

## 4. ViewModel and routes

- [ ] 4.1 Create `PlaylistsHost` and `PlaylistsViewModel` (Activity-scoped; per-id `stateIn` track flows); verify `PlaylistsViewModelTest` with a fake repository: create returns the id, addSongs with skipDuplicates uses the partition, cleanUp removes missing ids
- [ ] 4.2 Add `PlaylistDetail(playlistId)` to `Routes.kt`; give `AppNavigation` a `playlists: PlaylistsHost` parameter and `onPlayPlaylist(songs, index, origin, shuffle)`; wire `PlaylistsViewModel` in `AppRoot`; update existing shell tests with a `FakePlaylistsHost`; verify `AppNavigationTest`, `TabStateRetentionTest`, `LibraryNavigationTest`, `MiniPlayerDockingTest` still pass

## 5. Playlists UI

- [ ] 5.1 Add `AppIcons.DragHandle`, `Shuffle`, `Add`, `Close`, `Check`, `Delete`; verify they render in a preview
- [ ] 5.2 Rewrite `ui/playlists/PlaylistsScreen.kt` (header with count, two-column `PlaylistCard` grid, FAB → `NewPlaylistDialog`, empty state, overflow kept) and delete `PlaceholderList.kt`; verify `PlaylistsScreenTest`: cards with name/meta, count text, empty state, FAB opens dialog, blank name disabled, confirm calls create
- [ ] 5.3 Create `ui/playlists/PlaylistDetailScreen.kt` (header, Shuffle play / Play, missing banner + Clean up, overflow Rename/Delete with confirm, empty hint); verify `PlaylistDetailScreenTest`: header texts, buttons call back with the right shuffle flag and start index, banner appears with missing ids and Clean up calls back, rename/delete flows
- [ ] 5.4 Create `ui/playlists/ReorderableList.kt` (long-press drag on the handle → `onMove(from, to)`, `SwipeToDismissBox` → `onRemove`, `isCurrent` and missing greying on rows); verify `PlaylistDetailScreenTest` swipe removes a row and a drag from row 2 to row 1 reports `onMove(1, 0)`

## 6. Multi-select

- [ ] 6.1 Add selection mode to `FolderDetailScreen` (`rememberSaveable` ids, long-press enters, tap toggles, checkboxes, contextual bar with count / Select all / close, `BackHandler`, "Add N to playlist ›" button); verify `FolderDetailSelectionTest`: long-press → "1 selected", tap → "2 selected", select all → "N selected", close → normal bar, back exits selection, tap in selection does not call play
- [ ] 6.2 Create `AddToPlaylistSheet`, `NewPlaylistDialog`, `RenamePlaylistDialog`, `DeletePlaylistDialog`, `DuplicatesDialog` in `ui/playlists/PlaylistDialogs.kt` and wire the add flow (sheet → optional name → duplicate prompt → add → snackbar → selection cleared); verify a `PlaylistDialogsTest` for the sheet rows, blank-name guard and the duplicate dialog's two choices
- [ ] 6.3 Write `AddToPlaylistFlowTest` over `AppNavigation` with a real `PlaylistsViewModel` on an in-memory database: select 3 in "Music" → Add → New playlist "Late night" → Playlists tab shows the card with "3 songs" → detail lists the 3 in folder order → select the same 3 again → "3 already in this playlist" → Skip duplicates → still 3; verify it passes on both emulators

## 7. Integration and docs

- [ ] 7.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify `BUILD SUCCESSFUL`, every test passes, and the merged manifest still has 0 hits for `INTERNET`
- [ ] 7.2 Manual walkthrough on both emulators: long-press → select two → Add → New playlist → card on home; Shuffle play → mini player + highlight; reorder persists after reopening; swipe removes; rename; `am force-stop` + relaunch keeps the playlist; move a file away + pull-to-refresh → greyed row + Clean up; delete with confirm; `adb reboot` the API 26 emulator → playlist persists; verify each `playlists` / `multi-select` scenario and note deviations here
- [ ] 7.3 Update `Build-plan/README.md` (row 4 ✅), `Build-plan/phases/04-playlists.md` (status, result, task progress) and `Build-plan/decisions.md` (D-30…); commit on `main` and push; verify `git status` is clean and `git log -1` shows the commit
