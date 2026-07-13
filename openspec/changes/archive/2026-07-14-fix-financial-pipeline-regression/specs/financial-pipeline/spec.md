# Delta for financial-pipeline

## MODIFIED Requirements

### R9: Supabase RPC `recalcular_resumen_diario`

MODIFIED: Data sources change from `public.ventas` to `public.dispensaciones UNION ALL public.servicios_extra`. Cost source changes from `ventas.costo_unitario_snapshot` (via JOIN) to `dispensacion_items.costo_real_*` with `costo_unitario_snapshot` fallback. Pending balance queries UNION ALL instead of `ventas`. All other logic (payments aggregation, inventory snapshot, idempotent upsert) unchanged.

#### R9.1: Calculation Logic

The function SHALL:

1. **Sales aggregation**: Compute via CTE `daily_ventas` using `SELECT ... FROM public.dispensaciones WHERE optica_id = p_optica_id AND fecha = p_fecha UNION ALL SELECT ... FROM public.servicios_extra WHERE optica_id = p_optica_id AND fecha = p_fecha`. For each dispensacion row, cost SHALL be `SUM(COALESCE(costo_real_od,0) + COALESCE(costo_real_oi,0) + COALESCE(costo_real_montura,0) + COALESCE(costo_real_biselado,0) + COALESCE(costo_real_lc,0))` from `dispensacion_items WHERE dispensacion_id = d.id`. If no items exist, SHALL fall back to `COALESCE(d.costo_unitario_snapshot, 0)`. For servicio_extra rows, cost SHALL be `0::numeric`.

2. **Payments aggregation**: UNCHANGED from existing spec.

3. **Pending balance**: Query via UNION ALL of `dispensaciones` + `servicios_extra`, LEFT JOIN aggregated `pagos` by `venta_id` namespace key. For dispensaciones the join key is `'v_disp_' || d.id`; for servicios_extra it is `'v_serv_' || se.id`.

4. **Inventory snapshot**: UNCHANGED.

5. **Idempotent upsert**: UNCHANGED.

(Previously: R9 read from `public.ventas` table for sales aggregation, cost via JOIN through `ventas → dispensaciones → dispensacion_items`, and pending balance via `public.ventas`.)

#### Scenario: costo_real_* from dispensacion_items is used when items exist

- GIVEN a dispensacion on 2026-07-05 with linked items having `costo_real_od = 25.00` and `costo_real_montura = 80.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `105.00` for that row, NOT `costo_unitario_snapshot`

#### Scenario: costo_unitario_snapshot fallback when no items exist

- GIVEN a dispensacion on 2026-07-05 with no linked items, having `costo_unitario_snapshot = 15.00`
- WHEN `recalcular_resumen_diario('o1', '2026-07-05')` is called
- THEN `ventas_costo_total` includes `15.00` from the fallback column

#### Scenario: UNION ALL output matches transactional SUM

- GIVEN test data with 3 dispensaciones (total S/ 500) and 2 servicios_extra (total S/ 200) on the same fecha
- WHEN `recalcular_resumen_diario('test_o', '2026-07-01')` is called
- THEN `resumen_diario.ventas_monto_total` = `700.00` (the exact SUM of both source tables)

#### Scenario: Idempotent upsert does not duplicate rows

- GIVEN `recalcular_resumen_diario('o1', '2026-07-05')` has been called once
- WHEN it is called a second time with no data changes
- THEN `resumen_diario` has exactly 1 row for `('o1', '2026-07-05')`
- AND `calculado_en` is updated to the newer timestamp
