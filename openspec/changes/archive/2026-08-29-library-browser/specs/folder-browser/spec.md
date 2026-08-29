## Purpose

The device music library as the user browses it: audio files grouped by the folder they are stored in, queried live from the system media index, with a folder list, a folder detail (song list), refresh, a short-audio filter, and an empty state.

## ADDED Requirements

### Requirement: Library is queried live from the system media index
The app SHALL obtain the list of songs by querying the system media index (MediaStore) for audio flagged as music, and SHALL NOT persist a copy of that list across process restarts. A refresh SHALL always perform a new query. The query SHALL run off the main thread so the UI stays responsive.

#### Scenario: Songs come from the media index
- **WHEN** the library is loaded with the audio permission granted
- **THEN** every song shown corresponds to an entry in the system media index flagged as music, and no song is shown that is absent from the index

#### Scenario: No persisted library
- **WHEN** the app process is restarted and the library is opened
- **THEN** the library is obtained by a new query, not from any file or database written by the app

#### Scenario: Query does not block the UI
- **WHEN** the library is being loaded or refreshed
- **THEN** the UI remains interactive and shows a loading indicator (first load) or the previous list (refresh) until the result arrives

### Requirement: Songs are grouped by storage folder
Each song SHALL belong to exactly one folder: the directory the file is stored in, identified by the system media index's folder ("bucket") identity on Android 10 and later, and by the same rule applied to the file path on Android 8.0–9. The folder's display name SHALL be the directory's own name (not its full path).

#### Scenario: Nested folders are separate
- **WHEN** `/Music/a.mp3` and `/Music/Rock/b.mp3` exist
- **THEN** the folder list shows "Music" containing `a.mp3` and "Rock" containing `b.mp3`, as two separate folders

#### Scenario: Same grouping on Android 8.0–9
- **WHEN** the same files are indexed on a device running Android 8.0–9
- **THEN** the folder list shows the same folder names and counts as on Android 10 or later

### Requirement: Short audio is hidden
The library SHALL exclude audio whose duration is below a minimum length of 30 seconds. The threshold SHALL be a single constant in code in this change.

#### Scenario: Clip under the threshold
- **WHEN** an audio file of 5 seconds flagged as music is present in the media index
- **THEN** it is not counted in any folder and does not appear in any song list

#### Scenario: Track above the threshold
- **WHEN** an audio file of 35 seconds flagged as music is present in the media index
- **THEN** it appears in its folder's song list and count

### Requirement: Folder list
The Folders tab SHALL list every folder that contains at least one song, sorted by name (case-insensitive), each row showing the folder name and its song count. The header SHALL show the title "Folders" and the total number of songs in the library. A footer SHALL state that pull-to-refresh is available and that short audio is hidden.

#### Scenario: Folder rows
- **WHEN** the library contains songs in folders "Download" (1 song), "Music" (2 songs) and "Rock" (1 song)
- **THEN** the Folders tab shows rows "Download · 1 song", "Music · 2 songs", "Rock · 1 song" in that order and the header total reads "4 songs"

#### Scenario: Alphabetical ignores case
- **WHEN** folders named "beta" and "Alpha" both contain songs
- **THEN** "Alpha" is listed before "beta"

### Requirement: Pull-to-refresh re-queries the library
Pulling down on the folder list SHALL re-query the media index. While the refresh is in progress the previous list SHALL remain visible with a refresh indicator; when it completes the list SHALL reflect the new result.

#### Scenario: New file appears after refresh
- **WHEN** a new music file is indexed after the folder list was loaded and the user pulls to refresh
- **THEN** the refreshed list includes the new file in its folder and the counts are updated

#### Scenario: Previous list stays visible
- **WHEN** the user pulls to refresh
- **THEN** the folders already shown remain visible until the new result replaces them

### Requirement: Folder detail
Tapping a folder SHALL open a detail screen pushed above the tabs, titled with the folder name and showing its song count, listing that folder's songs sorted by title (case-insensitive). Each row SHALL show the title, the artist or "Unknown artist" when the file has no artist tag, and the duration formatted as minutes:seconds (hours:minutes:seconds when an hour or longer). The screen SHALL provide a back affordance; back (affordance or system back) SHALL return to the Folders tab with the tab selected. Tapping a song SHALL have no effect in this change.

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
- **WHEN** the user taps a song row in this change
- **THEN** nothing happens (no navigation, no playback)

### Requirement: Empty library state
When the permission is granted but the query returns no songs, the Folders tab SHALL show an empty state that says no music was found, suggests copying music into a folder such as /Music, notes that short audio is hidden, and offers a "Rescan" action that re-queries the library.

#### Scenario: No music on the device
- **WHEN** the audio permission is granted and the media index contains no music above the threshold
- **THEN** the Folders tab shows the empty state with the "Rescan" action instead of a folder list

#### Scenario: Rescan finds music
- **WHEN** music is added and indexed and the user taps "Rescan"
- **THEN** the folder list replaces the empty state

### Requirement: Library follows the audio permission
The library SHALL be queried once the audio permission is granted, SHALL be cleared when the permission is no longer granted, and the "No music found" permission state SHALL replace both the folder list and the folder detail while the permission is missing.

#### Scenario: Loaded on grant
- **WHEN** the user grants the audio permission
- **THEN** the library is queried and the Folders tab shows the result without a relaunch

#### Scenario: Revoked while on a folder detail
- **WHEN** the permission is revoked in system settings while the app is backgrounded on a folder detail screen, and the user returns
- **THEN** the "No music found" permission state is shown in place of the song list
