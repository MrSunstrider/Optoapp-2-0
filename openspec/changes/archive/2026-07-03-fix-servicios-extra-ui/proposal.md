# Proposal: Fix servicios extra UI rendering and sync normalization

## Change Summary

Six bugs prevent servicios extra from being correctly displayed in cierre de caja and reportes screens, and cause raw `metodoPago` values to bypass normalization at the sync boundary.

| # | Location | Bug | Impact |
|---|----------|-----|--------|
| 1 | `TransactionItem.kt:26` | Label uses 2-way when (`dispensacionId != null` → "Dispensación", else → "Servicio Extra"). A pago with neither `dispensacionId` nor `servicioExtraId` displays "Servicio Extra" incorrectly. | Wrong label on orphan pagos |
| 2 | `CierreCajaScreen.kt` | `CierreCajaUiState` exposes `serviciosExtraHoy` and `totalServiciosExtra` but the screen never renders them. "TOTAL VENTAS DEL DÍA" shows only dispensaciones. No servicios extra section exists. | Servicios extra invisible in cierre de caja |
| 3 | `ReportesScreen.kt:210` | `LazyColumn` with `items(dispensaciones)` only renders dispensaciones. `ReportesViewModel.allServiciosDelPeriodo` exists but the screen never collects it. Servicios extra contribute to totals but are invisible in the detail list. | Servicios extra invisible in reportes detail |
| 4 | `ReportesScreen.kt:85-92` / `ReporteFinancieroPdfGenerator.kt` | PDF generator `generate()` only receives `dispensaciones: List<DispensacionOptica>`, not servicios extra. Detail section iterates only dispensaciones. | PDF report incomplete |
| 5 | `SyncFinanzasDto.kt:119` | `PagoRemoto.toEntity()` copies `metodoPago = metodoPago` verbatim with no normalization. `ServicioRemoto.toEntity()` normalizes via `remotoServicioExtraMetodoToLocal()`, but Pago does not. | Raw/unnormalized metodoPago stored for pagos |
| 6 | `CierreCajaViewModel.kt:150-152` | `getTotalesPorMetodo()` does raw `groupBy { it.metodoPago }` creating map keys from unnormalized values. No filtering or normalization applied. | Totals by method unreliable |

## Intent

Make servicios extra **visible** in cierre de caja and reportes UI, and **normalize** `metodoPago` at the sync boundary so all downstream consumers (totals by method, PDF reports, UI labels) operate on clean data.

## Scope

### Files to modify

| File | Change |
|------|--------|
| `optoapp/.../ui/components/cierre-caja/TransactionItem.kt` | Fix label to 3-way when: disp → "Dispensación", servExtra → "Servicio Extra", orphan → "Pago" |
| `optoapp/.../ui/screens/CierreCajaScreen.kt` | Add servicios extra section: show `serviciosExtraHoy` count and `totalServiciosExtra` alongside existing dispensaciones breakdown |
| `optoapp/.../ui/screens/ReportesScreen.kt` | Collect `allServiciosDelPeriodo`, merge with dispensaciones in "Detalle de Ventas" `LazyColumn`, pass servicios to PDF generator |
| `optoapp/.../util/ReporteFinancieroPdfGenerator.kt` | Add `serviciosExtra: List<ServicioExtra>` parameter, render servicios extra rows in PDF detail section |
| `optoapp/.../domain/SyncFinanzasDto.kt` | Normalize `metodoPago` in `PagoRemoto.toEntity()` (add `.remotoPagoMetodoToLocal()` or reuse existing normalizer) |
| `optoapp/.../viewmodel/CierreCajaViewModel.kt` | Normalize `metodoPago` in `getTotalesPorMetodo()` before grouping |

### Files NOT in scope

- `ReportesViewModel.kt` — totals (`totalVendido`, `totalPagado`, `cobrosPeriodo`) already include servicios extra correctly (confirmed in exploration)
- `BIViewModel.kt` — separate change, out of scope
- `OptoRepository.kt` — already exposes `getAllServiciosForOptica()`

## Approach: TDD

Write failing tests first, then implement fixes.

### Test plan

| Test | Target | Assertion |
|------|--------|-----------|
| `TransactionItemTest` — orphan pago label | `TransactionItem.kt` | Pago with null dispensacionId and null servicioExtraId renders "Pago" |
| `TransactionItemTest` — servicio extra label | `TransactionItem.kt` | Pago with null dispensacionId and non-null servicioExtraId renders "Servicio Extra" |
| `TransactionItemTest` — dispensacion label | `TransactionItem.kt` | Pago with non-null dispensacionId renders "Dispensación" |
| `CierreCajaViewModelTest` — servicios extra in totalGeneral | `CierreCajaViewModel.kt` | `totalGeneral == totalVentasHoy + totalServiciosExtra` when servicios exist for today |
| `CierreCajaViewModelTest` — getTotalesPorMetodo normalizes | `CierreCajaViewModel.kt` | Pagos with raw metodoPago values are normalized before grouping |
| `PagoRemotoTest` — metodoPago normalization | `SyncFinanzasDto.kt` | `PagoRemoto.toEntity()` normalizes metodoPago the same way `ServicioRemoto.toEntity()` does |
| `ReporteFinancieroPdfGeneratorTest` — servicios in PDF | `ReporteFinancieroPdfGenerator.kt` | PDF contains servicios extra rows when list is non-empty |

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Minimal — UI-only changes + one sync DTO normalization | Low | All changes are additive or corrective; no data model changes |
| `PagoRemoto.toEntity()` normalization must match `ServicioRemoto.toEntity()` behavior exactly | Low | Extract shared normalizer or reuse `remotoServicioExtraMetodoToLocal()` — same function, same behavior |
| CierreCajaScreen UI addition may affect layout | Low | Add servicios extra as a subsection within existing structure; no structural layout changes |
| PDF generator signature change requires updating all call sites | Low | Only one call site (`ReportesScreen.kt:85-92`) |

## Dependencies

- Existing `repository.getAllServiciosForOptica()` — already exposed ✅
- Existing `remotoServicioExtraMetodoToLocal()` — reuse for pago normalization ✅
- Existing test infrastructure (Robolectric, Mockk) — standard ✅

## Ready for Spec

Yes
