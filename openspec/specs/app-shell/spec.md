# app-shell Specification

## Purpose
The single-activity application shell: a two-tab bottom navigation between Playlists and Folders, predictable back-stack behaviour, retention of each tab's state, and the entry point to Settings. Every later capability renders inside this shell.

## Requirements

### Requirement: Two-tab bottom navigation
The app SHALL present a bottom navigation bar with exactly two destinations, **Playlists** and **Folders**, each with an icon and label. The currently selected destination SHALL be visually distinct from the unselected one. The app SHALL open on the Playlists destination.

#### Scenario: Cold launch lands on Playlists
- **WHEN** the app is launched with the audio permission already granted
- **THEN** the Playlists destination is shown and the Playlists tab is highlighted as selected

#### Scenario: Switching to Folders
- **WHEN** the user taps the Folders tab
- **THEN** the Folders destination is shown and the Folders tab becomes the selected tab

#### Scenario: Re-tapping the selected tab
- **WHEN** the user taps the tab that is already selected
- **THEN** the destination remains shown and no content or scroll position is lost

### Requirement: Tab state is retained
Switching between tabs SHALL preserve each tab's UI state (such as scroll position) so that returning to a tab shows it as it was left. Configuration changes such as rotation SHALL preserve the selected tab.

#### Scenario: Returning to a tab
- **WHEN** the user scrolls the Folders destination, switches to Playlists, then switches back to Folders
- **THEN** Folders is shown at the same scroll position it was left at

#### Scenario: Rotation keeps the selected tab
- **WHEN** the device is rotated while the Folders tab is selected
- **THEN** the Folders tab is still selected after the rotation

### Requirement: Back navigation
The system back action SHALL behave predictably: from any non-default tab it returns to the Playlists tab; from the Playlists tab with nothing pushed above it, it exits the app.

#### Scenario: Back from Folders
- **WHEN** the user is on the Folders tab and presses system back
- **THEN** the Playlists tab is shown and selected

#### Scenario: Back from Playlists exits
- **WHEN** the user is on the Playlists tab with no screen pushed above it and presses system back
- **THEN** the app moves to the background (exits) rather than showing another screen

### Requirement: Placeholder tab content
Until the playlists capability exists, the Playlists tab SHALL show a clearly labelled placeholder (the destination title and a short "nothing here yet" message) rather than a blank screen. The Folders tab shows real content as specified by the `folder-browser` capability.

#### Scenario: Placeholder visible
- **WHEN** the user opens the Playlists tab with the audio permission granted
- **THEN** the tab's title and a placeholder message are visible and the screen is not blank

### Requirement: Settings entry point
The Playlists destination SHALL expose an overflow action that opens a Settings screen. Settings SHALL be a full-screen destination pushed above the tabs: it SHALL show a back affordance, SHALL NOT show the bottom navigation bar, and leaving it SHALL return the user to exactly where they were.

#### Scenario: Opening Settings
- **WHEN** the user taps the overflow action on the Playlists destination and chooses Settings
- **THEN** the Settings screen is shown with a title, a back affordance, and no bottom navigation bar

#### Scenario: Leaving Settings
- **WHEN** the user presses the back affordance or system back on the Settings screen
- **THEN** the Playlists destination is shown with the Playlists tab selected and the bottom navigation bar visible

#### Scenario: Settings stub content
- **WHEN** the Settings screen is shown in this change
- **THEN** it displays a footer stating the app version and that the app has no INTERNET permission, and no other functional controls yet

### Requirement: Detail screens hide the bottom navigation
A detail screen pushed above a tab (such as a folder's song list) SHALL NOT show the bottom navigation bar. Leaving it SHALL return to the tab it was opened from, with that tab selected and the bottom navigation bar visible.

#### Scenario: Detail hides the bar
- **WHEN** the user opens a folder from the Folders tab
- **THEN** the folder detail is shown without the bottom navigation bar

#### Scenario: Back restores the tab
- **WHEN** the user presses back on a folder detail
- **THEN** the Folders tab is shown, selected, with the bottom navigation bar visible
