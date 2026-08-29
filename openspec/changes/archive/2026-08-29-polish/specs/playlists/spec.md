## MODIFIED Requirements

### Requirement: Playlist detail
The playlist detail SHALL be pushed above the tabs and show the art tile, the name, "N songs · <total duration>", a primary **Shuffle play** action and a **Play** action, and the tracks in order. Each row SHALL show a drag handle, the title, the artist or "Unknown artist", and the duration, and the row of the song currently playing SHALL be highlighted. Back SHALL return to the Playlists tab. A playlist with no tracks SHALL show an empty state with an "Open Folders" action that leaves the detail and shows the Folders tab.

#### Scenario: Open a playlist
- **WHEN** the user taps the "Late night" card
- **THEN** the detail shows "Late night", "42 songs · 2h 58m", Shuffle play, Play, and the 42 rows in playlist order

#### Scenario: Empty playlist
- **WHEN** the user opens a playlist with no tracks
- **THEN** the detail shows "0 songs" and an empty state titled "No songs yet" with a hint to add songs from Folders and an "Open Folders" action; Play and Shuffle play do nothing; tapping "Open Folders" shows the Folders tab
