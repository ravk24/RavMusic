# Phase 7 — Polish

**OpenSpec change:** `polish` · **Status:** ✅ Done (2026-08-29)

**Result:** Shipped as one `polish` change. `assembleDebug` + 88/88 JVM tests green (the real Preferences
DataStore runs on a temp file in JVM tests); connected suite 111/111 on API 26 and 111/111 on API 36
(the API 36 total was reached over two runs: four failures in the first — three checkbox assertions that
can never pass because `Checkbox(onCheckedChange = null)` exposes no toggle state, and one service test
whose playback was paused by a sleep timer left over from the previous test — were fixed in the tests and
the classes re-run green, 16/16); merged manifest 0 `INTERNET`, `enableOnBackInvokedCallback` on.
Manual walkthrough on both emulators: Dark override → `am force-stop` → relaunch on a light system comes
up dark with light status icons; threshold Off reveals the 5 s clip (4 → 5 songs, footer drops the note),
2 min hides everything (empty-library state, hint "Audio under 2 min hidden."); a file moved away while
the app was in the background disappears from its folder on resume without a manual rescan; an
unplayable file mid-queue is skipped with the "Couldn't play gamma — skipped" notice (seen via the
accessibility tree on both APIs; the 4 s snackbar outran the screenshot); empty playlist → "Open
Folders" lands on the Folders tab; long-press + drag on API 36 selects "2 selected" with the animated
navy bar; Now Playing slides up/down. Deviations: `adb shell input motionevent` does not exist on API 26,
so drag-select there is covered by `FolderDetailDragSelectTest` only; on API 29+ scoped storage keeps
MediaStore in sync with an `mv`, so "stale index" had to be simulated by truncating the file; the splash
screen keeps the system palette for its own frame (accepted, D-49); no edge auto-scroll while dragging
(D-52).

## Goal
Make it feel finished: the edges, the empty states, the settings, the icon.

## In scope
- Real **Settings** screen (artboard 1g): theme override System / Light / Dark persisted with
  DataStore (`RavMusicTheme(darkTheme)` already takes the override), "Skip short audio" threshold,
  "Rescan library" with last-query time and song count
- Deleted-file handling end to end: greyed rows, skip on playback, "Clean up" on a playlist
- Empty states for every list (no playlists yet, empty playlist, empty folder)
- Motion: mini player ↔ Now Playing expand/collapse, selection-mode top-bar transition
- Drag-select range in multi-select (spec F2 stretch goal) — **in scope**, done
- ~~App icon~~ — **done early** during Phase 1 from the supplied `RavMusic-icon.png` (decision D-14)
- ~~Optional: embedded album art extraction~~ — **deferred** by the user on 2026-08-29 (decision D-02 stands)

## Out of scope
Equaliser, widgets, Android Auto, Wear OS, tag editing (spec **F9**).

## Capabilities
`settings` (new); modifies `theme`, `app-shell`, `folder-browser`, `multi-select`, `playlists`, `playback`, `now-playing`

## Verification
- Theme override persists across process death
- Delete a file from storage, reopen the playlist: row greyed, playback skips it, "Clean up" removes it
- Every screen has a non-blank empty state

## Task progress
See `openspec/changes/polish/tasks.md` (13 tasks, six groups) — moves to
`openspec/changes/archive/` when the change is archived.
