## ADDED Requirements

### Requirement: Local patient-delete quota is scoped per user and óptica
The device daily delete counter MUST be keyed by óptica id, authenticated user identity (normalized email), and UTC calendar day. Distinct users on the same device MUST NOT share the same counter for the same óptica.

#### Scenario: Two users same óptica same day
- **GIVEN** user A@x.com has incremented the counter for óptica O today
- **WHEN** user B@y.com is the active session for óptica O
- **THEN** B's remaining local deletes MUST start independent of A's counter

### Requirement: Pacientes upload aborts on proven double fetch failure even without updatedAt
When batch remote fetch failed and conflict filtering still marks every pending local paciente as upload-safe, the upload MUST abort and mark `upload_pacientes`/`batch` error, whether or not local rows have non-null `updatedAt`.

#### Scenario: All null updatedAt under batch fetch failure
- **GIVEN** batch remote fetch failed
- **AND** local pending pacientes all have `updatedAt = null`
- **AND** filterConflicts returns all of them as safe
- **WHEN** upload runs
- **THEN** zero rows are upserted remotely
- **AND** sync state records a double-fetch failure for the batch
