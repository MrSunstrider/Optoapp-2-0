# Backfill `resumen_diario` with real costs

Change: `costos-margenes-reales` (S19, S23). Deploy gate: G5.

## Why

Before S19, `resumen_diario.ventas_costo_total` ignored servicio labor cost, servicio frame items and gifts, and counted claimed (`Reclamada`) sales. Every day already stored keeps those overstated margins until it is recalculated. This runbook rewrites the historical rows once, with a before/after delta report per optica.

## Authorization (read first)

This is a remote, production write. **Do not run any command in this runbook without explicit user authorization that names all three of:**

1. **Destination**: the exact Supabase project (production is `sflhtihqdhrlryeyrzdo`).
2. **Operation**: this backfill, with its date range and the list of opticas.
3. **Credential**: which credential or session runs it (for example a `service_role` SQL session opened by the user).

Local development access, an existing CLI login, a linked project or a stored password is **not** authorization. If any of the three is missing or ambiguous, stop and ask. Never use `supabase db push` or `--linked` as part of this runbook.

## Preconditions

- G4 is done: migrations `20261015120000`, `20261015130000` and `20261015140000` are applied on the destination, deployed through their own authorized gate.
- `recalcular_resumen_diario_admin` shares `app_private.recalcular_resumen_diario_core` with the member RPC. Confirm this before backfilling, because the old admin body wrote the overstated costs:

```sql
SELECT prosrc LIKE '%app_private.recalcular_resumen_diario_core%' AS delega_al_core
FROM pg_proc WHERE oid = 'public.recalcular_resumen_diario_admin(text, date)'::regprocedure;
```

  Expected: `true`. If `false`, stop: G4 is incomplete.

## Procedure

Run all steps in one SQL session as `service_role`, the only role allowed to execute `recalcular_resumen_diario_admin`. The snapshots are temporary tables, so the session must stay open until the delta report is saved.

### 1. Before snapshot

```sql
CREATE TEMP TABLE backfill_antes AS
SELECT optica_id, fecha, ventas_monto_total, ventas_costo_total
FROM public.resumen_diario;
```

### 2. Backfill

Each optica is recalculated from its first sale to today. Every day is upserted, so the script can be re-run safely after an interruption.

```sql
DO $$
DECLARE
    v_optica TEXT;
    v_desde DATE;
    v_dia DATE;
BEGIN
    FOR v_optica, v_desde IN
        SELECT optica_id, min(fecha) FROM (
            SELECT optica_id, fecha FROM public.dispensaciones
            UNION ALL
            SELECT optica_id, fecha FROM public.servicios_extra
        ) ventas
        GROUP BY optica_id
    LOOP
        FOR v_dia IN SELECT generate_series(v_desde, CURRENT_DATE, interval '1 day')::date LOOP
            PERFORM public.recalcular_resumen_diario_admin(v_optica, v_dia);
        END LOOP;
        RAISE NOTICE 'optica % recalculated from %', v_optica, v_desde;
    END LOOP;
END $$;
```

To limit the run to the opticas or range the user authorized, add a `WHERE optica_id IN (...)` and adjust `v_desde`.

### 3. Delta report per optica

The comparison covers only days that already had a row before the backfill, so it measures the correction and not newly filled days; `dias_nuevos` counts those separately. Net margin is sales minus cost of sales minus operating expenses (`gastos_operativos`) on the compared days; the backfill does not change expenses, so the net delta equals the gross delta.

```sql
WITH comparados AS (
    SELECT a.optica_id,
           a.ventas_monto_total AS ventas_antes, a.ventas_costo_total AS costo_antes,
           r.ventas_monto_total AS ventas_despues, r.ventas_costo_total AS costo_despues
    FROM backfill_antes a
    JOIN public.resumen_diario r USING (optica_id, fecha)
), gastos AS (
    SELECT g.optica_id, sum(g.monto) AS gastos
    FROM public.gastos_operativos g
    WHERE EXISTS (SELECT 1 FROM backfill_antes a WHERE a.optica_id = g.optica_id AND a.fecha = g.fecha)
    GROUP BY g.optica_id
), nuevos AS (
    SELECT r.optica_id, count(*) AS dias_nuevos
    FROM public.resumen_diario r
    WHERE NOT EXISTS (SELECT 1 FROM backfill_antes a WHERE a.optica_id = r.optica_id AND a.fecha = r.fecha)
    GROUP BY r.optica_id
)
SELECT c.optica_id,
       sum(c.costo_antes)                                            AS costo_antes,
       sum(c.costo_despues)                                          AS costo_despues,
       sum(c.costo_despues - c.costo_antes)                          AS delta_costo,
       sum(c.ventas_antes - c.costo_antes) - coalesce(max(g.gastos), 0)     AS margen_neto_antes,
       sum(c.ventas_despues - c.costo_despues) - coalesce(max(g.gastos), 0) AS margen_neto_despues,
       sum((c.ventas_despues - c.costo_despues) - (c.ventas_antes - c.costo_antes)) AS delta_margen_neto,
       coalesce(max(n.dias_nuevos), 0)                               AS dias_nuevos
FROM comparados c
LEFT JOIN gastos g USING (optica_id)
LEFT JOIN nuevos n USING (optica_id)
GROUP BY c.optica_id
ORDER BY delta_margen_neto;
```

Save the output with the run date and attach it to the release. Expected shape: `delta_costo >= 0` and `delta_margen_neto <= 0` for every optica. Sales can drop too, because claimed sales no longer count. A positive `delta_margen_neto` needs investigation before the release note goes out. An optica that had no rows before the backfill does not appear in the report.

### 4. Verify

Pick one optica from the report, note one recalculated day, then let a member of that optica run a finanzas sync in the app (it calls the member RPC `recalcular_resumen_diario`). The row must not change, because both RPCs share the same core.

## Rollback

`resumen_diario` is a derived cache: sources are never modified by this runbook. Reverting the numbers means restoring `backfill_antes` into `resumen_diario`, which brings the overstated margins back. Only do it with the same explicit authorization (destination, operation, credential).

## Release note

Share with the opticas after the delta report is reviewed:

> **Márgenes más precisos.** Ahora el costo de cada venta incluye la mano de obra de los servicios, las monturas usadas en servicios y los regalos entregados, y ya no cuenta las ventas reclamadas. Por eso verás márgenes más bajos que antes en tus reportes: son tus márgenes reales. Tus registros de ventas y cobros no se modificaron.
