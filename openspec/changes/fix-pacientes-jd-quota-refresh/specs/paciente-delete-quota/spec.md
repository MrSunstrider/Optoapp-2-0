# Spec: paciente-delete-quota

## ADDED Requirements

### Requirement: Remaining deletes RPC is per-user and membership-scoped
The system MUST expose `paciente_eliminaciones_restantes_hoy(p_optica_id text)` as SECURITY DEFINER with fixed `search_path`. Callers without `auth.uid()` or without membership in `usuario_optica` for `p_optica_id` MUST receive `0`. The count MUST include only `pacientes_delete_audit` rows for that optica where `deleted_by = auth.uid()` within the current UTC calendar day. The return value MUST be `GREATEST(0, 10 - count)`.

#### Scenario: Member with no deletes today
- **GIVEN** an authenticated user who is a member of óptica A
- **AND** the user has zero delete-audit rows for A today (UTC)
- **WHEN** they call `paciente_eliminaciones_restantes_hoy(A)`
- **THEN** the function returns 10

#### Scenario: Non-member
- **GIVEN** an authenticated user who is not a member of óptica B
- **WHEN** they call `paciente_eliminaciones_restantes_hoy(B)`
- **THEN** the function returns 0

### Requirement: Local daily delete quota consumes on local delete
When an authorized admin/gerente deletes a patient, after a successful local Room delete and sync tombstone the client MUST increment the device daily delete counter before attempting the remote DELETE. A subsequent remote network failure MUST still leave the quota consumed so further local deletes that day cannot exceed 10 via repeated remote failures.

#### Scenario: Remote fails after local delete
- **GIVEN** deletesToday = 0 and DAILY_DELETE_LIMIT = 10
- **WHEN** local delete succeeds and remote DELETE throws IOException
- **THEN** the daily counter is incremented
- **AND** the UI result is Error (incomplete sync message)

### Requirement: Patient list refresh cannot hang on empty
`PacienteViewModel.refresh()` MUST clear `isLoading` within 5 seconds even when the pacientes flow remains empty.

#### Scenario: Empty óptica pull-to-refresh
- **GIVEN** an óptica with zero patients
- **WHEN** refresh() is invoked
- **THEN** isLoading becomes false within 5 seconds
