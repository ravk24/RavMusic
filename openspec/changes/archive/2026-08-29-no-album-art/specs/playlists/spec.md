## MODIFIED Requirements

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
