## 1. Matching and data

- [x] 1.1 Create `data/model/SongQuery.kt` (`normaliseQuery`, `isFiltering`, `matchesQuery`, `List<Song>.matching`, `List<PlaylistTrack>.matching`) and `data/model/PlaylistSearch.kt` (`PlaylistSearchHit`, `searchPlaylists`); extract `planPlaylistPlay` into `PlaylistModels.kt`; verify `SongQueryTest`, `PlaylistSearchTest` and the `PlaylistModelsTest` play-plan test pass
- [x] 1.2 Add `PlaylistDao.observeAllTracks()`, `PlaylistStore.allTracks`, `PlaylistsHost.allTracks` (ViewModel, `NoPlaylists`, both test fakes); verify `PlaylistDaoTest.allTracksSpanPlaylistsInOrderAndFollowMovesAndDeletes` and `PlaylistsViewModelTest` "allTracks mirrors every playlist"

## 2. Shared UI

- [x] 2.1 Add `AppIcons.Search`, `ui/components/SearchTopBar.kt` (`SearchTopBar`, `SearchEmpty`), `SongRow.subtitle` / `trailing`, and `enabled` on `ReorderableList` / `reorderEnabled` on `ReorderableTrackList` (handle hidden when off); verify `assembleDebug` is green and `QueueSheet` still reorders (`NowPlayingScreenTest`)

## 3. Screens

- [x] 3.1 Playlist detail: search action, bar swap, filtered `ReorderableTrackList` with reordering off, tap mapped to the full-list index, `SearchEmpty`, back closes the search; verify `PlaylistDetailScreenTest.searchFiltersRowsMapsTheIndexAndKeepsSwipe` and `searchMatchesArtistShowsNoMatchAndClearsOrCloses`
- [x] 3.2 Folder detail: three-way top bar (title / search / selection), filtered rows, drag-select and "Select all" over the shown rows, back leaves selection then search; verify `FolderDetailScreenTest.searchFiltersSongsAndTapPlaysTheMatch` and `FolderDetailSelectionTest.selectAllWhileFilteringSelectsShownOnlyAndBackKeepsTheQuery`; the existing selection tests still pass
- [x] 3.3 Playlists home search action, `Search` route, `PlaylistSearchScreen` (hint / hits with "Artist · Playlist" / no-match, open-playlist action), `AppNavigation` entry using `searchPlaylists` + `planPlaylistPlay`, `PlaylistDetail` entry refactored onto `planPlaylistPlay`; verify `PlaylistsScreenTest.searchActionCallsBack`, `PlaylistSearchScreenTest`, `PlaylistSearchNavigationTest` (no bottom bar, plays the right playlist from the hit, detail above search, back keeps the query, back again lands on Playlists)

## 4. Integration, ship and docs

- [x] 4.1 Bump to versionCode 4 / 1.1.0; run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify `BUILD SUCCESSFUL`, every test passes, merged manifest still 0 hits for `INTERNET`
- [x] 4.2 Manual walkthrough on API 36: filter in a playlist (handles disappear, swipe removes, tap plays the right song), filter in a folder then long-press / select all, global search → play → Now Playing origin is the playlist name → back keeps the query → open playlist → back; rotate with a query typed; note deviations here
  — *2026-08-30, run on the API 26 emulator via adb/uiautomator:* Download folder → Search → "gam" left only "gamma"; long-press gave "1 selected" / "Select all 1" (the filtered count); "Add 1 to playlist › New playlist › Late night" ended selection with "Added 1 to Late night" and the search bar still holding "gam". Playlists → Search showed the hint, "gam" listed "gamma — Unknown artist · Late night", tapping it started playback (session `state=3`, mini player up), "Open playlist" opened the "Late night" detail (drag handle present, unfiltered), back returned to the search with "gam" and the hit, back again landed on Playlists with the bottom bar. Note: with the search field focused the first system back only dismisses the keyboard — expected Android behaviour. The playlist-filter swipe/handle behaviour and the rotation case are covered by `PlaylistDetailScreenTest` / the `rememberSaveable` state rather than this walkthrough.
- [x] 4.3 `assembleRelease` → `app/release/RavMusic-1.1.0.apk` (new intent filters visible in `aapt dump badging`, still no `INTERNET`); update `Build-plan/decisions.md` (D-59), `Build-plan/README.md`, root `README.md`; commit on `main`, push, archive `open-with` and `search`; verify `git status` is clean
