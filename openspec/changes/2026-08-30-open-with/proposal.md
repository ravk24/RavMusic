## Why

Android's "Open with" sheet never lists RavMusic for an audio file: the manifest declares only the launcher filter, so a track tapped in Files, a download, or a voice note shared from a messenger always goes to another player. A music player that cannot be chosen for a music file feels broken, and the user asked for exactly this.

## What Changes

- **The app registers for audio files**: `VIEW` intents with a `content` or `file` URI and an audio MIME type (including the `application/*` aliases some file managers use for .ogg/.m4a/.flac), and `SEND` intents carrying one audio file. No web links, no multiple files.
- **Opening a file plays just that file**: a single-song queue with the origin "Opened file", Now Playing opens on top of the Playlists tab, and back returns there with the mini player showing. A second file opened while the app runs replaces the queue in place.
- **Titles come from the best source available**: the media index row (with artist and duration) when the file has one, otherwise the provider's display name or the file name without its extension.
- **Works without the audio permission**: the system's per-file read grant is enough to play the file; the library screens behind Now Playing keep gating as before.
- **Unplayable files** use the existing skip notice and never open Now Playing.

Explicitly not in this change: playing the file's whole folder, adding opened files to playlists, reading embedded tags with `MediaMetadataRetriever`, persisting URI grants, `SEND_MULTIPLE`.

## Capabilities

### New Capabilities
- `open-with`: which intents the app handles, what is played, how the song is described, navigation and permission behaviour, and failure handling.

### Modified Capabilities
- none — `queue`, `now-playing` and `mini-player` behave as specified for a one-song queue.

## Impact

- `AndroidManifest.xml`: two new intent filters on `MainActivity` (still `singleTask`).
- `MainActivity`: reads the launch intent (fresh launches only) and `onNewIntent`, hands a pure `OpenRequest` to `AppViewModel`; `AppRoot` resolves it and passes a one-shot `OpenedFile` to `AppNavigation`.
- New `data/model/OpenedFile.kt` (pure: request mapping, title from file name, synthetic ids) and `data/mediastore/UriSongResolver.kt` (library / MediaStore / provider lookup); `AppContainer` gains the resolver.
- `AppNavigation`: plays the song and pushes `NowPlaying` once the session reports it current.
- Tests: JVM tests for the pure parts and `AppViewModel`; instrumented resolver test against MediaStore and a shell navigation test. No new dependencies or permissions; still no network.
