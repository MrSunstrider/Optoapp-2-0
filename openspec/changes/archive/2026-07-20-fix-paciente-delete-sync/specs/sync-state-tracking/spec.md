# Delta for Sync State Tracking

## ADDED Requirements

| # | Requirement | Fix |
|---|-------------|-----|
| R1 | Download Phase 1 retries pending remote deletes | F1 |
| R2 | Download Phase 2 guards re-insertion via skipIds | F1 |
| R3 | savePaciente stamps updatedAt before upsert | F3 |
| R4 | Download Phase 1 propagates CancellationException | F4 |
| R5 | savePaciente requires admin or gerente role | F6 |

### Requirement: Download Phase 1 SHALL retry pending remote deletes

The system MUST retry pending remote `paciente` deletions during `download()` Phase 1, before the download loop. A prior local delete with a failed remote delete leaves a tombstone. Phase 1 retries the remote DELETE; on IOException it logs and preserves the tombstone — Phase 2 `skipIds` then prevents re-insertion.

#### Scenario: Remote delete retry succeeds

- GIVEN a tombstone for paciente `"P1"`
- WHEN Phase 1 retries the remote DELETE and it succeeds
- THEN `clearEntityState(opticaId, "paciente", "P1")` is called

#### Scenario: Remote delete retry fails — tombstone preserved

- GIVEN a tombstone for `"P1"` and remote DELETE throws IOException
- WHEN Phase 1 retry runs
- THEN the error is logged and the tombstone is NOT cleared

### Requirement: Download Phase 2 SHALL skip re-insertion via skipIds

The system MUST combine `conflictDao.getConflictEntityIds()` and pending-deletion tombstones into a `skipIds` set. Remote entities whose ID is in `skipIds` MUST be skipped (`return@forEach`) during download, preventing resurrection after partial delete.

#### Scenario: Pending-delete ID skipped

- GIVEN a tombstone for `"P1"` exists AND `"P1"` is still on Supabase
- WHEN `download()` Phase 2 runs
- THEN `"P1"` is in `skipIds` and is NOT upserted to Room

#### Scenario: No tombstones — all inserted

- GIVEN zero pending deletions for the optica
- WHEN `download()` runs
- THEN all remote pacientes are upserted normally

### Requirement: savePaciente SHALL stamp updatedAt before upsert

`OptoRepository.insertPaciente` MUST set `updatedAt = Instant.now().toString()` before passing the entity to Room. Every create or edit MUST produce a fresh timestamp so conflict detection works correctly.

#### Scenario: New paciente gets fresh updatedAt

- GIVEN a Paciente with `updatedAt = null`
- WHEN `OptoRepository.insertPaciente` is called
- THEN the stored entity has a non-null `updatedAt` equal to the current instant

#### Scenario: Edit refreshes updatedAt

- GIVEN a Paciente with `updatedAt = "2025-01-01T00:00:00Z"`
- WHEN `OptoRepository.insertPaciente` is called
- THEN the stored `updatedAt` is greater than the previous value

### Requirement: Download Phase 1 SHALL propagate CancellationException

The Phase 1 inner try/catch MUST include `catch (e: CancellationException) { throw e }` before `catch (e: IOException)`. Without this, `CancellationException` is caught by the outer `catch (e: Exception)` and silently swallowed, blocking coroutine cancellation.

#### Scenario: Cancellation during Phase 1 propagates

- GIVEN the coroutine scope is cancelled while Phase 1 iterates tombstones
- WHEN `download()` Phase 1 runs
- THEN `CancellationException` is rethrown, not swallowed by the outer catch

#### Scenario: IOException does not cancel

- GIVEN a pending-delete retry throws IOException
- WHEN Phase 1 catches it
- THEN the error is logged and the loop continues to the next tombstone

### Requirement: savePaciente SHALL require admin or gerente role

`PacienteViewModel.savePaciente` MUST call `AuthorizationGuard.requireRole(role, setOf("admin", "gerente"), "guardar paciente")` before performing the save, matching the pattern in `deletePacienteGuarded`.

#### Scenario: Admin saves successfully

- GIVEN role = `"admin"`
- WHEN `savePaciente` is called
- THEN the guard passes and the paciente is saved

#### Scenario: Non-admin is denied

- GIVEN role = `"vendedor"`
- WHEN `savePaciente` is called
- THEN `IllegalArgumentException` is thrown with message containing "Unauthorized"
- AND the paciente is NOT saved
