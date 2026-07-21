# Delta for Sync Conflict

## ADDED Requirements

### Requirement: resolveDuplicatePacientesByHistoria SHALL accumulate merged data across multiple iterations

When 3 or more pacientes share the same `historiaOptometrica`, `PacienteRepository.resolveDuplicatePacientesByHistoria` MUST accumulate merged data through each iteration. The canonical paciente variable MUST be mutable (`var`) and MUST be reassigned to the merge result after each duplicate is processed. The current implementation uses `val canonical`, which causes data from the first merged duplicate to be lost when processing subsequent duplicates — only the last merge result is persisted.

#### Scenario: Three duplicates — all fields preserved

- GIVEN three pacientes A (oldest), B, C with same HO
- AND A has `email = "a@test.com"`, B has `telefono = "111"`, C has `direccion = "Calle 1"`
- WHEN `resolveDuplicatePacientesByHistoria` runs
- THEN the final merged record has `email = "a@test.com"`, `telefono = "111"`, `direccion = "Calle 1"`
- AND the merged record has `id = A.id` (oldest survives)
- AND B and C are deleted from Room

#### Scenario: Two duplicates — no regression

- GIVEN two pacientes A and B with same HO
- WHEN `resolveDuplicatePacientesByHistoria` runs
- THEN A absorbs non-blank fields from B and B is deleted
- AND behavior is identical to before the fix (no regression)

### Requirement: Upload SHALL abort on double fetch failure instead of silently bypassing conflict detection

During `upload()`, when `fetchAllRemotePacientes` returns an empty list AND local entities exist with timestamps, the system MUST NOT treat all local entities as conflict-safe. The existing code sets `effectiveRemoteMap = null` to fall back to per-entity `fetchRemoteUpdatedAt` queries. However, if those per-entity queries also fail (double network failure), the upload loop MUST NOT silently proceed — it SHALL either abort the upload with an error result, or at minimum log a prominent warning before proceeding. An empty `effectiveRemoteMap` alone (without per-entity success) is insufficient; the upload loop SHALL verify that conflict filtering actually executed before marking entities as synced.

#### Scenario: Double network failure during upload — sync returns error

- GIVEN local pacientes exist with `updatedAt` timestamps
- AND `fetchAllRemotePacientes` returns empty (first failure)
- AND per-entity `fetchRemoteUpdatedAt` also fails (second failure)
- WHEN `upload()` runs
- THEN `filterConflicts` reports that conflict detection could not complete
- AND sync returns an error result or logs a prominent warning
- AND entities are NOT silently uploaded as conflict-free

#### Scenario: Normal batch fetch fails but per-entity succeeds — upload proceeds

- GIVEN `fetchAllRemotePacientes` returns empty
- AND per-entity `fetchRemoteUpdatedAt` succeeds for each local entity
- WHEN `upload()` runs
- THEN `effectiveRemoteMap = null` triggers per-entity fallback
- AND conflict detection completes normally
- AND safe entities are uploaded
