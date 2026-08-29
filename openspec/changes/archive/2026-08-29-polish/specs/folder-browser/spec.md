## MODIFIED Requirements

### Requirement: Short audio is hidden
The library SHALL exclude audio whose duration is below the "Skip short audio" threshold chosen in Settings (default 30 seconds; Off hides nothing), as specified by the `settings` capability. The threshold SHALL be applied when the library is queried, and the loaded library SHALL record the threshold it was queried with.

#### Scenario: Clip under the threshold
- **WHEN** an audio file of 5 seconds flagged as music is present in the media index and the threshold is 30 s
- **THEN** it is not counted in any folder and does not appear in any song list

#### Scenario: Track above the threshold
- **WHEN** an audio file of 35 seconds flagged as music is present in the media index and the threshold is 30 s
- **THEN** it appears in its folder's song list and count

#### Scenario: Threshold off
- **WHEN** the threshold is Off and the same 5-second file is present
- **THEN** it appears in its folder's song list and count

### Requirement: Folder list
The Folders tab SHALL list every folder that contains at least one song, sorted by name (case-insensitive), each row showing the folder name and its song count. The header SHALL show the title "Folders" and the total number of songs in the library. A footer SHALL state that pull-to-refresh is available and, unless the threshold is Off, that audio under the applied threshold is hidden.

#### Scenario: Folder rows
- **WHEN** the library contains songs in folders "Download" (1 song), "Music" (2 songs) and "Rock" (1 song)
- **THEN** the Folders tab shows rows "Download · 1 song", "Music · 2 songs", "Rock · 1 song" in that order and the header total reads "4 songs"

#### Scenario: Alphabetical ignores case
- **WHEN** folders named "beta" and "Alpha" both contain songs
- **THEN** "Alpha" is listed before "beta"

#### Scenario: Footer reflects the threshold
- **WHEN** the threshold is 15 s
- **THEN** the footer reads "Pull to refresh · audio under 15s hidden"; with the threshold Off it reads "Pull to refresh"

### Requirement: Folder detail
Tapping a folder SHALL open a detail screen pushed above the tabs, titled with the folder name and showing its song count, listing that folder's songs sorted by title (case-insensitive). Each row SHALL show the title, the artist or "Unknown artist" when the file has no artist tag, and the duration formatted as minutes:seconds (hours:minutes:seconds when an hour or longer). The screen SHALL provide a back affordance; back (affordance or system back) SHALL return to the Folders tab with the tab selected. Tapping a song SHALL start playback of the folder from that song, as specified by the `playback` capability, and the row of the song currently playing SHALL be visually highlighted. Long-pressing a song SHALL enter selection mode as specified by the `multi-select` capability; while selecting, tapping a row toggles its selection instead of playing. A folder with no songs SHALL show an empty state with a "Back to folders" action. The library SHALL be re-queried when the app returns to the foreground, so songs deleted while the app was away disappear from the list (and the folder, if emptied, from the Folders tab) without a manual rescan.

#### Scenario: Open a folder
- **WHEN** the user taps the "Rock" row containing "Beta Song" (41 s, no artist tag)
- **THEN** a screen titled "Rock" with "1 song" is shown, listing "Beta Song", "Unknown artist", "0:41"

#### Scenario: Back returns to Folders
- **WHEN** the user presses the back affordance or system back on a folder detail screen
- **THEN** the Folders tab is shown with the Folders tab selected

#### Scenario: Tagged artist and long duration
- **WHEN** a song has artist "Nocturne Ave" and duration 1 h 2 min 5 s
- **THEN** its row shows "Nocturne Ave" and "1:02:05"

#### Scenario: Song tap is inert
- **WHEN** the user taps a song row
- **THEN** the tap is no longer inert: playback starts as described in "Song tap plays the folder"

#### Scenario: Song tap plays the folder
- **WHEN** the user taps "Glass Rain" in the "Music" folder
- **THEN** "Glass Rain" starts playing with the rest of the folder queued after it, the mini player appears, and the "Glass Rain" row is highlighted as the current song

#### Scenario: Long-press enters selection
- **WHEN** the user long-presses "Glass Rain"
- **THEN** selection mode starts with "Glass Rain" selected and nothing starts playing

#### Scenario: Empty folder
- **WHEN** the user opens a folder whose songs have all been removed
- **THEN** an empty state titled "Nothing here yet" with a "Back to folders" action is shown, and the action returns to the Folders tab

#### Scenario: File deleted while the app was away
- **WHEN** the user leaves the app, a song's file is deleted and the media index updated, and the user returns to the open folder detail
- **THEN** the song's row is gone and the count is one lower, without pulling to refresh

### Requirement: Empty library state
When the permission is granted but the query returns no songs, the Folders tab SHALL show an empty state that says no music was found, suggests copying music into a folder such as /Music, notes that audio under the applied threshold is hidden (omitted when the threshold is Off), and offers a "Rescan" action that re-queries the library.

#### Scenario: No music on the device
- **WHEN** the audio permission is granted and the media index contains no music above the threshold
- **THEN** the Folders tab shows the empty state with the "Rescan" action instead of a folder list

#### Scenario: Rescan finds music
- **WHEN** music is added and indexed and the user taps "Rescan"
- **THEN** the folder list replaces the empty state
