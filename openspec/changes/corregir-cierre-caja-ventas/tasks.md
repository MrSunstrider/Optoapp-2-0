# Tasks: Corregir Cierre de Caja, Reportes, BI y fix de anulaciones

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~390 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All changes in single PR | PR 1 | Independent per-file, no stacked deps |

## Phase 1: Foundation

- [x] 1.1 Fix `DispensacionRepository.deletePagoRegistrandoAnulacionEnCaja` — remove `fechaAnulacion` default param; set `reversal.fecha = existing.fecha` (ANUL-1-a/b/c)
- [x] 1.2 Rewrite `supabase/migrations/20260514000000_rpc_resumen_financiero.sql` — replace `dispensaciones`+`servicios_extra` UNION with direct `SELECT FROM public.ventas` (RPC-1-a/b)

## Phase 2: Cierre de Caja — TDD

- [x] 2.1 RED: add tests to `CierreCajaViewModelTest` — `totalGeneral` from ventas (CC-1-a/b), serviciosExtraHoy as `List<Venta>` (CC-1-c), pago via `ventaId` (CC-1-d), orphan pago fallback (CC-1-e)
- [x] 2.2 GREEN: inject `VentaDao` into `CierreCajaViewModel`; replace `getAllDispensacionesForOptica`+`getAllServiciosForOptica` with `ventaDao.getVentasByOpticaAndDateRange` in `observePagos()`; derive desglose by `origen`; keep existing pago classification via legacy IDs
- [x] 2.3 Update `CierreCajaScreen` — "TOTAL VENTAS DEL DÍA" label shows `uiState.totalGeneral`; servicios detail iterates `List<Venta>` (CC-2-a)

## Phase 3: Reportes — TDD

- [x] 3.1 RED: create `ReportesViewModelTest` — `totalVendido` from ventas (RF-1-a/b), empty period returns zero
- [x] 3.2 GREEN: inject `VentaDao` into `ReportesViewModel`; add `allVentasDelPeriodo: StateFlow<List<Venta>>`; change `totalVendido` to sum `Venta.montoTotal` (keep `totalPagado` on legacy entities per design decision)

## Phase 4: BI Dashboard — TDD

- [x] 4.1 RED: add tests to `BIViewModelTest` — `recaudacionProyectada` from ventas (BI-1-a/b), empty period zero (BI-1-c)
- [x] 4.2 GREEN: inject `VentaDao` into `BIViewModel`; replace `dispensaciones.sumOf { montoTotal } + servicios.sumOf { montoTotal }` with `ventaDao.getVentasByOpticaAndDateRange` in `observeStats()`; remove `repository.getAllServiciosForOptica()` from combine chain

## Phase 5: Verification

- [x] 5.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all new and existing tests pass
- [x] 5.2 Verify equivalence: old vs new totals match for identical seeded data (CONSIST-1)
