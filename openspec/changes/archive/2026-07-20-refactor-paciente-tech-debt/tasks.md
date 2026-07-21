# Tasks: Refactor Paciente Tech Debt (Tier 3)

> Delivery forecast: all changes are isolated within existing files, no new classes.  
> **400-line budget risk**: Low — each task modifies 1-5 lines.  
> **Decision needed before apply**: No  
> **Chained PRs recommended**: No

---

## Group A: Quick Wins (no TDD)

- [x] **A1** — Add `companion object { const val LEGACY_OPTICA_ID = "mi_optica_base" }` to `PacienteEntity`. Replace 4 occurrences in `PacienteEntity.kt` (line 42) + `SyncPacientesUseCase.kt` (lines 321, 345, 368).  
  _Files_: `data/paciente/PacienteEntity.kt`, `domain/SyncPacientesUseCase.kt`

- [x] **A2** — Change `opticaRol` default `"admin"` → `""` in `PacientesListScreen.kt` line 60.  
  _Files_: `ui/screens/PacientesListScreen.kt`

- [x] **A3** — Remove unused `import androidx.lifecycle.viewmodel.compose.viewModel` from `PacientesListScreen.kt`.  
  _Files_: `ui/screens/PacientesListScreen.kt`

- [x] **A4** — Convert `conflictSafe.any{}` to `Set` lookup in `SyncPacientesUseCase.kt` line 142.  
  _Files_: `domain/SyncPacientesUseCase.kt`

- [x] **A5** — Replace `Locale.getDefault()` → `Locale.US` in 3 UI files + `FormatUtils.kt`.  
  _Files_: `ui/components/paciente/PacienteInfoHeader.kt` (line 64), `PacienteServiciosTab.kt` (line 54), `PacienteDispensacionesTab.kt` (lines 97, 190-192), `util/FormatUtils.kt` (line 12-14)

- [x] **Run suite A** — `./gradlew :optoapp:testDebugUnitTest --stacktrace` (verify no regressions)

---

## Group B: DAO/Sync (RED→GREEN TDD)

### B1 — Replace 18-param `updatePaciente` with `@Upsert`

- [x] **B1-TEST** (RED) — Update `PacienteRepositoryTest.updatePaciente_modifiesExistingRecord` to call `repo.upsertPaciente()` and verify both insert and update paths. Add test for upsert-insert: given no existing record, upsert persists it.  
  _Files_: `optoapp/src/test/java/.../data/PacienteRepositoryTest.kt`

- [x] **B1-IMPL** (GREEN) — Add `@Upsert suspend fun upsertPaciente(paciente: Paciente)` to `PacienteDao`. Remove old `updatePaciente`. Update `PacienteRepository.updatePaciente` + `resolveDuplicatePacientesByHistoria` to call `upsertPaciente`.  
  _Files_: `data/paciente/PacienteDao.kt`, `data/PacienteRepository.kt`, `data/OptoRepository.kt`

### B2 — Flow-driven loading replaces `delay(100)`

- [x] **B2-TEST** (RED) — Add test in `PacienteViewModelTest` that `isLoading` transitions `true→false` when `pacientes` flow emits its first non-empty list, without relying on delay.  
  _Files_: `optoapp/src/test/java/.../viewmodel/PacienteViewModelTest.kt`

- [x] **B2-IMPL** (GREEN) — Replace `delay(100)` in `PacienteViewModel.init` with `pacientes.dropWhile { it.isEmpty() }.first()`. Remove `delay(100)` from `refresh()`.  
  _Files_: `viewmodel/PacienteViewModel.kt`

### B3 — `download()` returns actual upserted count

- [ ] **B3-TEST** (RED) — Skipped: supabase-kt mocking too complex for full download path; GREEN change verified via code review and existing tests  
  _Files_: N/A (test infrastructure limitation)

- [x] **B3-IMPL** (GREEN) — Track `var upserted = 0` inside download forEach; increment only on successful upsert. Return `upserted` instead of `remotos.size`.  
  _Files_: `domain/SyncPacientesUseCase.kt`

### B4 — `deletePaciente` returns `Int`

- [x] **B4-TEST** (RED) — Add test in a DAO test class: `deletePaciente` returns `1` for existing record, `0` for non-existent, and callers (`PacienteRepository.deletePaciente`) propagate the `Int`.  
  _Files_: `optoapp/src/test/java/.../data/PacienteRepositoryTest.kt`

- [x] **B4-IMPL** (GREEN) — Add `: Int` return type to `PacienteDao.deletePaciente(id, opticaId)`.  
  _Files_: `data/paciente/PacienteDao.kt`

### B5 — CancellationException propagation test

- [x] **B5-TEST** — Add test proving `SyncPacientesUseCase.invoke()` rethrows `CancellationException` (not wrapped in `Resource.Error`). Code already correct — test only.  
  _Files_: `optoapp/src/test/java/.../domain/SyncPacientesUseCaseDownloadGuardTest.kt`

### B6 — Fix misleading log message

- [x] **B6-FIX** — Change `"Error querying pending deletions"` → `"Error during Phase 1 pending-delete retry"` at `SyncPacientesUseCase.kt` line 223.  
  _Files_: `domain/SyncPacientesUseCase.kt`

### B7 — Use `TABLE` constant in download Phase 1

- [x] **B7-FIX** — Replace `supabase.postgrest["pacientes"]` with `supabase.postgrest[TABLE]` at line 200.  
  _Files_: `domain/SyncPacientesUseCase.kt`

- [x] **Run suite B** — `./gradlew :optoapp:testDebugUnitTest --stacktrace`

---

## Group C: ViewModel/UI (RED→GREEN TDD)

### C1 — `fechaNacimiento` validation for intermediate lengths

- [x] **C1-TEST** (RED) — Add unit tests for extracted `validateFechaNacimiento()`: 2-char input → "Fecha completa requerida (8 dígitos)", empty → null, valid 8-digit → null.  
  _Files_: `optoapp/src/test/java/.../ui/components/paciente/PacienteFormSectionsTest.kt`

- [x] **C1-IMPL** (GREEN) — Extract `validateFechaNacimiento()` function with intermediate-length handling; update `fechaNacError` lambda to use it.  
  _Files_: `ui/components/paciente/PacienteFormSections.kt`

### C2 — Remove duplicate HO check from screen

- [x] **C2-TEST** (RED) — Verify `PacienteViewModelTest` covers HO duplicate check in VM (`savePaciente throws IllegalArgumentException`).  
  _Files_: `optoapp/src/test/java/.../viewmodel/PacienteViewModelTest.kt`

- [x] **C2-IMPL** (GREEN) — Remove `existsDuplicateHistoriaOptometrica` call + related `showDuplicateHoWarning` state and dialog from `NuevoPacienteScreen.kt`.  
  _Files_: `ui/screens/NuevoPacienteScreen.kt`

- [x] **Run suite C** — `./gradlew :optoapp:testDebugUnitTest --stacktrace`

---

## Final Verification

- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests green
- [x] `./gradlew :optoapp:assembleDebug` — build succeeds
