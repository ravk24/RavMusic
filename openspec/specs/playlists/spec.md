# playlists Specification

## Purpose
Playlists are the app's main play unit: named, ordered lists of songs the user assembles from folders, stored on the device, played (usually shuffled) from a detail screen, and kept tidy when files disappear.

## Requirements

### Requirement: Playlists persist on the device
A playlist SHALL have a name, a creation time and an ordered list of tracks. Each track SHALL store the song's MediaStore URI together with a snapshot of its title, artist and duration, so the playlist renders without querying the media index. Playlists SHALL survive process death and device reboot, and SHALL never leave the device.

#### Scenario: Survives relaunch
- **WHEN** the user creates a playlist with three songs, force-stops the app and opens it again
- **THEN** the playlist is listed with the same name and the same three songs in the same order

#### Scenario: Survives reboot
- **WHEN** the device is rebooted and the app is opened
- **THEN** every playlist and its tracks are still present

### Requirement: Create, rename and delete
The user SHALL be able to create a playlist by entering a name, rename an existing playlist, and delete a playlist after confirming. Names SHALL be trimmed and SHALL NOT be blank. Deleting a playlist SHALL remove its tracks but SHALL NOT affect the audio files.

#### Scenario: Create from the home screen
- **WHEN** the user taps "+" on the Playlists tab, enters "Late night" and confirms
- **THEN** a playlist named "Late night" with 0 songs appears in the grid

#### Scenario: Blank name rejected
- **WHEN** the user tries to confirm a playlist name that is empty or only whitespace
- **THEN** the confirm action is unavailable and no playlist is created

#### Scenario: Rename
- **WHEN** the user chooses Rename on a playlist detail, enters "Late nights" and confirms
- **THEN** the detail header and the home card show "Late nights"

#### Scenario: Delete with confirmation
- **WHEN** the user chooses Delete on a playlist detail and confirms
- **THEN** the app returns to the Playlists tab and the playlist is gone; cancelling keeps it

### Requirement: Playlists home grid
The Playlists tab SHALL show every playlist as a card in a two-column grid, each with the name and "N songs · <total duration>" (total formatted as "2h 58m" or "51m") and no art tile. The header SHALL show the title "Playlists" and the playlist count. A "+" action SHALL create a new playlist. When there are no playlists an empty state SHALL explain how to make one and offer the create action. Tapping a card SHALL open the playlist detail.

#### Scenario: Grid contents
- **WHEN** two playlists exist: "Late night" (42 songs, 2 h 58 min) and "Focus" (1 song, 4 min)
- **THEN** the grid shows two cards "Late night — 42 songs · 2h 58m" and "Focus — 1 song · 4m" and the header reads "2 playlists"

#### Scenario: Empty home
- **WHEN** no playlists exist
- **THEN** the empty state with a "New playlist" action is shown instead of a grid, and the "+" action still works

### Requirement: Playlist detail
The playlist detail SHALL be pushed above the tabs and show the name, "N songs · <total duration>", a primary **Shuffle play** action and a **Play** action, and the tracks in order; it SHALL NOT show an art tile. Each row SHALL show a drag handle, the title, the artist or "Unknown artist", and the duration, and the row of the song currently playing SHALL be highlighted. Back SHALL return to the Playlists tab. A playlist with no tracks SHALL show an empty state with an "Open Folders" action that leaves the detail and shows the Folders tab.

#### Scenario: Open a playlist
- **WHEN** the user taps the "Late night" card
- **THEN** the detail shows "Late night", "42 songs · 2h 58m", Shuffle play, Play, and the 42 rows in playlist order

#### Scenario: Empty playlist
- **WHEN** the user opens a playlist with no tracks
- **THEN** the detail shows "0 songs" and an empty state titled "No songs yet" with a hint to add songs from Folders and an "Open Folders" action; Play and Shuffle play do nothing; tapping "Open Folders" shows the Folders tab

### Requirement: Playing a playlist
**Play** SHALL start the playlist from its first track with shuffle off. **Shuffle play** SHALL enable shuffle and start from a random track. Tapping a row SHALL start the playlist from that row with shuffle off. In every case the queue SHALL be the playlist's tracks in playlist order, excluding missing tracks, and the queue origin SHALL be the playlist name.

#### Scenario: Shuffle play
- **WHEN** the user taps Shuffle play on "Late night"
- **THEN** a track of "Late night" starts playing, the mini player appears, shuffle is enabled on the session, and the origin is "Late night"

#### Scenario: Row tap
- **WHEN** the user taps the third row
- **THEN** the third track starts playing with the following tracks queued in playlist order and shuffle off

### Requirement: Reorder and remove
Dragging a row by its handle SHALL move it to the drop position and the new order SHALL be persisted. Swiping a row away SHALL remove that track from the playlist (not from the device).

#### Scenario: Drag to reorder
- **WHEN** the user drags the second row above the first and releases, then leaves and reopens the playlist
- **THEN** the former second track is listed first

#### Scenario: Swipe to remove
- **WHEN** the user swipes a row off the screen
- **THEN** the track disappears from the playlist and the count decreases by one

### Requirement: Missing files
A track whose URI is no longer present in the loaded library SHALL be shown greyed out, SHALL be skipped when the playlist is played, and the detail SHALL offer a "Clean up" action that removes all such tracks. While the library has not been loaded, no track SHALL be flagged as missing.

#### Scenario: Deleted file
- **WHEN** a file referenced by a playlist track has been deleted and the library re-queried
- **THEN** its row is greyed, "1 song can't be found · Clean up" is shown, and Play skips it

#### Scenario: Clean up
- **WHEN** the user taps "Clean up"
- **THEN** every missing track is removed from the playlist and the banner disappears
