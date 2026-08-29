# mini-player Specification

## Purpose
The persistent mini player: a compact bar docked at the bottom of every screen while a queue is loaded, showing the current song with play/pause and progress, and offering a swipe to stop.

## Requirements

### Requirement: Docked above the bottom navigation
While a queue is loaded the mini player SHALL be visible at the bottom of every screen — above the bottom navigation bar on the tab screens, and at the very bottom on screens that hide the bar (folder detail, Settings). While no queue is loaded it SHALL NOT be shown and SHALL take no space.

#### Scenario: Visible on tabs
- **WHEN** a song is playing and the user is on the Playlists or Folders tab
- **THEN** the mini player is shown directly above the bottom navigation bar

#### Scenario: Visible on a detail screen
- **WHEN** a song is playing and the user opens a folder detail
- **THEN** the mini player is shown at the bottom of the screen with no navigation bar below it

#### Scenario: Hidden when idle
- **WHEN** nothing has been played since the app started, or the queue was cleared
- **THEN** no mini player is shown and the content extends to the bottom navigation bar

### Requirement: Content and controls
The mini player SHALL show the song title, the artist or "Unknown artist", and a play/pause button that toggles playback; it SHALL NOT show album art or a placeholder tile. A thin progress line along its top edge SHALL show the position within the current song. Its content SHALL follow the current song when the queue advances.

#### Scenario: Toggle from the mini player
- **WHEN** a song is playing and the user taps the mini player's pause button
- **THEN** playback pauses and the button changes to play; tapping it again resumes

#### Scenario: Queue advances
- **WHEN** the current song ends and the next one starts
- **THEN** the mini player shows the next song's title and artist, and the progress line restarts

### Requirement: Swipe to stop
Swiping the mini player horizontally away SHALL stop playback, clear the queue and hide the mini player.

#### Scenario: Swipe away
- **WHEN** a song is playing and the user swipes the mini player off the screen
- **THEN** audio stops, the media notification is removed, and the mini player disappears

### Requirement: Tap reserved for Now Playing
Tapping the mini player body SHALL open the Now Playing screen as specified by the `now-playing` capability. The play/pause button and the swipe gesture keep their own behaviour.

#### Scenario: Tap is inert
- **WHEN** the user taps the mini player's title area
- **THEN** the tap is no longer inert: the Now Playing screen opens

#### Scenario: Controls unaffected
- **WHEN** the user taps the mini player's play/pause button
- **THEN** playback toggles and the Now Playing screen does not open
