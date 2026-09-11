# Design: fix-pacientes-jd-residuals

## Technical Approach

Client-only patch closing three Judgment Day pacientes residuals. (1) `SyncPacientesUseCase.download()` fail-closed when Phase 2 guard queries fail. (2) `NuevoPacienteScreen` save gate + strict shared birth-date parse in `DateUtils`. (3) `SessionManager.dailyPacienteDeleteKey` keyed on UTC calendar day. Builds on completed `fix-pacientes-jd-quota-refresh`; no Supabase changes.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Pacientes guard failure | Fail-closed: 0 upserts, `markError`, no fetch | Fail-open (`emptySet()`); skip-all without fetch | Prevents clobbering conflict winners/tombstones; matches FR-01 delta |
| Guard retry | One inline retry per query, then fail-closed | No retry; multi-retry with backoff | Handles transient Room locks; low complexity |
| Phase 1 pending-delete retry | Unchanged (log-and-continue on outer failure) | Fail-closed Phase 1 too | Out of scope; Phase 2 guard is the resurrection risk |
| Batch error entity type | `download_pacientes` / `batch` | `download_paciente`; per-entity errors | Mirrors existing `upload_pacientes` batch pattern |
| Fecha validation | Screen gate + `DateUtils.parseBirthDateFromDigits` | ViewModel-only; lenient `fromDisplayFormat` | Minimal diff; single strict parse source; blank stays optional |
| `validateFechaNacimiento` | Thin wrapper over shared parse | Duplicate rules in screen | Keeps UI messages; tests stay in `PacienteFormSectionsTest` |
| UTC delete key | `LocalDate.now(ZoneOffset.UTC)` via `DateUtils.utcToday()` | Device TZ; server RPC gate | Aligns with `guard_pacientes_delete` / RPC audit; offline-first preserved |
| Other sync modules | Fail-open unchanged | Broad fail-closed rollout | Explicitly out of scope; note pattern for follow-up |

## Data Flow — Download Guard Failure

Phase 1 (pending-delete remote retry) runs first and is unchanged. Phase 2 assembles `skipIds`; failure short-circuits before network fetch.

```mermaid
sequenceDiagram
    participant DL as download()
    participant P1 as Phase 1 tombstone retry
    participant CD as ConflictDao
    participant SD as SyncEntityStateDao
    participant ST as SyncStateTracker
    participant NET as fetchRemotePacientesForDownload
    participant REPO as OptoRepository

    DL->>P1: getPendingDeletions + remote DELETE retry
    Note over P1: Outer catch logs; continues on error

    DL->>CD: getConflictEntityIds (retry x1)
    alt throws (non-cancellation)
        CD-->>DL: Exception
        DL->>ST: markError(opticaId, download_pacientes, batch, msg)
        DL-->>DL: return 0 (no NET, no upserts)
    end

    DL->>SD: getPendingDeletions Phase 2 (retry x1)
    alt throws (non-cancellation)
        SD-->>DL: Exception
        DL->>ST: markError(...)
        DL-->>DL: return 0
    end

    DL->>NET: fetch remote rows
    loop each remote not in skipIds
        DL->>REPO: upsertPaciente
    end
    DL-->>DL: return upserted count
```

**Cancellation**: `CancellationException` from guard queries propagates; no `markError`, no fail-closed swallow.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/SyncPacientesUseCase.kt` | Modify | Extract `buildPacienteSkipIds()` with per-query retry; fail-closed returns 0 before fetch; `markError` on guard failure |
| `domain/SyncPacientesUseCaseDownloadGuardTest.kt` | Modify | RED→GREEN: guard throw → 0 upserts; Phase 2 deletion throw; cancellation propagates; extend `FakeConflictDao` with throw hook |
| `test/.../FakeConflictDao.kt` | Modify | Optional `throwOnGetConflictEntityIds` for failure tests |
| `util/DateUtils.kt` | Modify | Add `utcToday()`, `parseBirthDateFromDigits(digits): LocalDate?` (strict `LocalDate.of`) |
| `ui/components/paciente/PacienteFormSections.kt` | Modify | Refactor `validateFechaNacimiento` to delegate to strict parse |
| `ui/screens/NuevoPacienteScreen.kt` | Modify | Save gate: block when non-blank + validation error; toast; parse via `parseBirthDateFromDigits` |
| `data/SessionManager.kt` | Modify | `dailyPacienteDeleteKey` uses `DateUtils.utcToday()` |
| `test/DateUtilsTest.kt` | Modify | Strict parse + `utcToday` boundary cases |
| `test/PacienteViewModelTest.kt` | Modify | UTC key fixture if delete-quota tests assume local date |

## Interfaces / Contracts

```kotlin
// DateUtils — new public API
fun utcToday(): LocalDate = LocalDate.now(ZoneOffset.UTC)

/** 8-digit ddMMyyyy; null if blank, incomplete, or invalid calendar date */
fun parseBirthDateFromDigits(digits: String): LocalDate?

// SyncPacientesUseCase — private
private suspend fun buildPacienteSkipIds(opticaId: String): Set<String>?
// Returns skipIds on success; null signals fail-closed (caller returns 0)
```

Save gate condition (screen layer):

```kotlin
fechaNacimiento.isNotBlank() && validateFechaNacimiento(fechaNacimiento) != null → block + toast
else → parseBirthDateFromDigits(fechaNacimiento) for Paciente.fechaNacimiento
```

## Testing Strategy (TDD)

`strict_tdd: true` — one RED test per scenario, then minimal GREEN, then refactor.

| Layer | RED first | Assert |
|-------|-----------|--------|
| Unit — sync | `getConflictEntityIds` throws → 0 downloaded, no `upsertPaciente`, `markError(download_pacientes, batch)` | `SyncPacientesUseCaseDownloadGuardTest` + `TestableDownloadUseCase` |
| Unit — sync | Phase 2 `getPendingDeletions` throws → same fail-closed | Mock `syncStateDao` throw on second call |
| Unit — sync | Guard `CancellationException` propagates | Existing Phase 1 pattern extended |
| Unit — sync | Happy path conflict skip | Existing tests unchanged |
| Unit — fecha | `parseBirthDateFromDigits("31022020")` → null; `"15061990"` → 1990-06-15 | `DateUtilsTest` |
| Unit — fecha | `validateFechaNacimiento` regression | `PacienteFormSectionsTest` pass |
| Unit — fecha | Save blocked logic extracted to testable predicate or structural test on `saveAction` branches | New small test class if needed; prefer pure-function tests over Compose |
| Unit — UTC | Key contains UTC `yyyyMMdd` when local ≠ UTC | `DateUtilsTest.utcToday` + documented instant, or package-visible key builder test |
| Regression | Full suite | `./gradlew :optoapp:testDebugUnitTest --stacktrace` |

Gate before push: full `testDebugUnitTest`. **400-line budget risk: Low** (~80–150 authored lines).

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration. Deploy with app release. UTC quota reset occurs at UTC midnight (document if support tickets arise). Transient Room errors may skip one pacientes download cycle until next sync — acceptable vs data loss.

## Rollback

1. Revert `SyncPacientesUseCase` fail-closed guard → restore `emptySet()` catch blocks.
2. Revert `NuevoPacienteScreen` save gate; restore lenient `fromDisplayFormat` path for fecha.
3. Revert `SessionManager.dailyPacienteDeleteKey` to `LocalDate.now()` (device TZ).
4. Remove `DateUtils.utcToday` / `parseBirthDateFromDigits` if unused; revert test additions.
5. Revert delta specs. No DB rollback.

## Open Questions

- [ ] None blocking — spec deltas already define FR-01 pacientes exception and fecha/UTC requirements.
