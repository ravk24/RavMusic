## ADDED Requirements

### Requirement: Theme override
The Settings screen SHALL offer a theme choice of **System**, **Light** and **Dark**, defaulting to System. The choice SHALL be persisted on the device, SHALL apply immediately to every screen and to the system bar icon colours, and SHALL survive the app being killed and relaunched. System SHALL follow the device dark-mode setting as specified by the `theme` capability.

#### Scenario: Dark on a light system
- **WHEN** the device is in light mode and the user chooses Dark
- **THEN** the app immediately renders the dark palette with light status-bar icons, and Settings shows Dark selected

#### Scenario: Survives relaunch
- **WHEN** the user chooses Light, the app process is killed, and the app is opened again on a device in dark mode
- **THEN** the app opens in the light palette

#### Scenario: Back to System
- **WHEN** Dark is selected on a light-mode device and the user chooses System
- **THEN** the app returns to the light palette

### Requirement: Short-audio threshold
The Settings screen SHALL offer a "Skip short audio" threshold of **Off**, **15 s**, **30 s**, **1 min** and **2 min**, defaulting to 30 s. Changing it SHALL persist the value and re-query the library so folders and song lists reflect the new threshold without a manual rescan. Audio shorter than the threshold SHALL be hidden as specified by the `folder-browser` capability; Off SHALL hide nothing.

#### Scenario: Threshold off reveals a clip
- **WHEN** a 5-second clip flagged as music is on the device and the user chooses Off
- **THEN** after the re-query the clip appears in its folder and in the song count

#### Scenario: Threshold raised hides a song
- **WHEN** a 65-second song is listed and the user chooses 2 min
- **THEN** after the re-query the song is no longer listed and its folder's count drops

#### Scenario: Threshold persists
- **WHEN** the user chooses 15 s and relaunches the app
- **THEN** Settings shows 15 s selected and the library is queried with a 15-second threshold

### Requirement: Rescan and library info
The Settings screen SHALL show when the library was last queried and how many songs it holds ("Last scan · <relative time> · N songs"), and SHALL offer a **Rescan library** action that re-queries the library the same way pull-to-refresh does, showing progress while it runs. Before the first query the info line SHALL say the library has not been scanned yet.

#### Scenario: Rescan
- **WHEN** a new song has been copied and indexed and the user taps "Rescan library"
- **THEN** progress is shown, then the info line updates to the new song count and "just now"

#### Scenario: Not yet scanned
- **WHEN** Settings is opened before the library has been queried (for example without the audio permission)
- **THEN** the info line reads "Library not scanned yet" and Rescan is disabled
