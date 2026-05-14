# Tasks: optoapp-pend — Android Cleanup Wave 2

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1200-1500 |
| 400-line budget risk | **High** |
| Chained PRs recommended | **Yes** |
| Suggested split | 5 stacked PRs (Phase 1 → 2 → 3 → 4 → 5) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: **No**
Chained PRs recommended: **Yes**
Chain strategy: stacked-to-main
400-line budget risk: High

---

## Phase 1: Critical Tests

- [ ] 1.1 RED: Write `PostSaveSyncSchedulerTest.kt` — debounce + session gating
- [ ] 1.2 GREEN: Implement minimal `SyncGate` interface + fake for testing
- [ ] 1.3 RED: Write `SubscriptionViewModelTest.kt` — tier, canAddPaciente flows
- [ ] 1.4 GREEN: Make VM testable with fake SubscriptionManager
- [ ] 1.5 RED: Write `SubscriptionManagerTest.kt` — tier resolution, dev override
- [ ] 1.6 GREEN: Add constructor injection for testability

## Phase 2: Pure Function Extractions

- [ ] 2.1 RED: Characterization test for `EvaluacionViewModel.parseDipOrDnp`
- [ ] 2.2 MOVE: Extract `parseDipOrDnp`, `formatDipForUi` → `dip/DipParser.kt`
- [ ] 2.3 GREEN: Delegate VM calls to DipParser, verify tests pass
- [ ] 2.4 RED: Characterization tests for refraction + diagnostico logic
- [ ] 2.5 MOVE: Extract `calcularDiagnostico`, helpers → `diagnostico/DiagnosticoCalculator.kt`
- [ ] 2.6 GREEN: Update VM imports, verify behavior unchanged

## Phase 3: File Splits (Data + Core)

- [ ] 3.1 RED: Snapshot `MembershipRepository` DTO behavior
- [ ] 3.2 MOVE: Extract DTOs → `MembershipRepositoryDtos.kt`, update imports
- [ ] 3.3 MOVE: Extract `RefraccionTableBuilder` → `RecetaRefraccionTable.kt`
- [ ] 3.4 MOVE: Extract uploaders → `SyncFinanzasUploaders.kt`
- [ ] 3.5 GREEN: Run all existing tests, verify no regression

## Phase 4: UI Component Splits

- [x] 4.1 MOVE: Extract `EvaluacionFormSections` → `evaluacion/AnamnesisSection.kt`, `evaluacion/ExamenVisualSection.kt`, `evaluacion/RefraccionSection.kt`, `evaluacion/ContactologiaSection.kt`, `evaluacion/CierreSection.kt`
- [x] 4.2 MOVE: Extract `MainDrawerScreen` drawer content → `components/MainDrawerContent.kt`
- [x] 4.3 MOVE: Extract `NuevaDispensacionScreen` → `dispensacion/LenteForm.kt`, `dispensacion/MonturaForm.kt`, `dispensacion/PagosSection.kt`
- [x] 4.4 MOVE: Extract `NuevoPacienteScreen` → `paciente/PacienteFormSections.kt`
- [x] 4.5 MOVE: Extract `NuevoServicioScreen` → `servicio/ServicioForm.kt`
- [x] 4.6 MOVE: Extract `MonturasScreen` → `monturas/MonturaList.kt`, `monturas/MonturaForm.kt`
- [x] 4.7 MOVE: Extract `PacienteEvaluacionesTab` → `paciente/EvaluacionListItem.kt`
- [x] 4.8 GREEN: Build + test — all UI renders correctly

## Phase 5: BOM + Cleanup

- [ ] 5.1 Update `libs.versions.toml` → `composeBom = "2024.12.01"`
- [ ] 5.2 Build — fix any compilation errors from BOM change
- [ ] 5.3 Complete `ConfiguracionScreen` modularization (delegate to existing sub-sections)
- [ ] 5.4 Run full test suite — all must pass
