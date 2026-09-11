# Exploration: fix-pacientes-jd-residuals

Close remaining Judgment Day pacientes residuals after APPROVED Round 1 on confirmed severes. Builds on `fix-pacientes-jd-quota-refresh` (RPC restore, local quota consumption, refresh timeout — already applied).

---

## Current State

### Residual 1 — SyncPacientesUseCase download guard fail-open

`SyncPacientesUseCase.download()` queries two local guard sets before upserting remote rows:

```233:248:optoapp/src/main/java/com/example/optoapp/domain/SyncPacientesUseCase.kt
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "paciente").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val deletedIds = try {
            syncStateTracker.dao.getPendingDeletions(opticaId)
                .filter { it.entityType == "paciente" }
                .map { it.entityId }
                .toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying deleted IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }
```

When either query throws (except `CancellationException`, which is not rethrown here), `skipIds` becomes empty and **all** remote pacientes are upserted — including entities with active `conflict_records` or pending delete tombstones. That can overwrite local conflict winners and resurrect locally deleted patients.

The same fail-open pattern exists in Historial/Inventario/Proveedores/OrdenesCompra, but scope for this change is **pacientes only**.

Existing tests (`SyncPacientesUseCaseDownloadGuardTest`) cover happy-path guard usage, conflict skip, tombstone phase-1, and upsert counts. **No test covers guard-query failure behavior.**

Main spec `openspec/specs/sync-conflict/spec.md` FR-01 currently mandates fail-open on guard query errors (scenario: "Network error during conflict query"). In practice `ConflictDao`/`SyncEntityStateDao` are **Room-local**; failures are typically SQLite/DB errors, not network I/O.

### Residual 2 — NuevoPacienteScreen save bypasses fecha validation

`PacienteFormSections` computes and displays `validateFechaNacimiento(fechaNacimiento)` errors (partial 1–7 digits, invalid month/day, invalid calendar dates like `31022020`).

`NuevoPacienteScreen.saveAction` gates save only on `nombreCompleto`, `edad`, and `telefono` — it never reads `validateFechaNacimiento`:

```89:102:optoapp/src/main/java/com/example/optoapp/ui/screens/NuevoPacienteScreen.kt
    val saveAction: () -> Unit = {
        if (!saving) {
            ...
            } else if (nombreCompleto.isNotBlank() && edad.isNotBlank() && telefono.isNotBlank()) {
                val p = Paciente(
                    ...
                    fechaNacimiento = fechaNacimiento.takeIf { it.isNotBlank() }?.let { DateUtils.fromDisplayFormat(DateUtils.formatDateInput(it)) },
```

Validation uses strict `LocalDate.of(y, m, d)`; persistence uses lenient `LocalDate.parse(value, displayFormatter)` in `DateUtils.fromDisplayFormat` (no `ResolverStyle.STRICT`). UI can show **"Fecha inválida"** while save still persists a **leniently adjusted** date (e.g. `31022020` → March 2, 2020). Partial input (1–7 digits) saves as `null` despite visible error — UX inconsistency, not data corruption, but still violates user expectation.

`PacienteViewModel.savePaciente` performs role/HO duplicate checks only; no fecha validation.

Unit tests exist for `validateFechaNacimiento` (`PacienteFormSectionsTest`); instrumented tests cover UI error display (`PacienteFlowTest`). **No test asserts save is blocked when validation fails.**

### Residual 3 — SessionManager daily delete key vs server UTC day

Local offline delete gate (`PacienteViewModel.deletePacienteGuarded`) uses `SessionManager.getPacienteDeleteCountToday` / `incrementPacienteDeleteCountToday`:

```169:171:optoapp/src/main/java/com/example/optoapp/data/SessionManager.kt
    private fun dailyPacienteDeleteKey(opticaId: String): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "paciente_delete_count_${opticaId}_$day"
```

`LocalDate.now()` uses the **device default timezone**, not UTC and not `DateUtils.userPreferredZone`.

Server enforcement uses UTC calendar day in both:
- Trigger `guard_pacientes_delete()` (`20260425013000_pacientes_delete_guardrails.sql`)
- RPC `paciente_eliminaciones_restantes_hoy` (restored in `20260911000000_fix_paciente_eliminaciones_restantes_hoy.sql`)

The Kotlin client does **not** call the RPC; local EncryptedSharedPreferences counter is the sole offline gate. Near UTC midnight (or any TZ offset from UTC), the client key can roll on a different calendar day than server audit — allowing extra local deletes or blocking prematurely relative to server trigger limits. Max drift ≈ timezone offset from UTC (e.g. up to ~5h for America/Lima at day boundaries).

`fix-pacientes-jd-quota-refresh` fixed quota consumption order and refresh timeout but explicitly left UTC alignment out of scope.

---

## Affected Areas

| File | Why |
|------|-----|
| `optoapp/src/main/java/com/example/optoapp/domain/SyncPacientesUseCase.kt` | Download guard fail-open on `getConflictEntityIds` / second `getPendingDeletions` |
| `optoapp/src/test/java/com/example/optoapp/domain/SyncPacientesUseCaseDownloadGuardTest.kt` | Add fail-closed / no-upsert-on-guard-failure tests |
| `optoapp/src/main/java/com/example/optoapp/ui/screens/NuevoPacienteScreen.kt` | `saveAction` must gate on fecha validation |
| `optoapp/src/main/java/com/example/optoapp/util/DateUtils.kt` | Optional: strict parse helper shared with validate |
| `optoapp/src/main/java/com/example/optoapp/ui/components/paciente/PacienteFormSections.kt` | `validateFechaNacimiento` (already tested; may export for save gate) |
| `optoapp/src/main/java/com/example/optoapp/data/SessionManager.kt` | `dailyPacienteDeleteKey` TZ alignment |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/PacienteViewModelTest.kt` | Delete quota tests may need UTC key fixture |
| `openspec/changes/fix-pacientes-jd-residuals/specs/` (propose/spec) | Delta for sync-conflict FR-01 pacientes exception; fecha save gate; UTC quota key |
| `openspec/specs/sync-conflict/spec.md` | FR-01 fail-open scenario conflicts with JD severe for pacientes |

**Out of scope (advice only):** Identical fail-open in `SyncHistorialUseCase`, `SyncInventarioUseCase`, `SyncProveedoresUseCase`, `SyncOrdenesComUseCase` — reuse pattern after pacientes proves stable.

---

## Approaches

### Residual 1 — Download guard failure

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. Fail-closed (abort upserts)** — On guard query failure, log error, skip all download upserts (return 0 or mark batch error); still allow Phase 1 pending-delete retry | Prevents clobbering conflicts/tombstones; matches JD severe intent | Contradicts current FR-01 fail-open spec; sync may show 0 downloads on transient DB glitch | Low |
| **B. Fail-safe skip-all** — Treat guard failure as "skip every remote ID" (fetch remote but upsert none) | Same safety as A without spec ambiguity on "abort" | Wastes network fetch; still downloads data unnecessarily | Low |
| **C. Keep fail-open (status quo)** | Matches existing FR-01 scenario | JD-confirmed severe: overwrites conflicted/deleted pacientes | None |
| **D. Retry guard query (1–2×) then fail-closed** | Handles transient Room lock errors | More complex; still needs fail-closed fallback | Medium |

### Residual 2 — Fecha nacimiento save gating

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. Block save in screen** — Call `validateFechaNacimiento(fechaNacimiento)` in `saveAction`; toast on error; disable save when invalid non-blank input | Minimal diff; aligns UI error with save | Logic duplicated at screen layer | Low |
| **B. Shared strict parse** — Add `DateUtils.parseBirthDateDigits(digits): LocalDate?` using same rules as `validateFechaNacimiento`; use in save + replace lenient `fromDisplayFormat` path | Single source of truth; fixes lenient-parse bypass | Slightly wider DateUtils change | Low–Med |
| **C. ViewModel validation** — `savePaciente` throws on invalid fecha | Centralized; testable without Compose | Screen must surface VM errors | Med |

### Residual 3 — UTC daily delete key

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. UTC in SessionManager** — `LocalDate.now(ZoneOffset.UTC)` in `dailyPacienteDeleteKey` | One-line fix; aligns with server trigger/RPC audit window | Users see quota reset at UTC midnight, not local midnight | Low |
| **B. Use DateUtils with fixed UTC** — Extract `DateUtils.utcToday()` | Consistent utility; reusable | SessionManager still needs import/dependency | Low |
| **C. Call server RPC before delete** — Authoritative quota | Perfect alignment | Requires network; breaks offline-first delete path | High |
| **D. User preferred TZ** — `DateUtils.today()` / `userPreferredZone` | Matches app date UX elsewhere | Still drifts from server UTC; does not fix JD finding | Low |

---

## Recommendation

Implement all three residuals in one bounded change (`fix-pacientes-jd-residuals`), separate from archived `fix-pacientes-jd-quota-refresh`:

1. **Sync guard (Residual 1): Approach A + optional single retry** — Fail-closed for pacientes download: if `getConflictEntityIds` or the Phase 2 `getPendingDeletions` query throws (rethrow `CancellationException`), log, mark sync error for download batch, return 0 upserts. Do **not** use `emptySet()`. Add spec **MODIFIED** delta for pacientes-only exception to FR-01 fail-open. Pattern reuse note for other modules in design, not in this PR.

2. **Fecha nacimiento (Residual 2): Approach B + A** — Block save when `fechaNacimiento.isNotBlank() && validateFechaNacimiento(fechaNacimiento) != null` (toast). Parse via strict shared helper (same logic as validate) instead of lenient `fromDisplayFormat`. Keep fecha optional when blank.

3. **UTC quota key (Residual 3): Approach A** — `LocalDate.now(ZoneOffset.UTC)` in `dailyPacienteDeleteKey`. Document that daily limit resets at UTC midnight to match `pacientes_delete_audit` and `guard_pacientes_delete`.

**PR size forecast:** ~80–150 authored lines across 4–6 Kotlin files + delta specs. **400-line budget risk: Low.**

---

## Risks

- **Spec conflict:** FR-01 fail-open scenario must be explicitly MODIFIED for pacientes; otherwise verify phase may flag inconsistency.
- **Fail-closed download:** Transient Room errors could block all paciente downloads until retry; acceptable trade-off vs data loss on conflicts.
- **UTC quota UX:** Users in UTC−5 may see quota reset at 7pm local; document in error strings or help if support tickets arise.
- **Lenient-parse removal:** Edge cases that previously saved adjusted dates will now be rejected — intended behavior.
- **No RPC client integration:** Local and server quotas can still diverge if user deletes on multiple devices; out of scope (existing architecture).

---

## Test Plan

### Residual 1
- [ ] `SyncPacientesUseCaseDownloadGuardTest`: when `getConflictEntityIds` throws `RuntimeException`, verify **no** `repository.upsertPaciente` calls and download count 0.
- [ ] Same when Phase 2 `getPendingDeletions` throws.
- [ ] `CancellationException` from guard queries still propagates (extend existing Phase 1 cancellation test pattern).
- [ ] Happy-path regression: conflicted ID still skipped (existing tests).

### Residual 2
- [ ] Unit test: strict parse rejects `31022020` (matches validate error).
- [ ] Screen or ViewModel test: save blocked when `fechaNacimiento = "3102202"` (partial) with toast/error path.
- [ ] Save succeeds with blank fecha and with valid `15061990`.
- [ ] Regression: `PacienteFormSectionsTest` unchanged/passing.

### Residual 3
- [ ] Unit test SessionManager (or PacienteViewModel delete test with mocked clock/key): key uses UTC date boundary — e.g. fixed instant where device local date ≠ UTC date, assert correct key.
- [ ] Regression: existing `deletePacienteGuarded` quota tests pass with UTC key mocking.

### Gates
- `./gradlew :optoapp:testDebugUnitTest --tests "*SyncPacientesUseCase*" --tests "*PacienteFormSections*" --tests "*PacienteViewModel*"`
- Full `testDebugUnitTest` before push.

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `fix-pacientes-jd-residuals` with three in-scope items above, explicit FR-01 delta for pacientes fail-closed, and reference to completed `fix-pacientes-jd-quota-refresh` as dependency context (no re-work on RPC/quota-order/refresh).
