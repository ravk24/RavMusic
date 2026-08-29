# Personal Music Player — Project Spec

A simple, offline, ad-free music player for personal use on Android. Plays local files only. Built for one user (me), never going to the Play Store.

**Why this exists:** VLC's multi-select UI is painful, every other player has ads, accounts, subscriptions, or wants internet access. This app does none of that.

**Hard constraints (non-negotiable):**
- No ads, no accounts, no telemetry, no subscriptions, no API keys, no backend
- Fully offline — the manifest will not even declare the `INTERNET` permission, so network access is physically impossible
- Lightweight — no analytics SDKs, no crash reporters, no bloat dependencies

---

## Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard for modern Android |
| UI | Jetpack Compose + Material 3 | Declarative UI, easy to iterate on design |
| Playback | Media3 (ExoPlayer + MediaSessionService) | Background play, lockscreen/notification controls, headset buttons, audio focus — all handled by the library |
| Music discovery | MediaStore API | System-indexed audio, instant queries, no file crawling, no SAF pickers |
| Local storage | Room (SQLite) | Playlists persist across reboots, purely on-device |
| Architecture | Single activity, MVVM-ish (ViewModel + StateFlow) | Simple, testable enough for a personal app |
| Distribution | Locally signed release APK, sideloaded | No Play Store |

**Min SDK:** 26 (Android 8.0). **Target SDK:** latest stable.

---

## Core Concept

Folders are where songs **live**. Playlists are what you **play**.

- Browse phone storage by folder (via MediaStore bucket grouping, not raw filesystem)
- Multi-select songs from folders → add to a playlist
- Play playlists, usually shuffled
- The app never "plays a folder" directly — playback is always playlist- or selection-based

No artist/album/genre views. Not needed.

---

## Permissions

Exactly one runtime permission:

- `READ_MEDIA_AUDIO` — Android 13+ (API 33+)
- `READ_EXTERNAL_STORAGE` — fallback for Android 12 and below (maxSdkVersion 32)

Plus `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (declared in manifest, not user-facing) so playback survives when the app is backgrounded.

**Not declared:** `INTERNET`. This is the privacy guarantee.

---

## Features — Detailed Specs

### F1. Folder Browser

**Purpose:** find songs on the device, grouped the way they're organized in storage.

- Query MediaStore for all audio (`IS_MUSIC != 0`), grouped by `BUCKET_DISPLAY_NAME` / `BUCKET_ID`
- Folder list screen: folder name + song count per folder, sorted alphabetically
- Tap a folder → song list: title, artist (if tagged), duration
- Tapping a song plays it immediately (ad-hoc queue = that folder's songs, starting from the tapped one)
- Pull-to-refresh re-queries MediaStore (for newly copied files)
- Exclude junk: filter out very short audio (< 30s default) to skip notification sounds / WhatsApp audio; keep this a constant in code, easy to tweak

**Edge cases:**
- Song deleted from storage but still referenced → show greyed-out, skip on playback, offer "clean up" on the playlist
- Empty state when no music found → friendly message + hint to check permission

### F2. Multi-Select (the VLC fix)

**Purpose:** select many songs fast, without fighting the UI.

- Long-press any song → enters selection mode
- In selection mode: tap toggles selection, checkboxes visible on every row
- Top bar switches to contextual mode: selected count, "Select all in folder", close (X)
- Drag-select: long-press then drag over rows to select a range (stretch goal, phase 2 of polish)
- Primary action: **Add to playlist** → bottom sheet listing existing playlists + "New playlist" option
- Selection survives scrolling but not navigation away

### F3. Playlists

**Purpose:** the main play unit. Saved locally, survive reboots.

**Data model (Room):**
- `Playlist(id, name, createdAt, sortOrder)`
- `PlaylistTrack(id, playlistId, mediaStoreUri, title, artist, duration, position)`
  - Snapshot title/artist/duration so the list renders instantly without re-querying MediaStore
  - `mediaStoreUri` is the content URI — stable across reboots

**Operations:**
- Create (name it via dialog), rename, delete (with confirm)
- Add songs (from multi-select flow, or from a song's overflow menu)
- Remove songs (swipe-to-remove or multi-select within playlist)
- Reorder: drag handle on each row
- Duplicates: allowed but warn ("3 songs already in this playlist — add anyway / skip duplicates")

**Playlist detail screen:**
- Header: playlist name, song count, total duration
- Big **Shuffle Play** button (primary action) + regular Play button
- Song rows with drag handles, currently-playing song highlighted

### F4. Playback

**Purpose:** rock-solid background playback.

- `MediaSessionService` + ExoPlayer — playback lives in a foreground service, not the activity
- Media notification with play/pause/next/prev + seek (Media3 default notification)
- Lockscreen controls, Bluetooth/headset button support — free via MediaSession
- Audio focus handled: pause on phone call, duck or pause on other audio (Media3 default behavior)
- Pause (don't stop) when headphones unplugged (`AUDIO_BECOMING_NOISY`)
- Queue = the playlist (or folder selection) that started playback
- Gapless-ish: ExoPlayer default transition is fine, no crossfade needed (maybe later)

**Supported formats:** whatever ExoPlayer handles — mp3, aac/m4a, flac, ogg, wav covers everything realistically on the phone.

### F5. Shuffle & Repeat

- Shuffle toggle → `player.shuffleModeEnabled` (ExoPlayer built-in, handles queue order internally)
- Repeat cycle button: Off → Repeat All → Repeat One → Off (`player.repeatMode`)
- Both states shown in Now Playing and persist for the session
- Starting a playlist via **Shuffle Play** enables shuffle and starts from a random track

### F6. Sleep Timer

- Accessible from Now Playing screen (moon icon)
- Presets: 15 / 30 / 45 / 60 min, custom minutes input, and **"End of current track"**
- Runs as a coroutine in the playback service (not the UI — must survive app being backgrounded)
- Last 10 seconds: fade volume to zero, then pause (not stop — resume works normally after)
- Active timer shown in Now Playing (remaining time), tap to cancel or extend
- "End of current track" = pause when current media item completes

### F7. Now Playing + Mini Player

**Mini player** (persistent, docked above bottom of every screen while something is loaded):
- Album art thumbnail (or placeholder), title, artist, play/pause button
- Progress shown as a thin line at the top of the bar
- Tap → expands to Now Playing; swipe away → stops playback and clears queue

**Now Playing (full screen):**
- Large album art (embedded art via MediaStore/metadata retriever, fallback placeholder)
- Title, artist
- Seek bar with elapsed / total time
- Controls row: shuffle toggle — prev — play/pause (big) — next — repeat cycle
- Secondary row: sleep timer, current queue view
- Queue view: bottom sheet showing upcoming songs, tap to jump, drag to reorder

### F8. UI / Design

- Material 3, dynamic color (Material You) — picks up the phone's wallpaper palette automatically
- Dark theme follows system, with manual override in a minimal settings screen
- One accent, generous spacing, big touch targets — the whole point is a UI that doesn't fight back
- Design will be mocked separately before development; this spec defines structure, not final visuals

**Screens summary:**
1. Playlists (home) — grid or list of playlists, FAB "+"
2. Folders — second tab or nav destination
3. Folder detail (song list, multi-select)
4. Playlist detail
5. Now Playing (expandable from mini player)
6. Settings (tiny: theme override, min-duration filter, maybe rescan button)

Navigation: bottom nav with 2 tabs (Playlists / Folders) + mini player docked above it.

### F9. Explicitly Out of Scope

- Streaming, downloads, lyrics fetching, scrobbling, album art fetching from internet (no internet, period)
- Equalizer (maybe a later toy, system EQ intent is a cheap option)
- Widgets, Android Auto, Wear OS
- Tag editing
- Play Store release, crash reporting, analytics

---

## Project Structure (planned)

```
app/src/main/java/.../
├── MainActivity.kt
├── di/                  # manual DI or Hilt (decide at build time; manual is fine for this size)
├── data/
│   ├── mediastore/      # MediaStoreScanner — folder + song queries
│   ├── db/              # Room: entities, DAOs, database
│   └── repo/            # PlaylistRepository, LibraryRepository
├── playback/
│   ├── PlaybackService.kt   # MediaSessionService + ExoPlayer
│   └── SleepTimer.kt
└── ui/
    ├── theme/
    ├── playlists/
    ├── folders/
    ├── nowplaying/      # full screen + mini player
    └── components/      # shared: song row, selection top bar, etc.
```

---

## Build Order (development phases)

1. **Skeleton** — project setup, theme, bottom nav, permission request flow
2. **Library** — MediaStore scanner + folder browser showing real device songs
3. **Playback core** — PlaybackService, tap-to-play from folder, mini player *(first "it's a real app" moment)*
4. **Playlists** — Room setup, CRUD, multi-select flow, playlist detail
5. **Now Playing** — full screen, seek, shuffle/repeat wiring, queue sheet
6. **Sleep timer**
7. **Polish** — empty states, deleted-file handling, animations, app icon
8. **Ship** — release build, sign locally, sideload APK

Each phase ends with something installable and testable on the actual phone.

---

## Build & Install Notes

- Build via Android Studio: `Build > Generate Signed Bundle / APK > APK`, create a local keystore once, reuse it for updates (same signature = updates install over the old version without uninstalling)
- Debug APK also works fine for personal use if signing feels like ceremony
- Enable "Install unknown apps" for the file manager / method used to sideload
- APK size expectation: well under 10 MB with Compose + Media3, R8 shrinking on for release
