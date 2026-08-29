## MODIFIED Requirements

### Requirement: Settings entry point
The Playlists destination SHALL expose an overflow action that opens a Settings screen. Settings SHALL be a full-screen destination pushed above the tabs: it SHALL show a back affordance, SHALL NOT show the bottom navigation bar, and leaving it SHALL return the user to exactly where they were. Its content is specified by the `settings` capability.

#### Scenario: Opening Settings
- **WHEN** the user taps the overflow action on the Playlists destination and chooses Settings
- **THEN** the Settings screen is shown with a title, a back affordance, and no bottom navigation bar

#### Scenario: Leaving Settings
- **WHEN** the user presses the back affordance or system back on the Settings screen
- **THEN** the Playlists destination is shown with the Playlists tab selected and the bottom navigation bar visible

#### Scenario: Settings stub content
- **WHEN** the Settings screen is shown
- **THEN** it is no longer a stub: it shows the Theme choice, the Skip short audio threshold, the library info line with Rescan library, and the footer stating the app version and that the app has no INTERNET permission
