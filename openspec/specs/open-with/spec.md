# open-with Specification

## Purpose
Letting the system hand the app an audio file — from a file manager, a download, or a share sheet — and playing it at once, so RavMusic can be the player the user picks for any music file on the phone.

## Requirements

### Requirement: Audio files can be opened with the app
The app SHALL register as a handler for local audio files: `VIEW` intents whose data is a `content` or `file` URI with an `audio/*` MIME type (or the `application/ogg`, `application/x-ogg`, `application/itunes` and `application/x-flac` types some file managers attach to audio), and `SEND` intents carrying a single `audio/*` file. It SHALL NOT handle web links or multiple files. Opening a file SHALL start playing it and show Now Playing, whether or not the audio permission has been granted. A second file opened while the app is running SHALL replace what is playing without stacking another Now Playing screen. A file the player cannot open SHALL be announced with the existing "Couldn't play … — skipped" notice and SHALL NOT open Now Playing.

#### Scenario: Appears in the Open with sheet
- **WHEN** the user picks an .mp3 in a file manager and chooses "Open with"
- **THEN** RavMusic is offered alongside the other music players

#### Scenario: Opens and plays the file
- **WHEN** the user chooses RavMusic for "Midnight Freeway.mp3"
- **THEN** the app comes to the front on Now Playing, "Midnight Freeway" is playing, and the mini player appears once Now Playing is closed

#### Scenario: Second file while running
- **WHEN** Now Playing is showing an opened file and the user opens another file from the file manager
- **THEN** the new file plays, Now Playing shows it, and one back press returns to the Playlists tab

#### Scenario: Launched before permission is granted
- **WHEN** the audio permission has never been granted and the user opens a file with RavMusic
- **THEN** the file plays and Now Playing is shown; going back shows the permission screen as usual

#### Scenario: File cannot be read
- **WHEN** the opened file cannot be decoded or its read grant is refused
- **THEN** "Couldn't play <title> — skipped" is shown, no queue is loaded and Now Playing does not open

### Requirement: Opened file queue
The queue for an opened file SHALL contain that one song and its origin SHALL be "Opened file". The song's title SHALL come from the media index row when the file has one (together with its artist and duration), otherwise from the provider's display name or the file name without its extension, and "Unknown title" when nothing usable exists. A file that is in the loaded library, or has a media index row, SHALL be treated as that song (same id and URI), so its folder and playlist rows highlight it as current; any other file SHALL get an id that can never match a library or playlist song. Back from Now Playing SHALL return to the Playlists tab with the mini player showing.

#### Scenario: Single-item queue labelled "Opened file"
- **WHEN** a voice note shared from a messenger is opened with RavMusic
- **THEN** Now Playing shows its file name without the extension as the title, origin "Opened file", and the queue sheet lists exactly that one song

#### Scenario: Library song opened from a file manager is highlighted
- **WHEN** a file that the library already lists as "Glass Rain" is opened with RavMusic
- **THEN** it plays with the title "Glass Rain" and its artist, and its row in the folder detail is highlighted as the current song

#### Scenario: Back from Now Playing
- **WHEN** the user presses back on Now Playing after opening a file
- **THEN** the Playlists tab is shown with the bottom bar and the mini player still playing the file
