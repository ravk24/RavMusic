## Context

See proposal.md — Why. After 1.0.2 the manifest has a launcher filter only; `MainActivity` never inspects its intent; every path into playback is `PlayerBridge.play(QueuePlan)` over fully-formed `Song`s (`PlayerViewModel.playSongs` → `planQueue` → `MediaItems`); `NowPlaying` is pushed only by the mini player and pops itself when `PlayerState.hasQueue` turns false. Requirements are in `specs/open-with/spec.md`.

Constraints: no network (local schemes only), no new permissions, no album art, `Song` stays pure Kotlin, the existing skip notice handles unreadable files.

## Goals / Non-Goals

**Goals:**
- Be listed in "Open with" / share for audio files and play the chosen file immediately.
- Keep the intent → song → queue path pure enough to unit-test everything but the `ContentResolver` calls.
- Never confuse an opened file with a library or playlist song unless it *is* that song.

**Non-Goals:**
- Folder queues from an opened file, persisting grants, tag extraction, multi-file share.

## Decisions

### D1. Two intent filters, local schemes only
`VIEW` + `DEFAULT` with `content` and `file` schemes and `audio/*`, `application/ogg`, `application/x-ogg`, `application/itunes`, `application/x-flac`; `SEND` + `DEFAULT` with `audio/*`. No `BROWSABLE` (web links) and no `http(s)` — the app cannot go online. `launchMode="singleTask"` stays so a second open arrives in `onNewIntent`.

### D2. The request is pure data in `AppViewModel`
`MainActivity` reduces the intent to strings (`dataString`, `EXTRA_STREAM`, first `ClipData` item) and `openRequestUri(action, data, stream, clip)` picks the URI (`VIEW` → data, `SEND` → stream, both falling back to the clip). The result is an `OpenRequest(uri, mimeType, seq)` in the activity-scoped `AppViewModel` (`pendingOpen`), in memory only: a configuration change re-runs the resolution, a relaunch from recents (`savedInstanceState != null`) does not replay the file. `consumeOpen(seq)` is sequence-guarded so a newer request is never lost.
*Alternatives:* saved state — would replay after process death; a broadcast into the service — bypasses the shell that owns navigation.

### D3. `UriSongResolver` reuses what it can, fabricates the rest
Order: exact URI in the loaded library snapshot → MediaStore query of a `content://media/...` URI (real id; the canonical `audio/media/<id>` URI so it equals what playlists store) → `file://` looked up by `DATA` → the provider's `DISPLAY_NAME` → the last path segment. Steps after the first two use `openedSong`, whose id is `-(hash + 2) ≤ -2`: MediaStore ids are positive and `-1` is `PlaylistTrack.mediaStoreId`'s sentinel for non-numeric URIs, so a fabricated id can never light up a playlist row. Every provider call is wrapped: refusal, rejection or "unsupported" simply moves to the next step.

### D4. No persistable grant, no metadata retriever
`VIEW`/`SEND` grants are almost never persistable and `takePersistableUriPermission` throws when they are not; the per-UID read grant already covers the same-process `PlaybackService`, and an open file descriptor survives the activity finishing. `durationMs = 0` on a fabricated song is invisible — Now Playing reads the duration from the player.

### D5. Playback and Now Playing from the shell
`AppRoot` hands a one-shot `OpenedFile(song, seq)` to `AppNavigation`, which plays `listOf(song)` with origin "Opened file" and reports it handled. It then waits for `playerState.nowPlaying?.songId == song.id` before pushing `NowPlaying` (pushing earlier would race the screen's self-pop); an already-open Now Playing is not duplicated. Not gated by the audio permission: a granted URI plays without `READ_MEDIA_AUDIO`, and back lands on the gate, which is right.

### D6. File layout
```
app/src/main/AndroidManifest.xml                       intent filters
app/src/main/java/com/ravk24/ravmusic/
  data/model/OpenedFile.kt                             OpenRequest, openRequestUri, titleFromFileName, syntheticSongId, openedSong, OpenedFile
  data/mediastore/UriSongResolver.kt                   ContentResolver lookups
  AppViewModel.kt                                      pendingOpen / submitOpen / consumeOpen
  MainActivity.kt                                      intent reading, AppRoot resolution
  RavMusicApp.kt                                       AppContainer.uriSongResolver
  ui/navigation/AppNavigation.kt                       play + push NowPlaying
```

## Risks / Trade-offs

- [`file://` paths outside MediaStore on API 29+] → no file-path access under scoped storage; the player reports an error and the skip notice shows. Documented; every mainstream file manager sends `content://` today.
- [MediaStore query refused without the audio permission] → falls through to the display name; playback still works through the grant.
- [Provider revokes the grant mid-playback] → the player's error path clears the queue as for a deleted file.

## Migration Plan

No persisted data. Rollback is `git revert`.
