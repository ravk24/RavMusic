# Phase 5 — Now Playing

**OpenSpec change:** `now-playing` · **Status:** ⏳ Planned

## Goal
The full-screen player (artboard 1f / 1k) with seek, shuffle, repeat, and the queue sheet
(spec **F5**, rest of **F7**).

## In scope
- Expand from the mini player; large gradient art, title, artist, "Playing from: <source>"
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
- Seek-bar updates at 60 fps drain battery → poll position at ~250 ms only while visible
- Queue reorder must map between ExoPlayer's shuffled order and the displayed order

## Verification
- Toggle shuffle: queue sheet order changes; repeat-one loops the same track
- Drag a queued song above the current one: it plays next
