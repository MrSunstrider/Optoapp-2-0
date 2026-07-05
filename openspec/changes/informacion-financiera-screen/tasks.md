# Tasks: InformacionFinancieraScreen

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

| Field | Value |
|-------|-------|
| Estimated changed lines | ~750-850 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Repository+DI → PR 2: ViewModel → PR 3: Screen+Nav+Refactor |
| Delivery strategy | ask-on-risk |

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Repository interface+impl + DI binding + repository tests | PR 1 | Foundation, base for everything else |
| 2 | ViewModel + ViewModel tests | PR 2 | Depends on PR 1 |
| 3 | Screen UI + Nav route + NuevaDispensacion refactor | PR 3 | Depends on PR 2 |

## Phase 1: Foundation — Repository + DI (PR 1)

- [x] 1.1 RED: Write `DispensacionFinancieraRepositoryTest` — mock `OptoRepository` + `VentaDao`, test delegation for all 7 methods
- [x] 1.2 GREEN: Create `data/DispensacionFinancieraRepository.kt` — interface + `DispensacionFinancieraRepositoryImpl` delegating to `OptoRepository` + `VentaDao`
- [x] 1.3: Add `@Provides @Singleton` binding in `di/DatabaseModule.kt` for `DispensacionFinancieraRepository` → `Impl`
- [x] 1.4: Verify `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes

## Phase 2: ViewModel (PR 2)

- [x] 2.1 RED: Write `InformacionFinancieraViewModelTest` — test `init` loads context+pagos, `save()` upserts Venta + schedules sync, pagos CRUD, estado toggle, saldo reactivity
- [x] 2.2 GREEN: Create `viewmodel/InformacionFinancieraViewModel.kt` — `@HiltViewModel` with `FinancieraUiState`, `save()` flow per design sequence
- [x] 2.3: Verify `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes

## Phase 3: Screen — UI + Navigation + Refactor (PR 3)

- [x] 3.1 GREEN: Create `ui/screens/InformacionFinancieraScreen.kt` — sticky header, `OptoTextField` monto total, pagos list with `AbonoDialog`, saldo reactivo, `DropdownField` estado, Guardar button
- [x] 3.2: Add `composable("informacion_financiera/{dispensacionId}")` route with null guard in `MainDrawerScreen.kt`
- [x] 3.3: Replace `FinancieraInfoSection` call in `NuevaDispensacionScreen.kt` with summary Card + "Gestionar Pagos" button → navigate to new route
- [x] 3.4: Deprecate `FinancieraInfoSection` in `DispensacionFormSections.kt`
- [x] 3.5: Verify `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes

## Phase 4: Final Verification

- [x] 4.1: Full test suite pass — `./gradlew :optoapp:testDebugUnitTest --stacktrace`
