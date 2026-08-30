## Context

See proposal.md — Why. After 1.0.2 there is no search anywhere: no text field outside the name-a-playlist dialog, no `AppIcons.Search`. Playlist detail renders `ReorderableTrackList` (drag handle + swipe) and reports row taps as an index into the full track list; folder detail renders a `LazyColumn` with `dragSelect` over the song ids and a title bar that `AnimatedContent` swaps for the selection bar; the Playlists home has a hand-rolled header row; `PlaylistDao` only exposes per-playlist track flows; the `PlaylistDetail` nav entry holds the "drop missing, find the start index" play logic inline. Requirements are in the `playlists`, `folder-browser`, `multi-select` and `app-shell` deltas.

Constraints: strings inline, icons via `AppIcons` only, no album art, test tags on everything, pure logic in `data/model` with JVM tests.

## Goals / Non-Goals

**Goals:**
- One matching rule, one search bar, one empty state — the three screens differ only in what they filter.
- Filtering never corrupts a playlist: indices reported upward are always indices into the full list.
- Global search is cheap: one Room flow joined in memory with the playlist names.

**Non-Goals:**
- Library-wide search, fuzzy matching, history, queue-sheet search.

## Decisions

### D1. Pure matching in `data/model/SongQuery.kt`
`matchesQuery(title, artist?, query)`: trimmed query, case-insensitive `contains` on title or artist, blank matches everything; `List<Song>.matching` / `List<PlaylistTrack>.matching` return the same list for a blank query so nothing recomposes needlessly. `isFiltering(query)` is the single source of "is a filter active".

### D2. `SearchTopBar` replaces the title bar in place
A `TopAppBar` whose title is a borderless `TextField` (auto-focused, `ImeAction.Search` hides the keyboard), back arrow = close (`search_close`), clear action while text exists (`search_clear`), tags shared by every screen. `SearchEmpty` renders `No songs match “<q>”`. Search state (`searching`, `query`) is `rememberSaveable` per screen; a `BackHandler` closes the search before the screen.
*Alternative:* Material `SearchBar` — a docked/expanding component with its own layout rules; heavier than a bar swap and harder to test.

### D3. Playlist detail: filtering disables reordering, keeps swipe, maps the tap
`ReorderableList` gains `enabled`; `ReorderableTrackList` hides the drag handle entirely when `reorderEnabled` is false, so `onMove(from, to)` can only ever carry full-list indices. Swipe-to-remove is keyed by track id and stays. The row tap maps the shown index back with `tracks.indexOf(shown[i])` at tap time, so a list changing underneath is still handled. Play / Shuffle play ignore the filter (they play the playlist, not the view).
*Alternative:* translate `onMove` indices through the filter — ambiguous when the dragged row passes hidden rows.

### D4. Folder detail: three-way top bar, selection over the shown rows
`TopBarMode { Title, Search, Selection }` with Selection winning: entering selection from a filtered list keeps the query and returns to the search bar afterwards. `dragSelect(ids = shownIds)` and "Select all N" use the filtered list, so N is what the user sees; songs selected before narrowing stay selected (the count may exceed the rows shown — acceptable and stated). Two `BackHandler`s: selection first, then search.

### D5. Global search is a route, not a mode of the home screen
`Search : NavKey` pushed above `Playlists`: the bottom bar hides through the unchanged `TabRoutes` rule (the keyboard needs the room), system and predictive back pop it with no root `BackHandler`, `PlaylistDetail` can be pushed above it and the Nav3 saveable decorator keeps the query while the entry is on the stack, and the hoisted grid scroll state is untouched. A tab switch clears it like any detail.
*Alternative:* an inline mode on `PlaylistsScreen` — the FAB, grid scroll and bottom bar all need special-casing.

### D6. Data: one flow of every track, joined in memory
`PlaylistDao.observeAllTracks()` (`ORDER BY playlistId, position, id`; a query only, no schema version bump) → `PlaylistStore.allTracks` → `PlaylistsHost.allTracks` (`stateIn`, `WhileSubscribed`). `searchPlaylists(playlists, tracks, query)` filters, drops tracks whose playlist is gone, sorts by home-grid order then position and attaches the playlist name. A blank query yields nothing — the screen shows a hint rather than every track on the device.

### D7. `planPlaylistPlay` is the one play rule
The `PlaylistDetail` entry's inline "filter missing, start at the tapped URI" becomes `planPlaylistPlay(tracks, missing, tappedUri): PlaylistPlayPlan?` in `PlaylistModels.kt`, used by the detail (Play, Shuffle play, row tap) and by a search hit (that playlist's tracks, start at the hit's URI, origin = the playlist name).

### D8. Row reuse
`SongRow` gains `subtitle` (overrides the artist line: "Artist · Playlist") and `trailing` (the "Open playlist" icon button), so search hits are the same row as everywhere else.

### D9. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  data/model/SongQuery.kt            matching rule
  data/model/PlaylistSearch.kt       PlaylistSearchHit, searchPlaylists
  data/model/PlaylistModels.kt       + PlaylistPlayPlan, planPlaylistPlay
  data/db/PlaylistDao.kt             + observeAllTracks
  data/repo/PlaylistRepository.kt    + allTracks
  PlaylistsViewModel.kt              + PlaylistsHost.allTracks
  ui/components/SearchTopBar.kt      SearchTopBar, SearchEmpty
  ui/components/AppIcons.kt          + Search
  ui/components/SongRow.kt           + subtitle, trailing
  ui/playlists/ReorderableList.kt    + enabled / reorderEnabled
  ui/playlists/PlaylistDetailScreen.kt, PlaylistsScreen.kt, PlaylistSearchScreen.kt
  ui/folders/FolderDetailScreen.kt
  ui/navigation/Routes.kt, AppNavigation.kt
```

## Risks / Trade-offs

- [Filtered playlist looks reorder-less] → intended and stated in the spec; clearing the filter brings the handles back.
- [A song present twice in one playlist] → `planPlaylistPlay` starts at the first occurrence, as the detail always has.
- [Global search over thousands of tracks] → a `contains` scan on every keystroke is microseconds at playlist scale; `remember(list, all, query)` keeps it off the render path otherwise.

## Migration Plan

No persisted data changes. Rollback is `git revert`.
