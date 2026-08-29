## Purpose

The single runtime permission the app depends on: reading audio files on the device. Covers which permission is requested on which Android version, how the app behaves before it is granted, and how it recovers when it is denied, permanently denied, or revoked.

## ADDED Requirements

### Requirement: Exactly one audio-read permission
The app SHALL request exactly one runtime permission to read audio: `READ_MEDIA_AUDIO` on Android 13 (API 33) and later, and `READ_EXTERNAL_STORAGE` on Android 8.0 through 12L (API 26–32). The app SHALL NOT request any other runtime permission in this change and SHALL NOT declare the `INTERNET` permission.

#### Scenario: Android 13 or later
- **WHEN** the app requests audio access on a device running Android 13 or later
- **THEN** the system permission dialog is for `READ_MEDIA_AUDIO` only

#### Scenario: Android 12 or earlier
- **WHEN** the app requests audio access on a device running Android 8.0 through 12L
- **THEN** the system permission dialog is for `READ_EXTERNAL_STORAGE` only

#### Scenario: No network permission
- **WHEN** the installed app's declared permissions are inspected
- **THEN** `INTERNET` is not among them

### Requirement: Gated empty state before permission is granted
While the audio-read permission is not granted, the app SHALL show a "No music found" state in place of tab content. The state SHALL explain that the app needs permission to read audio files, state that nothing leaves the device because the app has no internet access, and offer an "Allow access to audio" action that triggers the system permission request.

#### Scenario: First launch without permission
- **WHEN** the app is launched and the audio-read permission has never been granted
- **THEN** the "No music found" state is shown with the explanation and the "Allow access to audio" action, and no tab content is shown

#### Scenario: Requesting permission
- **WHEN** the user taps "Allow access to audio" and the permission can still be requested
- **THEN** the system permission dialog is shown

### Requirement: Grant is applied immediately
When the permission is granted, the app SHALL replace the "No music found" state with the tab content without requiring a restart.

#### Scenario: Grant from the dialog
- **WHEN** the user grants the permission in the system dialog
- **THEN** the currently selected tab's content is shown in place of the empty state

### Requirement: Denial can be retried
When the user denies the permission but the system will still show the dialog, the empty state SHALL remain and the "Allow access to audio" action SHALL trigger the dialog again.

#### Scenario: Deny once
- **WHEN** the user denies the permission in the system dialog and then taps "Allow access to audio" again
- **THEN** the system permission dialog is shown again

### Requirement: Permanent denial routes to system settings
When the permission has been denied such that the system will no longer show the dialog, the empty state SHALL indicate that access must be enabled in system settings and the action SHALL open the app's system settings page.

#### Scenario: Deny with "don't ask again"
- **WHEN** the user has permanently denied the permission and taps the empty state's action
- **THEN** the app's page in system Settings is opened

#### Scenario: Granted in system settings
- **WHEN** the user grants the permission from system Settings and returns to the app
- **THEN** the tab content is shown without relaunching the app

### Requirement: Revocation is detected on return
The app SHALL re-evaluate the permission every time it returns to the foreground. If the permission was revoked while the app was in the background, the app SHALL show the "No music found" state on return.

#### Scenario: Revoked while backgrounded
- **WHEN** the user revokes the permission in system Settings while the app is in the background, then returns to the app
- **THEN** the "No music found" state is shown in place of tab content
