# Phase 2 — Library

**OpenSpec change:** `library-browser` · **Status:** ✅ Done (2026-08-29)

**Result:** see the Verification section below for the numbers recorded at the end of the change. The
path-hash folder id used below API 29 was verified to reproduce MediaStore's real `bucket_id` values
exactly (decision D-16).

## Goal
Show the user's real songs, grouped by the folders they live in (spec **F1**).

## In scope
- `MediaStoreScanner`: query `MediaStore.Audio.Media` (`IS_MUSIC != 0`), grouped by `BUCKET_ID` /
  `BUCKET_DISPLAY_NAME`; live queries, no cache (decision D-05)
- Folder list (artboard 1c): name + song count, alphabetical; pull-to-refresh re-queries
- Folder detail (artboard 1d, browse mode): title, artist (if tagged), duration; pushed above the tabs (no
  bottom bar); tap a song *does nothing yet* until phase 3
- Short-audio filter: hide clips under 30 s (constant in code; Settings row comes with the Settings change)
- Empty state when no music is found (shared `EmptyState` component with the phase 1 gate screen, different
  copy, "Rescan" action)
- `MediaStoreScanner` in `data/mediastore/`, `LibraryRepository` in `data/repo/`, wired through
  `AppContainer`; Activity-scoped `LibraryViewModel`

## Out of scope
Playback, multi-select, playlists.

## Capabilities
`folder-browser` (new)

## Key risks
- MediaStore bucket semantics differ across OEMs → tested with files in `/Music`, `/Download`, and a
  nested folder on API 26 and API 36
- Audio rows have no `BUCKET_*` columns before API 29 → folder derived from `DATA` with MediaStore's own
  formula (D-16)
- Large libraries (1000+ songs) → query on `Dispatchers.IO`, one grouping pass per scan, `LazyColumn`
  with stable keys

## Verification
- Folder list matches `adb shell content query --uri content://media/external/audio/media` counts
- Pull-to-refresh picks up a file pushed with `adb push` + media scan broadcast
- Clip under 30 s does not appear

**Results (2026-08-29):** 35 JVM unit tests green; 30 Compose UI tests green on the API 36 and API 26
emulators. Walkthrough with generated WAVs (35 s `/Music`, 41 s `/Music/Rock`, 65 s + 50 s `/Download`,
5 s `/Music` clip) on both: folder names and counts equal `content query` minus the hidden clip; pull-to-refresh
picked up a newly pushed file (3 → 4 songs); the detail shows "Unknown artist" and durations with no bottom bar;
revoking the permission while on a detail and returning shows "No music found", re-granting restores it. No
scanner exceptions in logcat on either API level.

## Task progress
See `openspec/changes/library-browser/tasks.md` (19 tasks, five groups) — moves to
`openspec/changes/archive/` when the change is archived.
