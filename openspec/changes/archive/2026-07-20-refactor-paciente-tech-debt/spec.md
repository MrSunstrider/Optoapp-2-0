# Spec: Refactor Paciente Tech Debt (Tier 3)

Pure refactor — zero behavioral change. All items are code hygiene, API consistency, or correctness fixes within existing contract.

## Group A: Quick Wins (no tests)

| # | Requirement | Verification |
|---|------------|-------------|
| A1 | `PacienteEntity` companion MUST expose `const val LEGACY_OPTICA_ID = "mi_optica_base"`. All references in `PacienteEntity.kt` and `SyncPacientesUseCase.kt` SHALL use it. | `grep -c '"mi_optica_base"'` in those two files returns 0 after. |
| A2 | `opticaRol` initial value in PacientesListScreen SHALL be `""` (not `"admin"`). | Compose previews and manual launch: no spurious role-based UI before auth loads. |
| A3 | Unused `import androidx.lifecycle.viewmodel.compose.viewModel` SHALL be removed from PacientesListScreen.kt. | Compile succeeds. |
| A4 | `conflictSafe.any{}` O(n²) in SyncPacientesUseCase.kt SHALL become O(1) set lookup. Same filtering result. | Unit test: filtered list matches expected set. |
| A5 | Currency formatting in UI files SHALL use `Locale.US` to produce consistent `.` decimal separator regardless of device locale. | Manual: currency displays as `s/. 1,234.56` on es-PE device. |

## Group B: DAO/Sync (TDD required)

| # | Requirement | Scenario |
|---|------------|----------|
| B1 | `PacienteDao` SHALL expose `@Upsert suspend fun upsertPaciente(paciente: Paciente)` replacing the 18-param `updatePaciente`. `PacienteRepository.updatePaciente` SHALL delegate to `upsertPaciente`. | GIVEN an existing paciente, WHEN updated via `upsertPaciente`, THEN fields are persisted correctly. |
| B2 | `PacienteViewModel` init SHALL NOT use `delay(100)`. `_isLoading` SHALL be driven from the pacientes flow combine emitting its first value. | GIVEN VM init, WHEN pacientes flow emits, THEN `_isLoading` becomes false without artificial delay. |
| B3 | `download()` SHALL return the count of actually upserted pacientes (accounting for skipIds), not `remotos.size`. | GIVEN 5 remote pacientes with 2 skipped, WHEN download runs, THEN returns 3. |
| B4 | `deletePaciente(id, opticaId)` in PacienteDao SHALL return `Int` (rows deleted) instead of `Unit`. | GIVEN an existing record, WHEN deleted via this method, THEN returns 1. GIVEN non-existent, THEN returns 0. |
| B5 | `CancellationException` SHALL propagate correctly in download Phase 1 (no catch-and-swallow). | VERIFY existing catch blocks at lines 208-209 and 220-221 already rethrow. No code change if confirmed. |
| B6 | Download Phase 1 outer catch log SHALL read "Error during Phase 1 pending-delete retry" not "Error querying pending deletions". | Code review: string matches. |
| B7 | Download Phase 1 SHALL use `TABLE` constant instead of hardcoded `"pacientes"` string literal. | Code review: `supabase.postgrest[TABLE]` at line 200. |

## Group C: ViewModel/UI (TDD required)

| # | Requirement | Scenario |
|---|------------|----------|
| C1 | `fechaNacimiento` field SHALL show validation error for intermediate-length input (not just at exactly 8 chars). | GIVEN user types "12" (2 chars), WHEN field validates, THEN shows "Fecha completa requerida (8 dígitos)". GIVEN user types "3102202" (7 chars), THEN same. GIVEN "29022020" (valid), THEN no error. |
| C2 | HO duplicate check SHALL exist ONLY in ViewModel (`savePaciente`). The screen-side duplicate check in NuevoPacienteScreen SHALL be removed. | GIVEN save with duplicate HO, WHEN savePaciente runs, THEN VM throws `IllegalArgumentException`. The screen SHALL NOT independently call `existsDuplicateHistoriaOptometrica`. |
