# Proposal: Refactor Paciente — Tech Debt Cleanup (Tier 3)

## Intent

Remediate 14 non-critical tech-debt items from the 3-round Judgment Day review of the Pacientes module. No behavioral changes — only code hygiene, API consistency, correctness of logging/error handling, and removal of brittle patterns.

## Scope

### In Scope

| # | Item | Type | Test needed |
|---|------|------|-------------|
| 1 | Extract `LEGACY_OPTICA_ID` constant from `"mi_optica_base"` (5 occurrences) | Quick win | No |
| 2 | Change `opticaRol` default `"admin"` → `""` in PacientesListScreen | Quick win | No |
| 3 | Remove unused `viewModel` import in PacientesListScreen | Quick win | No |
| 4 | Convert `conflictSafe.any` O(n²) → `Set` O(1) in SyncPacientesUseCase | Quick win | No |
| 5 | Use `Locale.US` in `formatAsCurrency()` to avoid locale-sensitive decimal | Quick win | No |
| 6 | Replace 18-param `updatePaciente` with `@Upsert` in PacienteDao | Refactor | Yes |
| 7 | Replace `delay(100)` timing hack with flow-driven loading in PacienteViewModel init | Refactor | Yes |
| 8 | Return actual upserted count (not `remotos.size`) in `download()` | Refactor | Yes |
| 9 | Fix `deletePaciente` return type from `Unit` to `Int` for API consistency | Refactor | Yes |
| 10 | Rethrow `CancellationException` in `refresh()` catch block (PacienteViewModel) | Refactor | Yes |
| 11 | Fix misleading log message in `download()` outer catch (SyncPacientesUseCase) | Refactor | Yes |
| 12 | Use `TABLE` constant instead of `"pacientes"` literal in download Phase 1 | Refactor | Yes |
| 13 | Show `fechaNacimiento` validation error for intermediate-length input | Refactor | Yes |
| 14 | Remove duplicate HO check from NuevoPacienteScreen (already in ViewModel) | Refactor | Yes |

### Out of Scope
- Items 15–22 (JSON array for etiquetas, Resource.Empty, firstOrNull, SQL MAX, layout slots, etiquetas UI, sort order, spinner error state) — deferred to future change

## Capabilities

### New Capabilities
None — pure refactor, no new specs.

### Modified Capabilities
None — no spec-level behavior changes.

## Approach

Apply in 3 groups to keep commits reviewable:

1. **Quick wins** (items 1–5): Trivial one-liners, no tests. Commit per item.
2. **DAO + sync correctness** (items 6, 8, 9, 11, 12): Replace `updatePaciente` with `@Upsert`, fix return types, constants, and log messages. Tests for each.
3. **ViewModel + UI hygiene** (items 7, 10, 13, 14): Flow-driven loading, CancellationException rethrow, validation, remove duplicate guard. Tests for each.

Each group independently revertible. TDD applies to groups 2 and 3 (test first).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `PacienteEntity.kt` | Modified | Add `LEGACY_OPTICA_ID = "mi_optica_base"` companion |
| `PacienteDao.kt` | Modified | Replace `updatePaciente` with `@Upsert`; fix `deletePaciente` return |
| `PacienteViewModel.kt` | Modified | Flow-driven init, rethrow in refresh |
| `PacientesListScreen.kt` | Modified | Fix default rol, remove unused import |
| `SyncPacientesUseCase.kt` | Modified | Set lookup, fix count, fix log, use TABLE constant |
| `PacienteFormSections.kt` | Modified | Enhanced validation for intermediate input |
| `NuevoPacienteScreen.kt` | Modified | Remove redundant HO check |
| `FormatUtils.kt` | Modified | Add `Locale.US` to `formatAsCurrency()` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `@Upsert` changes update behavior vs explicit UPDATE | Low | Verify all callers; existing Room tests cover upsert |
| `Locale.US` changes displayed format on es-PE devices | Low | Only affects decimal separator (`.` vs `,`); desired result |

## Rollback Plan

Each item is an isolated commit. Revert individual commits or git-revert the full branch. No migration needed — no schema changes.

## Dependencies

None.

## Success Criteria

- [ ] All 14 items applied
- [ ] Items 1–5 require no tests (trivial one-liners)
- [ ] Items 6–14 have passing unit tests (TDD: test before implementation)
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` green
- [ ] No behavioral regression in existing Paciente CRUD flows
