# Phase 7 — Polish

**OpenSpec change:** `polish` (may split into smaller changes) · **Status:** ⏳ Planned

## Goal
Make it feel finished: the edges, the empty states, the settings, the icon.

## In scope
- Real **Settings** screen (artboard 1g): theme override System / Light / Dark persisted with
  DataStore (`RavMusicTheme(darkTheme)` already takes the override), "Skip short audio" threshold,
  "Rescan library" with last-query time and song count
- Deleted-file handling end to end: greyed rows, skip on playback, "Clean up" on a playlist
- Empty states for every list (no playlists yet, empty playlist, empty folder)
- Motion: mini player ↔ Now Playing expand/collapse, selection-mode top-bar transition
- Drag-select range in multi-select (spec F2 stretch goal)
- ~~App icon~~ — **done early** during Phase 1 from the supplied `RavMusic-icon.png` (decision D-14)
- Optional: embedded album art extraction with a small in-memory cache (decision D-02 revisit)

## Out of scope
Equaliser, widgets, Android Auto, Wear OS, tag editing (spec **F9**).

## Capabilities
`settings` (new); modifies `theme`, `playlists`, `folder-browser`, `multi-select`

## Verification
- Theme override persists across process death
- Delete a file from storage, reopen the playlist: row greyed, playback skips it, "Clean up" removes it
- Every screen has a non-blank empty state
