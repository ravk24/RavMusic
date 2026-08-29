## MODIFIED Requirements

### Requirement: Content and controls
The mini player SHALL show the song title, the artist or "Unknown artist", and a play/pause button that toggles playback; it SHALL NOT show album art or a placeholder tile. A thin progress line along its top edge SHALL show the position within the current song. Its content SHALL follow the current song when the queue advances.

#### Scenario: Toggle from the mini player
- **WHEN** a song is playing and the user taps the mini player's pause button
- **THEN** playback pauses and the button changes to play; tapping it again resumes

#### Scenario: Queue advances
- **WHEN** the current song ends and the next one starts
- **THEN** the mini player shows the next song's title and artist, and the progress line restarts
