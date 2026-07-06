# Fase 9 — UI/UX Análisis de Negocio

## Intent

Construir la interfaz de "Mi Negocio" (reemplazo de las estadísticas BI actuales) que permita al dueño/gerente entender el estado financiero de su óptica en 30 segundos, con recomendaciones accionables, y acceso a detalle expandible. Se apoya en los UseCases de Fases 7-8 (`ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`, `GenerarRecomendacionesUseCase`) y los modelos de dominio (`AnalisisMensual`, `Recomendacion`, `Deudor`).

## Scope

### IS
- `AnalisisNegocioScreen` — pantalla principal "Tu negocio en 30 segundos"
  - Selector de mes con swipe horizontal (HorizontalPager)
  - Card de resumen: vendiste / cobraste / saldo / margen
  - Top 3 recomendaciones con prioridad (ALTA=rojo, MEDIA=amarillo)
  - Botón "Ver análisis completo"
- `AnalisisDetalleScreen` — pantalla secundaria con secciones expandibles
  - "Plata que entró y salió" (barras horizontales, expandida default)
  - "Lo que más te deja" (ranking por categoría con margen)
  - "Clientes que te deben" (lista de deudores con teléfono)
  - "Productos sin vender" (stock estancado)
  - "Plata que vas a tener" (proyección 30 días)
- `AnalisisNegocioViewModel` — orquesta los 3 UseCases, expone `StateFlow<AnalisisNegocioUiState>`
- Refactor de navegación:
  - Drawer entry "Estadísticas (BI)" → "Mi Negocio" (icono: `Icons.Default.TrendingUp`)
  - Ruta `estadisticas_bi` ahora sirve `AnalisisNegocioScreen`
  - Ruta `analisis_detalle` para la pantalla secundaria
  - Eliminar `BIScreen.kt` y `BIViewModel.kt` (funcionalidad subsumida)
- Agregar colores semánticos a OptoTokens:
  - `PositiveGreen = Color(0xFF27AE60)` — ganancia, positivo
  - `AlertRed = Color(0xFFE74C3C)` — deuda, alerta
  - `TextDark = Color(0xFF2C3E50)` — texto principal en cards
  - `WarningAmber = Color(0xFFF39C12)` — prioridad media
- Role gating: `analisis_negocio` visible solo para `canViewBiAndReports()` (admin/gerente)

### IS NOT
- No incluye widget de launcher (mencionado como fase futura deseable en 9.3)
- No incluye Fase 10 (QA, testing integral, criteria de aceptación final)
- No incluye responsive design (toda la app ya es mobile-first)
- No modifica el backend (Fases 6-8 ya completaron RPCs, Room, UseCases)
- No toca la pantalla de Reportes existente

## Approach

### Architecture

```
AnalisisNegocioViewModel
  ├── ObtenerAnalisisMensualUseCase  → produce AnalisisMensual
  ├── ObtenerDeudoresUseCase         → produce List<Deudor>
  └── GenerarRecomendacionesUseCase  → produce List<Recomendacion>
  │
  └── StateFlow<AnalisisNegocioUiState>
        ├── mesSeleccionado: LocalDate
        ├── analisis: AnalisisMensual?
        ├── deudores: List<Deudor>
        ├── recomendaciones: List<Recomendacion>
        ├── isLoading: Boolean
        └── error: String?
```

Same inject pattern as existing VMs: `@HiltViewModel` with `@Inject constructor`. On month change, relaunch all 3 UseCases in parallel via `coroutineScope { async { ... } }` and combine results into a single state update.

### Screen states

- **Loading**: shimmer/linear progress (reuse existing `LinearProgressIndicator` pattern)
- **Data**: cards with numbers, recommendations, expandable sections
- **Error**: card with message + retry button
- **Offline**: `AnalisisMensual.esOffline` → banner "Datos limitados — sin conexión" with partial data
- **Empty**: "Sin datos para este mes" placeholder card

### Navigation

```kotlin
// DrawerSections.kt — replace existing BI entry
if (showBiYReportes) {
    NavigationDrawerItem(
        label = { Text("Mi Negocio", ...) },
        selected = currentRoute == "analisis_negocio",
        onClick = { navController.navigateDrawer("analisis_negocio") },
        icon = { Icon(Icons.Default.TrendingUp, ...) }
    )
}

// MainDrawerScreen.kt NavHost
composable("estadisticas_bi") { AnalisisNegocioScreen(navController) }
composable("analisis_detalle") { AnalisisDetalleScreen(navController) }
```

### UI Components (to create)

| Component | Location | Description |
|-----------|----------|-------------|
| `MonthSwitcher` | inline or `ui/components/` | Horizontal swipe + arrow buttons for month selection |
| `ResumenCard` | inline in Screen | Big number card: "Este mes vendiste S/ X" |
| `RecomendacionCard` | inline in Screen | Priority-colored card with action button + dismiss |
| `ExpandableSection` | inline in DetalleScreen | Clickable header + animated Visibility |
| `BarraIngresosEgresos` | inline in DetalleScreen | Stacked horizontal bars (ventas, cobros, costos, gastos, ganancia) |

All inline unless reusable elsewhere. Compose is verbose — total ~800-1000 lines across both screens.

### Paydown / Cleanup

- `BIScreen.kt` — DELETE (subsumed by new screens)
- `BIViewModel.kt` — DELETE (subsumed by new ViewModel)
- `BIScreen.kt`'s shared composables (`KPICard`, `KpiMiniCard`, `LegendItem`) — KEEP in file for now (they're private), remove only if no other references. Grep shows no external references.

### Dependencies

- Fase 6 (ventas RPC + Room + sync)
- Fase 7 (AnalisisMensual, Deudores models)
- Fase 8 (Recomendaciones, ConfiguracionFinanciera)
- Existing: OptoTokens, DrawerSections, MainDrawerScreen, NavHost, hiltViewModel pattern

### Size Estimate

| File | Est. Lines | Notes |
|------|-----------|-------|
| `AnalisisNegocioScreen.kt` | ~350 | Main screen with month selector, summary card, recommendations |
| `AnalisisDetalleScreen.kt` | ~400 | Detail with 5 expandable sections, bars, lists |
| `AnalisisNegocioViewModel.kt` | ~120 | UseCase orchestration, month changes, error handling |
| `OptoTokens.kt` / `Color.kt` | +10 | Add 4 semantic colors |
| `DrawerSections.kt` | ~5 changed | Rename entry + update route |
| `MainDrawerScreen.kt` | ~5 changed | Add/update composable routes |
| **Total new** | **~870** | |
| **Deleted** | **~527** | `BIScreen.kt` (330) + `BIViewModel.kt` (197) |
| **Net delta** | **~350** | |

### Rollback Plan

- **Revert**: restore `DrawerSections.kt` entry to "Estadísticas (BI)" with route `estadisticas_bi`, restore `MainDrawerScreen.kt` to point `estadisticas_bi` → `BIScreen(navController)`, add back `BIScreen.kt` and `BIViewModel.kt` from git history, delete new files.
- **Low risk**: all changes are UI-only, no schema/RLS affected. No data loss possible.
- **Supabase schema/RLS**: NOT affected — Fase 9 is pure UI.

### Risks

1. **Brand colors not in theme** — `#2C3E50`, `#27AE60`, `#E74C3C` need to be added to OptoTokens. If the plan colors differ from M3 dynamic, they coexist (app uses M3 primary/secondary, new screens use semantic aliases).
2. **HorizontalPager interaction** — first use of swipe gesture in this app. Test on real device for gesture conflicts with drawer edge swipe.
3. **Offline data** — `GenerarRecomendacionesUseCase` calls RPCs which fail offline. The fallback room data in `ObtenerAnalisisMensualUseCase` returns `esOffline=true` — the UI must handle this gracefully.
4. **TDD mode** — tests must be written before implementation. All ViewModel and navigation tests needed first.

### Open Questions

- Month swipe: HorizontalPager with `animateToPage` or custom `Modifier.horizontalScroll` + snap? Prefer HorizontalPager via `foundation.pager` (Material 3). Confirm it works with `Modifier.snap` for month switching.
