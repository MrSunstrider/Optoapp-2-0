# Proposal: fix-pacientes-jd-residuals

## Intent

Close three Judgment Day pacientes residuals after Round 1 approval. Builds on completed `fix-pacientes-jd-quota-refresh` (RPC restore, quota consumption order, refresh timeout). Prevents sync clobbering conflict winners/tombstones, blocks invalid birth-date saves, and aligns offline delete quota with server UTC day.

## Scope

### In Scope
1. **Pacientes-only fail-closed download guards** — `SyncPacientesUseCase.download()`: if `getConflictEntityIds` or Phase 2 `getPendingDeletions` throws (except `CancellationException`), log, mark batch failure, return 0 upserts; never `emptySet()`.
2. **Fecha nacimiento save gate** — `NuevoPacienteScreen.saveAction` blocks when non-blank input fails `validateFechaNacimiento`; strict shared parse (not lenient `fromDisplayFormat`).
3. **UTC daily delete key** — `SessionManager.dailyPacienteDeleteKey` uses `LocalDate.now(ZoneOffset.UTC)` matching `guard_pacientes_delete` and `paciente_eliminaciones_restantes_hoy`.
4. Unit tests: guard failure, fecha save block, UTC key boundary.
5. Delta specs (see Capabilities).

### Out of Scope
- Fail-closed guards in Historial/Inventario/Proveedores/OrdenesCompra.
- Server RPC as offline delete gate.
- Supabase schema/RLS changes.

## Capabilities

### New Capabilities
- `paciente-fecha-save-gate`: Block save on invalid non-blank birth date; strict parse shared with UI validation.
- `paciente-delete-quota-utc`: Local delete counter keyed by UTC calendar day.

### Modified Capabilities
- `sync-conflict`: **FR-01 MODIFIED delta** — pacientes-only: guard query failure MUST fail-closed (0 upserts), not fail-open. Other entity types unchanged.

## Approach

| Residual | Approach |
|----------|----------|
| Download guard | Fail-closed; optional single retry |
| Fecha nacimiento | Screen gate + strict `DateUtils` helper; blank optional |
| Delete key | `LocalDate.now(ZoneOffset.UTC)` |

**Supabase:** No — client-only.

## Affected Areas

| Area | Impact |
|------|--------|
| `SyncPacientesUseCase.kt` | Fail-closed guards |
| `SyncPacientesUseCaseDownloadGuardTest.kt` | Guard-failure tests |
| `NuevoPacienteScreen.kt` | Fecha save gate |
| `DateUtils.kt` | Strict birth-date parse |
| `SessionManager.kt` | UTC delete key |
| `PacienteViewModelTest.kt` | UTC key fixture |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-01 conflict without delta | MODIFIED delta in spec phase |
| Room glitch blocks downloads | Accept vs data loss; retry next sync |
| UTC reset at non-local midnight | Document; matches server |

## Rollback Plan

1. Revert `SyncPacientesUseCase` fail-open catch blocks.
2. Revert fecha save gate and strict parse.
3. Revert `dailyPacienteDeleteKey` to device-local `LocalDate.now()`.
4. Remove new tests and delta specs.
5. No DB migration to undo.

## Dependencies

- `fix-pacientes-jd-quota-refresh` (done) — do not re-work RPC/quota-order/refresh.

## Success Criteria

- [ ] Guard failure → zero upserts; happy-path tests pass.
- [ ] Invalid/partial fecha blocks save; valid/blank saves.
- [ ] Delete key uses UTC at TZ boundary.
- [ ] `testDebugUnitTest` passes; FR-01 pacientes MODIFIED delta ready for verify.
