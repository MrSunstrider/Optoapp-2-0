## ADDED Requirements

### Requirement: Daily delete quota is charged once per patient deletion attempt chain
The client MUST NOT increment the daily patient-delete counter when a pending deletion tombstone for that patient already exists. The client MUST increment at most once when creating a new local delete+tombstone for a patient that was not already pending deletion.

#### Scenario: Remote fails then user retries
- **GIVEN** local delete and tombstone already exist for patient P
- **AND** remote DELETE previously failed
- **WHEN** the user confirms delete again for P
- **THEN** the daily counter MUST NOT increment again
- **AND** the client MUST retry remote DELETE only

#### Scenario: First local delete
- **GIVEN** no pending tombstone for patient P
- **WHEN** local delete+tombstone succeeds
- **THEN** the daily counter MUST increment exactly once before remote DELETE

### Requirement: Partial delete success leaves the detail screen
When local delete succeeded and remote DELETE failed, the UI MUST close the delete dialog and navigate away from patient detail (same as full success navigation), while still showing the incomplete-sync message.

#### Scenario: Local success remote failure
- **GIVEN** user confirms delete on DetallePaciente
- **WHEN** delete returns local-success with remote pending/error
- **THEN** the delete dialog is dismissed
- **AND** the app pops back from detail
- **AND** a toast explains remote sync will retry
