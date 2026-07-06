## Exploration: Análisis de Negocio UI (Fase 9)

### Current State

**Navigation structure**: `MainDrawerScreen.kt` is the root shell. It owns a `ModalNavigationDrawer` + inner `NavHost` with ~20 composable routes. Routes are plain strings (no sealed class). The `DrawerContent` composable lives in `DrawerSections.kt`, organized in GESTIÓN → PROGRAMACIÓN → FINANZAS → SISTEMA sections. Navigation uses `navController.navigateDrawer(route)` — a helper extension that applies `popUpTo(startDestination)`, `launchSingleTop`, and `restoreState`.

**Existing BI Screen**: `BIScreen.kt` + `BIViewModel.kt` live at:
- `ui/screens/BIScreen.kt` — stats dashboard with period selector, KPI cards, donut chart, bar chart, top-5 ranking
- `viewmodel/BIViewModel.kt` — uses `OptoRepository`, `SessionManager`, `VentaDao`; state pattern is `StateFlow<BIUiState>` data class

**Drawer entry**: Under "FINANZAS" section, gated by `showBiYReportes && canViewBiAndReports("admin", "gerente")`. Label: "Estadísticas (BI)", icon: BarChart, route: `estadisticas_bi`.

**Compose patterns confirmed**:
- ViewModel injection: `viewModel: SomeViewModel = hiltViewModel()` default param
- State collection: `val uiState by viewModel.uiState.collectAsState()`
- Role gating: early return with "Acceso restringido" card
- Scaffold + OptoTopAppBar + scrollable Column
- No sealed UI state classes — flat data class per screen

**Theme**: Material 3 via `OptoTokens`. Primary `#006D6F` (deep teal), tertiary `#B35B5B` (warm red). Original brand colors (`#2C3E50`, `#27AE60`, `#E74C3C`) from the plan are NOT present in the theme — they need to be added as semantic colors for this module.

**UseCases available** (Fases 7-8):
- `ObtenerAnalisisMensualUseCase` — RPC `rpc_analisis_mensual` → `AnalisisMensual` domain model
- `ObtenerDeudoresUseCase` — RPC `rpc_deudores` → `List<Deudor>`
- `GenerarRecomendacionesUseCase` — composes above two + config → `List<Recomendacion>`

**Domain models**:
- `AnalisisMensual`: ventasMes, cobrosMes, margenNetoPct, margenPorCategoria[], deudores, proyeccionCaja, stockEstancado[], valorInventario, ventasMesAnterior, variacionVentasPct, gastosMes, esOffline
- `Deudor`: pacienteNombre, pacienteTelefono, ventaId, saldo, diasDeuda
- `Recomendacion`: id, tipo, titulo, detalle, impactoEstimado, prioridad(ALTA/MEDIA/BAJA), accion, datosAccion
- `RecomendacionTipo`: COBRAR, ALERTA_CAIDA, MEJORAR_PRECIO, LIQUIDAR_STOCK, VENDER_MAS_DE, REDUCIR_GASTO

### Affected Areas

- `ui/screens/MainDrawerScreen.kt` — add route `analisis_negocio` and `analisis_detalle`
- `ui/screens/DrawerSections.kt` — replace/rename "Estadísticas (BI)" entry to "Mi Negocio"
- `ui/screens/AnalisisNegocioScreen.kt` — NEW: main "Tu negocio en 30 segundos" screen
- `ui/screens/AnalisisDetalleScreen.kt` — NEW: detail screen with expandable sections
- `viewmodel/AnalisisNegocioViewModel.kt` — NEW: orchestrates UseCases for main+detail
- `ui/theme/Color.kt` — add semantic colors (positive green, alert red, dark text)
- `ui/theme/OptoTokens.kt` — add brand accent tokens if not using M3 dynamic
- `data/AppRoles` — add `canViewAnalisisNegocio()` or reuse `canViewBiAndReports`
- Test: `MainDrawerContentTest.kt` — update route assertion

### Approaches

1. **Replace BI screen entirely** — route `estadisticas_bi` → `AnalisisNegocioScreen`. Simplest, but loses existing BI functionality.
   - Pros: Minimal navigation changes, no migration
   - Cons: Destroys existing stats screen; plan says to "augment" not delete
   - Effort: Low

2. **Add new route + rename drawer entry** — new route `analisis_negocio`, rename "Estadísticas (BI)" → "Mi Negocio", keep existing BI route alongside.
   - Pros: Backward compatible, clean separation
   - Cons: Two related screens in different routes
   - Effort: Medium

3. **Replace BI entry + keep old route for reference** — rename drawer to "Mi Negocio", route `estadisticas_bi` now serves `AnalisisNegocioScreen`. Old `BIScreen` either deleted or kept on a hidden route.
   - Pros: Clean user-facing change, aligns with plan ("reemplazar la actual de BI")
   - Cons: Irreversible if someone depends on old BI
   - Effort: Medium

### Recommendation

**Approach 3** — The plan explicitly says: "La sección del drawer 'estadisticas_bi' —ya existente— se renombra a 'Mi Negocio' o se crea una nueva entrada 'analisis' que reemplace a la actual de BI." Replace the drawer entry with "Mi Negocio" pointing to route `analisis_negocio`. Update the NavHost composable for `estadisticas_bi` to call `AnalisisNegocioScreen`. Delete the old `BIScreen` and `BIViewModel` since the new screens subsume their functionality.

### UI Flow

```
Drawer "Mi Negocio" → AnalisisNegocioScreen
  ├── Summary card (ventas, cobros, saldo, margen)
  ├── Top 3 recomendaciones (swipeable action cards)
  ├── Month selector (horizontal swipe)
  └── "Ver análisis completo" → AnalisisDetalleScreen
        ├── ▼ Plata que entró y salió (expanded by default)
        ├── ▶ Lo que más te deja
        ├── ▶ Clientes que te deben
        ├── ▶ Productos sin vender
        └── ▶ Plata que vas a tener
```

### Risks

- The brand colors (`#2C3E50`, `#27AE60`, `#E74C3C`) don't exist in OptoTokens — need addition
- `GenerarRecomendacionesUseCase` calls Supabase RPC and Room — ensure offline fallback renders correctly
- Month selector with swipe is a new interaction pattern in this app — no existing horizontal pager usage
- TDD mode enabled — tests must be written before any implementation

### Ready for Proposal

Yes — all codebase patterns identified, domain models and UseCases confirmed, approach decided.
