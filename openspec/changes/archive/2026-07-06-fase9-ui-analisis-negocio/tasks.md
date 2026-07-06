# Tasks: Fase 9 — UI/UX Análisis de Negocio

## Phase 1: ViewModel + State (TDD)

- [x] ### Task 1: AnalisisNegocioUiState + AnalisisNegocioViewModelTest ✅

**Test first** (RED → GREEN):
- Create `AnalisisNegocioViewModelTest.kt` with MockK for 3 UseCases + SessionManager
- Tests:
  - `init loads data from all 3 use cases` — assert state fields populated
  - `navigateMonth(+1) updates mesSeleccionado and reloads` — assert new month
  - `navigateMonth(-1) updates mesSeleccionado and reloads`
  - `isLoading is true during initial load`
  - `error state when use case fails` — assert error message
  - `offline detection — analisis.esOffline=true propagates to state`
- Then implement `AnalisisNegocioViewModel.kt`:
  - `@HiltViewModel class AnalisisNegocioViewModel @Inject constructor(...) : ViewModel()`
  - Injects: `ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`, `GenerarRecomendacionesUseCase`, `SessionManager`
  - State: `StateFlow<AnalisisNegocioUiState>` with flat data class
  - `init` loads current month via `DateUtils.today()`
  - `navigateMonth(delta: Int)` — updates month, relaunches all 3 UseCases
  - Graceful error handling per UseCase

## Phase 2: AnalisisNegocioScreen Composable (TDD)

- [x] ### Task 2: AnalisisNegocioScreenTest + AnalisisNegocioScreen ✅

**Test first** (RED → GREEN):
- Create `AnalisisNegocioScreenTest.kt` with Compose test rule
- Tests:
  - `renders summary card with ventas, cobros, saldo, margen`
  - `shows loading indicator initially`
  - `shows error card with retry button`
  - `shows month selector with current month`
  - `shows top 3 recommendations with priority colors`
  - `renders offline banner when esOffline=true`
- Then implement `AnalisisNegocioScreen.kt`:
  - Scaffold + OptoTopAppBar "Mi Negocio"
  - Role gating via `AppRoles.canViewBiAndReports()`
  - State collection: `val uiState by viewModel.uiState.collectAsState()`
  - MonthSwitcher: inline prev/next buttons + month label
  - ResumenCard: vendiste / cobraste / saldo / margen
  - RecomendacionCard × 3: priority-colored (ALTA=AlertRed, MEDIA=WarningAmber, BAJA=neutral)
  - "Ver análisis completo" button → navigates to `analisis_detalle`
  - Loading: LinearProgressIndicator
  - Error: card + retry button
  - Offline: banner "Datos limitados — sin conexión"

## Phase 3: AnalisisDetalleScreen Composable (TDD)

- [x] ### Task 3: AnalisisDetalleScreenTest + AnalisisDetalleScreen ✅

**Test first** (RED → GREEN):
- Create `AnalisisDetalleScreenTest.kt` with Compose test rule
- Tests:
  - `renders all 5 expandable sections`
  - `first section is expanded by default`
  - `clicking a section header toggles visibility`
  - `renders deudores list when data present`
  - `renders stock estancado items`
  - `shows proyeccion caja card`
- Then implement `AnalisisDetalleScreen.kt`:
  - Scaffold + OptoTopAppBar "Análisis Completo" with back nav
  - 5 expandable sections with AnimatedVisibility:
    1. "Plata que entró y salió" (default expanded) — BarraIngresosEgresos
    2. "Lo que más te deja" — ranking por categoría
    3. "Clientes que te deben" — deudores list (nombre, teléfono, saldo)
    4. "Productos sin vender" — stock estancado list
    5. "Plata que vas a tener" — proyección caja card

## Phase 4: Navigation + Drawer + Cleanup

- [x] ### Task 4: Update DrawerSections.kt ✅

- Rename "Estadísticas (BI)" → "Mi Negocio"
- Icon: `Icons.Default.TrendingUp` (was `BarChart`)
- Route stays `estadisticas_bi`
- Text fontWeight remains SemiBold

- [x] ### Task 5: Update MainDrawerScreen.kt ✅

- Replace `BIScreen(navController)` → `AnalisisNegocioScreen(navController)` for route `estadisticas_bi`
- Add new route `analisis_detalle` → `AnalisisDetalleScreen(navController)` (pass navController)

- [x] ### Task 6: Delete old BI files ✅

- Delete `ui/screens/BIScreen.kt`
- Delete `viewmodel/BIViewModel.kt`
- Delete `viewmodel/BIViewModelTest.kt`

## Phase 5: Theme + Existing Tests

- [x] ### Task 7: Update theme colors ✅

- In `ui/theme/Color.kt`: add `PositiveGreen(0xFF27AE60)`, `AlertRed(0xFFE74C3C)`, `TextDark(0xFF2C3E50)`, `WarningAmber(0xFFF39C12)`
- In `ui/theme/OptoTokens.kt`: add semantic `analisis {}` object with same 4 colors

- [x] ### Task 8: Update MainDrawerContentTest.kt + compilation guard ✅

- Update `conditionalItems_finanzas_whenShowCierreCajaOrShowBiYReportes` — "Estadísticas (BI)" → "Mi Negocio"
- Run `./gradlew :optoapp:testDebugUnitTest :optoapp:assembleDebug --no-configuration-cache` — verify BUILD SUCCESSFUL

---

## Task Dependencies

```
Task 1 (ViewModel) ──→ Task 2 (Main Screen) ──→ Task 3 (Detail Screen)
                                                    │
Task 4 (Drawer) ──→ Task 5 (NavHost) ──→ Task 6 (Delete BI)
                                                    │
Task 7 (Theme) ──→ Task 8 (Verify + Tests) ←───────┘
```

Tasks 1, 4, 7 can start in parallel. Tasks 2-3 depend on 1. Task 5 depends on 4. Task 6 depends on 5. Task 8 depends on all.
