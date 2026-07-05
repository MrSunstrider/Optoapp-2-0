# Proposal: Corregir Cierre de Caja, Reportes, BI y fix de anulaciones

## Intent

El bug raíz del módulo de ingresos: "TOTAL VENTAS DEL DÍA" en Cierre de Caja solo suma `dispensaciones`, ignorando `servicios_extra`. Fase 1 ya creó la tabla canónica `ventas` (ledger) con backfill, triggers y Room. Esta fase migra Cierre de Caja, Reportes y Dashboard BI a consultar `ventas` como única fuente de ingresos, y corrige el bug de anulaciones que crea reversals con fecha incorrecta.

## Scope

### In Scope
- `CierreCajaViewModel`: reemplazar queries separadas dispensaciones + servicios_extra por una sola query contra `ventas` (VentaDao). Label "TOTAL VENTAS DEL DÍA" usa `totalGeneral`.
- `CierreCajaScreen`: mostrar `totalGeneral` en la label principal; desglose de servicios extra se mantiene como informativo.
- `ReportesViewModel`: migrar `totalVendido`, `totalPagado`, `totalCobrado` y `cobrosPeriodo` a usar `ventas`.
- `BIViewModel`: `recaudacionProyectada` y `recaudacionCobrada` desde `ventas`.
- `DispensacionRepository.deletePagoRegistrandoAnulacionEnCaja`: fix fecha — usar `existing.fecha` en vez de `DateUtils.today()`.
- Migrar `rpc_resumen_financiero`: reemplazar UNION de `dispensaciones` + `servicios_extra` por query directa a `ventas`.
- Tests de regresión en los 3 ViewModels.

### Out of Scope
- Fase 3 (`InformacionFinancieraScreen`) y siguientes.
- Refactor de `pagos.dispensacion_id`/`servicio_extra_id` (conviven durante transición).
- Modificaciones a la UI de Reportes/BI más allá de cambiar fuente de datos.

## Capabilities

### New Capabilities
None — todos los cambios son refactors internos que no introducen nuevas capabilities a nivel de spec.

### Modified Capabilities
- `cierre-caja`: requirement "Servicios Extra in Cierre Totals" cambia `totalVentasHoy`/`totalServiciosExtra` de consultar `DispensacionOptica`/`ServicioExtra` a consultar `Venta`. Requirement "CierreCajaUiState desglose" se actualiza para reflejar fuente `ventas`.
- `reportes-financieros`: requirements `totalVendido`, `totalPagado`, `BI recaudacionProyectada` migran de sumar `DispensacionOptica.montoTotal + ServicioExtra.montoTotal` a sumar `Venta.montoTotal`. `totalCobrado` y `cobrosPeriodo` se simplifican usando `venta_id` en pagos.

## Approach

1. **CierreCajaViewModel**: inyectar `VentaDao`. En `observePagos()`, reemplazar `repository.getAllDispensacionesForOptica()` + `repository.getAllServiciosForOptica()` por `ventaDao.getVentasByOpticaAndDateRange()`. `totalVentasHoy` y `totalServiciosExtra` se derivan filtrando `ventas` por `origen` (ya no dos queries separadas).
2. **CierreCajaScreen**: label "TOTAL VENTAS DEL DÍA" muestra `uiState.totalGeneral`.
3. **ReportesViewModel**: `allDispensaciones` y `allServiciosDelPeriodo` se reemplazan por un único `allVentasDelPeriodo` desde `VentaDao`. `totalVendido` = sum de `Venta.montoTotal`. `totalPagado` = sum de pagos vía `venta_id`.
4. **BIViewModel**: `recaudacionProyectada` suma `Venta.montoTotal` en vez de `disp.montoTotal + serv.montoTotal`.
5. **Anulaciones fix**: eliminar `fechaAnulacion` parameter default. Usar `existing.fecha` para el reversal.
6. **RPC**: reemplazar queries separadas `dispensaciones` y `servicios_extra` con `SELECT ... FROM public.ventas WHERE optica_id = p_optica_id AND fecha >= p_from AND fecha < p_to`.
7. **Tests**: unit tests con Room in-memory + VentaDao para cada ViewModel modificado.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/CierreCajaViewModel.kt` | Modified | Queries dispensaciones+servicios → ventas |
| `optoapp/.../ui/screens/CierreCajaScreen.kt` | Modified | Label usa totalGeneral |
| `optoapp/.../viewmodel/ReportesViewModel.kt` | Modified | Sumas desde ventas |
| `optoapp/.../viewmodel/BIViewModel.kt` | Modified | KPIs desde ventas |
| `optoapp/.../data/DispensacionRepository.kt` | Modified | Fix fecha en anulación |
| `supabase/migrations/20260514000000_rpc_resumen_financiero.sql` | Modified | Query ventas directa |
| `openspec/specs/cierre-caja/spec.md` | Modified | Delta spec por cambio de fuente |
| `openspec/specs/reportes-financieros/spec.md` | Modified | Delta spec por cambio de fuente |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `ventas` local no poblada en Room (modo offline antes de primer sync) | Low | Fallback: si VentaDao retorna vacío, usar queries legacy como fallback temporal |
| `VentaDao.getVentasByOpticaAndDateRange` no cubre todos los filtros de Reportes | Low | Agregar queries adicionales al DAO si es necesario |
| Regresión en totales existentes | Medium | Test comparativo: mismos datos, mismos resultados antes/después |

## Rollback Plan

- **ViewModels**: revertir archivos individuales desde git. Cada ViewModel es independiente.
- **RPC**: `git checkout HEAD -- supabase/migrations/20260514000000_rpc_resumen_financiero.sql` + `supabase db reset` local + re-aplicar migraciones.
- **Anulaciones fix**: revertir `DispensacionRepository.kt`.
- Convivencia de `venta_id` con `dispensacion_id`/`servicio_extra_id` en `pagos` permite rollback sin pérdida de datos.

## Dependencies

- Fase 1 completa (`ventas` table, Venta entity, VentaDao, triggers en Supabase). Verificado: commit 60d93e1.
- `pagos.venta_id` poblado (backfill de Fase 1).

## Success Criteria

- [ ] Cierre de Caja "TOTAL VENTAS DEL DÍA" = suma de `ventas.montoTotal` para hoy (incluye dispensaciones + servicios_extra)
- [ ] Reportes financieros (`totalVendido`, `totalPagado`, `totalCobrado`) producen mismos números que antes
- [ ] BI Dashboard `recaudacionProyectada` = suma de `ventas.montoTotal` en el período
- [ ] Anular un pago del 29/06 no contamina el Cierre de Caja del 04/07
- [ ] `rpc_resumen_financiero` retorna los mismos 6 campos con datos correctos
- [ ] Tests unitarios pasan (`./gradlew :optoapp:testDebugUnitTest`)
