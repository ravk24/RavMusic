## Why

Phases 1–6 built every feature in the spec; what is left is the finish that separates "works" from "feels done". Spec F8 asks for a manual dark-mode override in a minimal Settings screen and a tweakable short-audio threshold; F1's edge cases ask that files deleted from storage disappear or grey out everywhere and are skipped without confusion; F2 lists drag-select as the "phase 2 of polish" stretch goal; and every list still has at least one plain-text placeholder instead of a real empty state. The Settings route has been a stub since Phase 1 waiting for exactly this change.

## What Changes

- **Settings screen** (artboard 1g) replaces the stub: **Theme** System / Light / Dark, persisted on the device and applied immediately (system bars included); **Skip short audio** threshold (Off / 15 s / 30 s / 1 min / 2 min) that re-queries the library when changed; **Rescan library** with the last scan time and the song count. The privacy footer stays.
- **Deleted files, end to end**: the library is re-queried when the app returns to the foreground, so songs deleted while the app was away vanish from folders and grey out in playlists without a manual rescan; when playback has to skip a missing file the app says so ("Couldn't play <title> — skipped") instead of silently jumping.
- **Empty states** for the folder detail and the playlist detail use the same illustration-plus-action layout as the Folders and Playlists tabs (the queue sheet needs none: Now Playing closes itself when the queue is gone).
- **Motion**: Now Playing slides up over the current screen and slides back down on collapse or predictive back; other screens cross-fade with a slight slide; the selection bar animates in and out.
- **Drag-select** (spec F2 stretch): long-press a song and drag up or down to select a contiguous range; dragging back shrinks it.
- Not in this change: embedded album art (D-02 stays), equaliser, widgets, tag editing (spec F9).

## Capabilities

### New Capabilities
- `settings`: the Settings screen's controls — theme override, short-audio threshold, rescan with last-scan info — and their persistence.

### Modified Capabilities
- `theme`: "Dark mode follows the system" becomes override-aware (System / Light / Dark; system bars follow the applied palette).
- `app-shell`: "Settings entry point" — the stub-content scenario now describes the real controls plus the footer.
- `folder-browser`: "Short audio is hidden" (threshold comes from Settings, not a constant), "Empty library state" (hint reflects the threshold), "Folder detail" (real empty state; songs deleted while the app was away disappear on return), "Folder list" (footer reflects the threshold).
- `multi-select`: "Entering and leaving selection" gains drag-select and the animated bar.
- `playlists`: "Playlist detail" — the empty playlist gets a real empty state with an action.
- `playback`: "Missing files are skipped" — the skip is reported to the UI.
- `now-playing`: "Opens from the mini player and closes back to where the user was" — the open/close is animated (slide up / slide down, predictive back).

## Impact

- New dependency `androidx.datastore:datastore-preferences` (runtime only, no code generation; no new permissions — still no `INTERNET`).
- `data/settings/` (repository + `ThemeMode`), `SettingsViewModel` + `SettingsHost`, `AppContainer` wiring; `MediaScanner.scan(minDurationMs)` and `LibraryRepository` read the threshold at scan time; `LibrarySnapshot` records it.
- `MainActivity` resolves the theme and restyles the system bars; `AppNavigation` gains transition specs, a snackbar host for skip notices and the `SettingsHost`; `AndroidManifest` opts into predictive back.
- `PlaybackService` broadcasts a skip event; `PlayerConnection` maps it into `PlayerState.skipped`.
- `SettingsScreen` rewritten; `FolderDetailScreen` (drag-select, animated bar, empty state), `PlaylistDetailScreen` (empty state), `EmptyState` (optional action).
- Tests: JVM (settings repository over a temp DataStore file, theme resolution, scanner threshold, range selection, ViewModels) and instrumented (settings screen, drag-select, transitions, skip notice, empty states) on API 36 and API 26.
