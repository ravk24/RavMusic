# Phase 4 — Playlists

**OpenSpec change:** `playlists` · **Status:** ✅ Done (2026-08-29)

**Result:** 58 JVM unit tests green; 65 Compose/instrumented tests green on the API 36 and API 26 emulators,
including `PlaylistDaoTest` (in-memory Room) and `AddToPlaylistFlowTest` (the real ViewModel over an in-memory
database driven through the shell: select → add to a new playlist → card → detail in order → duplicate prompt).
adb walkthrough on both: selection mode, add-to-playlist sheet, Shuffle play, drag reorder persisted, swipe remove,
rename, missing-file banner + Clean up, delete with confirmation; playlists survive `am force-stop` and, on API 26,
`adb reboot`. Room 2.8.4 + KSP 2.3.11 build with AGP's built-in Kotlin (no fallback pin needed).

## Goal
The main play unit, saved locally, plus the multi-select flow that is the whole reason this app exists
(spec **F2**, **F3**).

## In scope
- Room in `data/db/`: `Playlist(id, name, createdAt, sortOrder)`,
  `PlaylistTrack(id, playlistId, mediaStoreUri, title, artist, duration, position)` with snapshotted
  metadata so lists render without re-querying MediaStore
- `PlaylistRepository`
- Multi-select in folder detail (artboard 1d): long-press enters selection, tap toggles, contextual
  top bar (count, "Select all N", close), primary action **Add to playlist** → bottom sheet listing
  playlists + "New playlist"; selection survives scrolling, not navigation
- Playlist CRUD: create (dialog), rename, delete (confirm); duplicate warning ("3 already in this
  playlist — add anyway / skip")
- Playlists home grid (artboard 1a) with gradient art per playlist; FAB "+"
- Playlist detail (artboard 1e): header (name, count, total duration), **Shuffle play** + Play,
  drag-handle reorder, swipe-to-remove, currently-playing highlight, greyed missing-file rows + "clean up"

## Out of scope
Drag-select range (phase 7 stretch), Now Playing.

## Capabilities
`playlists` (new), `multi-select` (new)

## Key risks
- Room schema is the first persisted data → export schema JSON, plan for migrations from v1
- Reorder + swipe gestures in a `LazyColumn` → keep gesture code in one composable

## Verification
- Room DAO unit tests (in-memory DB)
- UI test: long-press → select 3 → add to new playlist → playlist shows 3 with correct order
- Reboot the emulator: playlists persist

## Task progress
See `openspec/changes/playlists/tasks.md` (19 tasks, seven groups) — moves to
`openspec/changes/archive/` when the change is archived.
