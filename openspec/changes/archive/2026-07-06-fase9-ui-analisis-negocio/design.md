# Design: Fase 9 — UI/UX Análisis de Negocio

## Technical Approach

Replace the existing BI screen with "Mi Negocio" — two Compose screens driven by `AnalisisNegocioViewModel` that orchestrates three domain UseCases. Route `estadisticas_bi` is repurposed to serve the main screen; old `BIScreen.kt` and `BIViewModel.kt` are deleted. Brand colors (`#2C3E50`, `#27AE60`, `#E74C3C`) added as semantic tokens in OptoTokens.

## Architecture Decisions

### Navigation strategy

| Option | Tradeoff | Decision |
|--------|----------|----------|
| New route + keep old BI | Two related routes, dead code retained | — |
| **Replace route in-place** | Route `estadisticas_bi` → `AnalisisNegocioScreen`. Cleanest, BI code deleted | **Chosen** |
| Hidden route for old BI | Dead code, no benefit | — |

### Month navigation

| Option | Tradeoff | Decision |
|--------|----------|----------|
| **HorizontalPager** (foundation.pager) | First swipe gesture in app, need to test drawer edge conflict | **Chosen** — M3 native, snap support |
| Custom horizontalScroll + snap | More code, reinvents pager lifecycle | — |
| Button-only (prev/next) | No swipe, worse UX | — |

### State model

| Option | Tradeoff | Decision |
|--------|----------|----------|
| **Single flat data class** | Matches existing pattern (OperacionHoyUiState, BIUiState) | **Chosen** — consistency over sealed hierarchy |
| Sealed class (Loading/Data/Error) | Better type safety, but breaks from project convention | — |

### Screen components inline vs. extracted

| Component | Decision | Rationale |
|-----------|----------|-----------|
| `MonthSwitcher` | Inline in Screen | Only used here, ~30 lines |
| `ResumenCard` | Inline | Only used here |
| `RecomendacionCard` | Inline | Only used here |
| `ExpandableSection` | Inline in DetalleScreen | AnimatedVisibility + click header |
| `BarraIngresosEgresos` | Inline | Only used here |

## Data Flow

```
Drawer "Mi Negocio" → route: estadisticas_bi
  ↕
AnalisisNegocioScreen
  ↕ collectAsState()
  ↕
AnalisisNegocioViewModel
  ├── ObtenerAnalisisMensualUseCase(opticaId, mes)
  ├── ObtenerDeudoresUseCase(opticaId)
  └── GenerarRecomendacionesUseCase(opticaId, mes)
       │
  ┌────┴────┐
  │   init  │  → launch all 3 in coroutineScope { async { } }
  │ on month│  → cancel previous, relaunch
  └─────────┘
       │
       ▼
AnalisisNegocioUiState
  mesSeleccionado / analisis / deudores / recomendaciones / isLoading / error
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/screens/AnalisisNegocioScreen.kt` | **Create** | Main screen: MonthSwitcher, ResumenCard, 3 RecomendacionCards, "Ver análisis completo" |
| `ui/screens/AnalisisDetalleScreen.kt` | **Create** | Detail: 5 expandable sections (bars, ranking, deudores list, stock, proyección) |
| `viewmodel/AnalisisNegocioViewModel.kt` | **Create** | Orchestrates 3 UseCases, StateFlow<AnalisisNegocioUiState>, month change handler |
| `ui/theme/Color.kt` | **Modify** | Add `PositiveGreen(0xFF27AE60)`, `AlertRed(0xFFE74C3C)`, `TextDark(0xFF2C3E50)`, `WarningAmber(0xFFF39C12)` |
| `ui/theme/OptoTokens.kt` | **Modify** | Add semantic color object `analisis {}` with the 4 colors (light + dark variants) |
| `ui/screens/DrawerSections.kt` | **Modify** | Rename "Estadísticas (BI)" → "Mi Negocio", icon `TrendingUp`, route `estadisticas_bi` unchanged |
| `ui/screens/MainDrawerScreen.kt` | **Modify** | Replace `BIScreen(navController)` → `AnalisisNegocioScreen(navController)`; add `analisis_detalle` route |
| `ui/screens/BIScreen.kt` | **Delete** | Subsumed by new screens |
| `viewmodel/BIViewModel.kt` | **Delete** | Subsumed by new ViewModel |

## Interfaces / Contracts

```kotlin
data class AnalisisNegocioUiState(
    val mesSeleccionado: LocalDate,
    val analisis: AnalisisMensual? = null,
    val deudores: List<Deudor> = emptyList(),
    val recomendaciones: List<Recomendacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AnalisisNegocioViewModel @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val generarRecomendaciones: GenerarRecomendacionesUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {
    val uiState: StateFlow<AnalisisNegocioUiState>
    fun navigateMonth(delta: Int)  // +1 or -1
    fun refresh()
}
```

`navigateMonth(delta)` updates `mesSeleccionado` which triggers a `flatMapLatest` / `switchMap` — when month changes, cancel in-flight requests and relaunch. `init` loads current month.

## Testing Strategy

| Layer | What to test | Approach |
|-------|-------------|----------|
| ViewModel unit | `init` loads data, month change reloads, loading/error states | MockK 3 UseCases + SessionManager, `UnconfinedTestDispatcher`, `runTest`. Same pattern as `ReportesViewModelTest` |
| ViewModel unit | Offline detection — `analisis.esOffline=true` propagates to state | Mock use case returns `Resource.Success` with `esOffline=true` |
| Compose UI | State renders correct cards, error shows retry, loading shows indicator | `ComposeContentTestRule` with state injection, verify composable existence |
| Navigation | Drawer "Mi Negocio" navigates to correct route | `MainDrawerContentTest` — assert `selected` and route match |

## Migration / Rollout

No migration required. All changes are UI-only — no schema, RLS, or data. Rollback via git revert of `openspec/changes/fase9-ui-analisis-negocio/` files + restore BI files from git.

## Open Questions

- None. All decisions documented above.
