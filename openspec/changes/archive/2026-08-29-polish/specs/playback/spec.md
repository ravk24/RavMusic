## MODIFIED Requirements

### Requirement: Missing files are skipped
If a queued song's file can no longer be opened when its turn comes, playback SHALL skip to the next song in the queue instead of stopping. If it was the last song in the queue, playback SHALL stop as at the end of the queue. In both cases the service SHALL report the skip to connected UI, which SHALL show a brief notice "Couldn't play <title> — skipped"; no error dialog SHALL be shown.

#### Scenario: Deleted file mid-queue
- **WHEN** the second of three queued songs has been deleted from storage and the first song ends
- **THEN** the third song starts playing and the notice names the second song

#### Scenario: Deleted last file
- **WHEN** the last queued song has been deleted and the previous song ends
- **THEN** playback stops without an error being shown, and the notice names the deleted song

#### Scenario: Notice is transient and one-shot
- **WHEN** a skip has been reported and the user opens another screen
- **THEN** the notice is not shown again for the same skip
