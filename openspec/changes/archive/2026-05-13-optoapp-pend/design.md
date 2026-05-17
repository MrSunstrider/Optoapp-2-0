# Design: optoapp-pend — Android Cleanup Wave 2

## Technical Approach

Pure refactor: extract large files, add missing tests, bump Compose BOM — zero behavior changes. Per-proposal priority: (1) critical tests, (2) high splits, (3) BOM update. Every extraction preserves EXACT behavior via characterization tests where unit tests don't exist.

## Architecture Decisions

### 1. Test Strategy for Untested Components

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Inject mockito/powermock | Works, but project has ZERO mocking deps — would change build config | **Rejected** — too invasive |
| Pure Kotlin fakes + contract tests | No infra change, follows existing pattern (see `EvaluacionViewModelTest` which tests method sigs only) | **Accepted** for SubscriptionVM, SubscriptionManager |
| Robolectric + in-memory Room | Already in libs (room-testing, robolectric available). Tests DB-backed logic | **Accepted** for PostSaveSyncScheduler (touches SyncGate/Supabase) |
| Characterisation tests with real Supabase | Flaky, requires network | **Rejected** |

**Chosen**: Write contract tests for `SubscriptionViewModel` (tier flow, canAddPaciente) and `SubscriptionManager` (tier resolution, dev override) using Kotlin fakes + `kotlinx-coroutines-test`. For `PostSaveSyncScheduler`, use an interface delegate to `SyncGate` + `FakeCoroutineScope` so debounce/session-gating is testable without Supabase.

### 2. EvaluacionViewModel Split

| Component | Current Lines | Extract Into | Pattern |
|-----------|--------------|--------------|---------|
| EvaluacionUiState | ~142 lines `(data class)` | **Stay in file** | Keep co-located with VM |
| `parseDipOrDnp`, `formatDipForUi` | 40 lines | `dip/DipParser.kt` as `internal object` | Pure functions, no dependencies |
| `parseRefraction`, `calcularDiagnostico`, `parseSnellenToLogMar`, `updateDiagnosticAuto`, `updateOtrosAuto`, `normalizeAndTranspose` | ~150 lines | `diagnostico/DiagnosticoCalculator.kt` as `internal object` | Pure functions, stateless |
| Left in VM | ~265 lines | Remaining: state fields, save/load/delete | ViewModel only |

EvaluacionUiState stays in the file because (a) it IS the VM contract, (b) moving it changes import paths for no net gain. Extracting the pure functions into stateless objects gives testable units without Hilt.

### 3. EvaluacionFormSections Split

| New File | Contents |
|----------|----------|
| `evaluacion/AnamnesisSection.kt` | `AnamnesisSection` composable |
| `evaluacion/ExamenVisualSection.kt` | `ExamenVisualSection` + private `VisionBinocularCard`, `ColorPerceptionCard`, `SaludOcularCard`, `OtrasPruebasCard` |
| `evaluacion/RefraccionSection.kt` | `RefraccionSection` + private `AddSection`, `DipSection`, `PrismasSection` |
| `evaluacion/ContactologiaSection.kt` | `ContactologiaSection` + private `QueratometriaCard` |
| `evaluacion/CierreSection.kt` | `CierreSection` + private `DiagnosticoCard`, `TratamientoCard`, `CitaCard` |

Each file = one top-level `@Composable` + its private helpers. Import `EvaluacionUiState` from the viewmodel package. No behavior change.

### 4. Large File Extraction Strategy (Pattern for All Splits)

```
1. RED: write a characterization test that snapshots current behavior
2. MOVE-ONLY: extract code to new file, keep original imports intact
3. GREEN: run characterization test → same behavior
4. REFACTOR: clean internal visibility (internal / private)

For files w/o existing tests: start with a contract test for public API.
For files w/ existing tests: add characterization before moving code.
```

### 5. RecetaPdfBuilder Refactoring

Extract `RxGridRow` sealed class + all `addRefraccion` grid logic (~180 lines) into `RecetaRefraccionTable.kt` as `internal class RefraccionTableBuilder`. The main builder keeps `addRefraccion` that delegates to the table builder. Keep remaining section methods (`addDiagnostico`, `addPrismas`, etc.) in place — they're small (<30 lines each).

### 6. Compose BOM Update

| From | To | Risk |
|------|----|------|
| `2024.02.02` | `2024.12.01` | Medium — Material3 API changes between Feb and Dec 2024 |

Bump only the version in `libs.versions.toml`, then run full build. If compilation fails, diagnose and fix per-error. Do not refactor any API calls as part of the bump — if something breaks, it gets its own commit + test.

## File Changes

### Created

| File | Contents |
|------|----------|
| `domain/sync/PostSaveSyncSchedulerTest.kt` | Contract + characterization tests |
| `viewmodel/SubscriptionViewModelTest.kt` | Contract tests for tier, canAddPaciente, launchProPurchase |
| `subscription/SubscriptionManagerTest.kt` | Contract tests for tier resolution, dev override |
| `evaluacion/dip/DipParser.kt` | Extracted `parseDipOrDnp`, `formatDipForUi` |
| `evaluacion/diagnostico/DiagnosticoCalculator.kt` | Extracted `parseRefraction`, `calcularDiagnostico`, `parseSnellenToLogMar`, `updateDiagnosticAuto`, `updateOtrosAuto`, `normalizeAndTranspose` |
| `ui/components/evaluacion/AnamnesisSection.kt` | Extracted from EvaluacionFormSections |
| `ui/components/evaluacion/ExamenVisualSection.kt` | Extracted |
| `ui/components/evaluacion/RefraccionSection.kt` | Extracted |
| `ui/components/evaluacion/ContactologiaSection.kt` | Extracted |
| `ui/components/evaluacion/CierreSection.kt` | Extracted |
| `MembershipRepositoryDtos.kt` (in data package) | Extracted DTOs (OpticaDto, UsuarioOpticaDto, PlanSettings, etc.) |
| `util/RecetaRefraccionTable.kt` | Extracted `RefraccionTableBuilder` + `RxGridRow` |
| `ui/screens/MainDrawerContent.kt` | Extracted drawer content composable from MainDrawerScreen |
| `ui/components/dispensacion/LenteForm.kt` | Extracted from NuevaDispensacionScreen |
| `ui/components/dispensacion/MonturaForm.kt` | Extracted |
| `ui/components/dispensacion/PagosSection.kt` | Extracted |
| `domain/SyncFinanzasUploaders.kt` | Extracted uploadDispensaciones, uploadServicios, uploadPagos |
| `ui/components/paciente/PacienteFormSections.kt` | Extracted from NuevoPacienteScreen |
| `ui/components/servicio/ServicioForm.kt` | Extracted from NuevoServicioScreen |
| `ui/components/monturas/MonturaList.kt` | Extracted from MonturasScreen |
| `ui/components/monturas/MonturaForm.kt` | Extracted |
| `ui/components/paciente/EvaluacionListItem.kt` | Extracted from PacienteEvaluacionesTab |

### Modified

| File | Change |
|------|--------|
| `viewmodel/EvaluacionViewModel.kt` | Remove ~190 lines of pure functions, delegate to new extracted objects |
| `ui/components/EvaluacionFormSections.kt` | Remove 5 section composables, import from new files |
| `data/MembershipRepository.kt` | Remove all DTO data/classes (~130 lines), import from new file |
| `ui/screens/MainDrawerScreen.kt` | Replace inline drawer content with `MainDrawerContent()` call |
| `util/RecetaPdfBuilder.kt` | Delegate `addRefraccion` to new `RefraccionTableBuilder` |
| `ui/screens/NuevaDispensacionScreen.kt` | Replace LenteCard/MonturaCard/PagosSection with component calls |
| `domain/SyncFinanzasUseCase.kt` | Delegate upload methods to extracted file |
| `ui/screens/NuevoPacienteScreen.kt` | Replace inline form sections |
| `ui/screens/NuevoServicioScreen.kt` | Replace inline form |
| `ui/screens/MonturasScreen.kt` | Replace inline list/form |
| `ui/screens/PacienteEvaluacionesTab.kt` | Replace inline item |
| `ui/screens/ConfiguracionScreen.kt` | Complete modularization (delegate to existing sub-sections) |
| `gradle/libs.versions.toml` | `composeBom` → `"2024.12.01"` |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| **Unit** (new) | `DipParser`, `DiagnosticoCalculator` | Pure function tests — no mocking needed, JUnit 4 |
| **Unit** (new) | `SubscriptionManager` tier/planCode flows | Kotlin fakes for MembershipRepository + DataStore, coroutines-test |
| **Unit** (new) | `PostSaveSyncScheduler` debounce + session gating | Extract `SyncGate` to interface, FakeCoroutineScope, verify ordering |
| **Unit** (new) | `SubscriptionViewModel` tier/canAddPaciente | Fake SubscriptionManager + OptoRepository, turbines for Flow assertions |
| **Characterization** | Before each extract: snapshot current behavior via test | Read output, move code, re-run → same result |
| **Integration** | N/A — pure refactor, no new integration surfaces | |
| **E2E** | N/A | |

## Migration / Rollout

No migration required. Per-item atomic commits with `git revert <hash>` rollback. Compose BUM bump isolated in its own commit for easy rollback.

## Open Questions

- [ ] `SyncGate` is a `Mutex` wrapper with package-private visibility — should we extract it to an interface for PostSaveSyncScheduler testing? (Yes, minimal change, enables clean tests)
- [ ] Confirm `ConfiguracionScreen` remaining modularization scope — explore says "already partially modularized". Review what's left inline.
