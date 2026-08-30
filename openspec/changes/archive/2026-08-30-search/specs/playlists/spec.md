## MODIFIED Requirements

### Requirement: Playlists home grid
The Playlists tab SHALL show every playlist as a card in a two-column grid, each with the name and "N songs · <total duration>" (total formatted as "2h 58m" or "51m") and no art tile. The header SHALL show the title "Playlists", the playlist count, and a Search action that opens search across all playlists. A "+" action SHALL create a new playlist. When there are no playlists an empty state SHALL explain how to make one and offer the create action. Tapping a card SHALL open the playlist detail.

#### Scenario: Grid contents
- **WHEN** two playlists exist: "Late night" (42 songs, 2 h 58 min) and "Focus" (1 song, 4 min)
- **THEN** the grid shows two cards "Late night — 42 songs · 2h 58m" and "Focus — 1 song · 4m" and the header reads "2 playlists"

#### Scenario: Empty home
- **WHEN** no playlists exist
- **THEN** the empty state with a "New playlist" action is shown instead of a grid, and the "+" action still works

#### Scenario: Search action
- **WHEN** the user taps the Search action in the header
- **THEN** the search-across-playlists screen opens above the tab

### Requirement: Playlist detail
The playlist detail SHALL be pushed above the tabs and show the name, "N songs · <total duration>", a primary **Shuffle play** action and a **Play** action, and the tracks in order; it SHALL NOT show an art tile. Each row SHALL show a drag handle, the title, the artist or "Unknown artist", and the duration, and the row of the song currently playing SHALL be highlighted. Back SHALL return to the Playlists tab. A playlist with no tracks SHALL show an empty state with an "Open Folders" action that leaves the detail and shows the Folders tab. A Search action SHALL replace the title bar with a search field that takes focus; while its text is not blank the list SHALL show only the tracks whose title or artist contains the text (case-insensitive), a no-match state SHALL read "No songs match “<text>”", tapping a row SHALL still play the playlist from that track, and Play / Shuffle play SHALL still play the whole playlist. Clearing the text SHALL show every track again; closing the search (its back affordance or system back) SHALL restore the title bar and the full list.

#### Scenario: Open a playlist
- **WHEN** the user taps the "Late night" card
- **THEN** the detail shows "Late night", "42 songs · 2h 58m", Shuffle play, Play, and the 42 rows in playlist order

#### Scenario: Empty playlist
- **WHEN** the user opens a playlist with no tracks
- **THEN** the detail shows "0 songs" and an empty state titled "No songs yet" with a hint to add songs from Folders and an "Open Folders" action; Play and Shuffle play do nothing; tapping "Open Folders" shows the Folders tab

#### Scenario: Filter by title or artist
- **WHEN** the user taps Search on "Late night" and types "glass"
- **THEN** only "Glass Rain" is listed; typing "hyaline" instead lists the same row by its artist; tapping it starts the playlist from "Glass Rain"

#### Scenario: No match
- **WHEN** the search text is "zzz"
- **THEN** the list is replaced by "No songs match “zzz”", and clearing the text lists every track again

### Requirement: Reorder and remove
Dragging a row by its handle SHALL move it to the drop position and the new order SHALL be persisted. Swiping a row away SHALL remove that track from the playlist (not from the device). While a search filter is active the rows SHALL NOT show a drag handle and SHALL NOT be reorderable; swiping a filtered row away SHALL still remove that track.

#### Scenario: Drag to reorder
- **WHEN** the user drags the second row above the first and releases, then leaves and reopens the playlist
- **THEN** the former second track is listed first

#### Scenario: Swipe to remove
- **WHEN** the user swipes a row off the screen
- **THEN** the track disappears from the playlist and the count decreases by one

#### Scenario: Reorder is off while filtering
- **WHEN** a search filter narrows the list
- **THEN** no row shows a drag handle, and clearing the filter brings the handles back

#### Scenario: Swipe still removes while filtering
- **WHEN** the user swipes a filtered row off the screen
- **THEN** that track is removed from the playlist and the count decreases by one

## ADDED Requirements

### Requirement: Search across playlists
From the Playlists tab the user SHALL be able to open a search screen that hides the bottom navigation and takes focus in its field. While the text is blank the screen SHALL show a hint; otherwise it SHALL list every track of every playlist whose title or artist contains the text (case-insensitive), ordered by playlist (home-grid order) then position, each row showing the title, "<artist or Unknown artist> · <playlist name>", the duration, and an "Open playlist" action. A song in two playlists SHALL appear twice. Tapping a row SHALL play that playlist from that track (missing tracks excluded, origin = the playlist name); "Open playlist" SHALL push the playlist detail above the search. No match SHALL read "No songs match “<text>”". The search text SHALL survive rotation and a playlist detail opened from a result. Back SHALL return to the Playlists tab.

#### Scenario: Type to search
- **WHEN** the user taps Search on the Playlists tab and types "rain"
- **THEN** the bottom bar is hidden and "Glass Rain — Hyaline · Late night" and "Glass Rain — Hyaline · Focus" are listed, in that order

#### Scenario: Play from a result
- **WHEN** the user taps the "Focus" hit
- **THEN** "Focus" starts playing from "Glass Rain", the mini player appears, and Now Playing reports the origin "Focus"

#### Scenario: Open the playlist from a result
- **WHEN** the user taps "Open playlist" on the "Late night" hit
- **THEN** the "Late night" detail opens; back returns to the search with "rain" still typed and the same hits

#### Scenario: No matches
- **WHEN** the text is "zzz"
- **THEN** "No songs match “zzz”" is shown

#### Scenario: Back returns to Playlists
- **WHEN** the user presses back on the search screen
- **THEN** the Playlists tab is shown with the bottom bar

#### Scenario: Query survives rotation
- **WHEN** the user rotates the device with "rain" typed
- **THEN** the search screen still shows "rain" and its hits
