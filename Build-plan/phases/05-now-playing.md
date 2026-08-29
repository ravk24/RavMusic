# Phase 5 — Now Playing

**OpenSpec change:** `now-playing` · **Status:** ✅ Done (2026-08-29)

**Result:** 66 JVM unit tests green; 81 Compose/instrumented tests green on the API 36 and API 26 emulators,
including `PlaybackServiceTest` cases for seek, next/previous, repeat-one looping, repeat-all wrapping, shuffle
toggling, queue jump, and both reorder policies. adb walkthrough on both: open from the mini player, seek, shuffle /
repeat cycle persisted across reopen, repeat-one loop, queue sheet with jump, collapse and back. Found and fixed one
real defect on the way: commands sent before the `MediaController` connected were last-one-wins (D-42).

## Goal
The full-screen player (artboard 1f / 1k) with seek, shuffle, repeat, and the queue sheet
(spec **F5**, rest of **F7**).

## In scope
- Expand from the mini player; title and artist (the gradient art was removed later, D-56), title, artist, "Playing from: <source>"
- Seek bar with elapsed / total
- Controls: shuffle toggle — prev — play/pause (large) — next — repeat cycle (Off → All → One)
- `player.shuffleModeEnabled` / `player.repeatMode` wired through the `MediaController`; state shown in
  Now Playing and persisted for the session
- Shuffle Play from a playlist enables shuffle and starts on a random track
- Queue bottom sheet: upcoming songs, tap to jump, drag to reorder
- Secondary row: sleep-timer chip (stub until phase 6), queue chip ("N left")

## Out of scope
Sleep timer behaviour, crossfade, lyrics, equaliser.

## Capabilities
`now-playing` (new), `shuffle-repeat` (new), `queue` (new)

## Key risks
- Seek-bar updates at 60 fps drain battery → the screen polls at 250 ms only while visible (D-40)
- Queue reorder must map between ExoPlayer's shuffled order and the displayed order → the shown order is
  frozen as the queue and shuffle turns off (D-39)

## Verification
- Toggle shuffle: queue sheet order changes; repeat-one loops the same track
- Drag a queued song above the current one: it plays next

## Task progress
See `openspec/changes/now-playing/tasks.md` (10 tasks, six groups) — moves to
`openspec/changes/archive/` when the change is archived.
