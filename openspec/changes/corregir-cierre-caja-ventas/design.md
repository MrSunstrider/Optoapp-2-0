# Design: Corregir Cierre de Caja, Reportes, BI y fix de anulaciones

## Technical Approach

Migrate CierreCaja, Reportes, and BI Dashboard from querying `dispensaciones` + `servicios_extra` separately to querying the canonical `ventas` table as the single source for revenue totals. Fix the anulaciones bug where reversals use `DateUtils.today()` instead of the original payment date. Replace Supabase `rpc_resumen_financiero` UNION with direct `ventas` query.

## Architecture Decisions

### Decision: VentaDao direct injection in ViewModels

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Inject `VentaDao` directly into ViewModels | Breaks convention of routing through `OptoRepository`; simpler, zero boilerplate | **Chosen** |
| Add passthrough methods to `OptoRepository` | Follows existing pattern; adds ~5 lines of indirection per query | Rejected |

**Rationale**: `OptoRepository` already holds `ventaDao` privately and delegates `upsertVenta`/`upsertVentaFromRemote`. Exposing read-only venta queries through OptoRepository adds no value — ViewModels get the DAO directly for the single `getVentasByOpticaAndDateRange()` call.

### Decision: ReportesViewModel introduces `allVentasDelPeriodo` instead of replacing existing flows

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Replace `allDispensaciones` + `allServiciosDelPeriodo` with `allVentasDelPeriodo` | Breaks `totalPagado` (needs `ventaId` in Room Pago, which doesn't exist yet) | Rejected |
| Add `allVentasDelPeriodo` alongside existing flows; only `totalVendido` consumes it | Keeps 2 flows active temporarily; correct behavior for `totalPagado` | **Chosen** |

**Rationale**: `Venta.montoTotal` replaces `disp.montoTotal + serv.montoTotal` for `totalVendido`. But `totalPagado` sums denormalized `montoPagado`/`aCuenta` fields from the legacy entities — these have no analog on `Venta`. Adding `ventaId` to Room Pago requires a DB migration out of scope here (proposal explicitly states "conviven durante transición").

### Decision: Keep ventaId out of Room Pago for now

**Choice**: Defer adding `ventaId` to Room Pago to a follow-up phase.
**Rationale**: Room migration + backfill adds risk. `ventaId` exists in Supabase but not Room. The `totalCobrado` and `cobrosPeriodo` simplifications via `venta_id` will happen when `ventaId` lands in Room.

## Data Flow

### CierreCajaViewModel — antes vs después

```
ANTES (buggy):
  dispensaciones ──→ filter(fecha==hoy) ──→ sum(montoTotal) = totalVentasHoy  ──┐
  servicios_extra ──→ filter(fecha==hoy) ──→ sum(montoTotal) = totalServExtra ──┤
                                                                                  ├── totalGeneral
DESPUÉS:
  ventaDao.getVentasByOpticaAndDateRange(optica, hoy, hoy) ──→ ventas             │
    ├── filter(origen=="dispensacion")  ──→ totalVentasHoy (desglose)            │
    ├── filter(origen=="servicio_extra")──→ totalServiciosExtra (desglose)        │
    └── sum(montoTotal) ──────────────────→ totalGeneral ────────────────────────┘
```

Pagos classification (`ventasHoy`/`cobrosAtrasados`) remains on pagos — these are about money received, not revenue recorded.

### ReportesViewModel

```
ventaDao.getVentasByOpticaAndDateRange(optica, start, end) ──→ allVentasDelPeriodo
  └── sum(montoTotal) ──→ totalVendido

totalPagado, totalCobrado, cobrosPeriodo ──→ unchanged (still legacy queries)
```

### BIViewModel

```
ventaDao.getVentasByOpticaAndDateRange(optica, range.start, range.end)
  └── sum(montoTotal) ──→ recaudacionProyectada

recaudacionCobrada ──→ unchanged (pagas flow)
```

### Anulaciones fix

```
deletePagoRegistrandoAnulacionEnCaja(pago, optica)
  existing = pagoDao.getPagoById(pago.id)
  reversal.fecha = existing.fecha  ← was DateUtils.today()
```

### Supabase RPC

```
ANTES: SELECT FROM dispensaciones + SELECT FROM servicios_extra → UNION in PL/pgSQL
DESPUÉS: SELECT FROM ventas WHERE optica_id=... AND fecha >= ... AND fecha < ...
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `viewmodel/CierreCajaViewModel.kt` | Modify | Inject VentaDao. Replace `getAllDispensacionesForOptica` + `getAllServiciosForOptica` with `getVentasByOpticaAndDateRange`. Derive desglose by `origen`. |
| `ui/screens/CierreCajaScreen.kt` | Modify | "TOTAL VENTAS DEL DIA" label shows `uiState.totalGeneral` instead of `uiState.totalVentasHoy`. |
| `viewmodel/ReportesViewModel.kt` | Modify | Inject VentaDao. Add `allVentasDelPeriodo` flow. `totalVendido` from ventas. |
| `viewmodel/BIViewModel.kt` | Modify | Inject VentaDao. `recaudacionProyectada` from ventas. |
| `data/DispensacionRepository.kt` | Modify | Remove `fechaAnulacion` default; use `existing.fecha` for reversal. |
| `supabase/migrations/20260514000000_rpc_resumen_financiero.sql` | Modify | Replace dispensaciones+servicios_extra UNION with direct ventas SELECT. |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (DAO) | `VentaDao.getVentasByOpticaAndDateRange` filtering | Existing `VentaDaoTest` covers it |
| Unit (VM) | CierreCaja: `totalGeneral = sum(ventas.montoTotal)`, desglose by `origen` | Mockk VentaDao, adapt existing `CierreCajaViewModelTest` tests |
| Unit (VM) | Reportes: `totalVendido` from ventas across all periods | Mockk VentaDao, adapt existing period tests |
| Unit (VM) | BI: `recaudacionProyectada` from ventas | Mockk VentaDao, adapt existing BI test |
| Unit (Repo) | Anulacion reversal uses `existing.fecha` | Verify reversal.fecha equals original payment date |
| Integration | RPC returns same 6 fields | Supabase local test |

## Migration / Rollout

- **Room**: No schema migration needed (VentaDao already exists in Room from Fase 1). Tests with Room in-memory should verify with ventas pre-seeded.
- **Supabase**: `rpc_resumen_financiero` SQL replaced in-place; re-run `supabase db reset` locally to apply.
- **Rollback**: Each file independently revertible via git. Legacy queries remain active in ReportesVIewModel for `totalPagado` during transition.
