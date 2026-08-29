# Phase 2 — Library

**OpenSpec change:** `library-browser` · **Status:** ⏳ Planned

## Goal
Show the user's real songs, grouped by the folders they live in (spec **F1**).

## In scope
- `MediaStoreScanner`: query `MediaStore.Audio.Media` (`IS_MUSIC != 0`), grouped by `BUCKET_ID` /
  `BUCKET_DISPLAY_NAME`; live queries, no cache (decision D-05)
- Folder list (artboard 1c): name + song count, alphabetical; pull-to-refresh re-queries
- Folder detail: title, artist (if tagged), duration; tap a song *does nothing yet* until phase 3
- Short-audio filter: hide clips under 30 s (constant in code; Settings row comes with the Settings change)
- Empty state when no music is found (reuses the phase 1 gate screen with different copy)
- `LibraryRepository` in `data/mediastore/` and `data/repo/`, wired through `AppContainer`

## Out of scope
Playback, multi-select, playlists, album art.

## Capabilities
`folder-browser` (new)

## Key risks
- MediaStore bucket semantics differ across OEMs → test with files in `/Music`, `/Download`, and a
  nested folder
- Large libraries (1000+ songs) → query on `Dispatchers.IO`, paged `LazyColumn`

## Verification
- Folder list matches `adb shell content query --uri content://media/external/audio/media` counts
- Pull-to-refresh picks up a file pushed with `adb push` + media scan broadcast
- Clip under 30 s does not appear
