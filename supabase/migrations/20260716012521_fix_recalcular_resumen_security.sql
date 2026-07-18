-- ============================================================================
-- Migration: Fix recalcular_resumen_security
-- Date: 2026-07-16
--
-- Changes from 20260714000000_fix_recalcular_resumen_diario.sql:
--   1. SECURITY INVOKER → SECURITY DEFINER (function runs as owner)
--   2. SET search_path = public → SET search_path = '' (no schema trust)
--   3. Added AND d.estado_entrega IS DISTINCT FROM 'Anulado' / AND se.estado IS
--      DISTINCT FROM 'Anulado' to ALL ventas-related queries, excluding
--      cancelled sales from aggregates.
--   4. Added ALTER FUNCTION ... OWNER TO postgres after the function.
-- ============================================================================

CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_ventas_cantidad INTEGER;
    v_ventas_monto NUMERIC;
    v_ventas_costo NUMERIC;
    v_cobros_cantidad INTEGER;
    v_cobros_monto NUMERIC;
    v_saldo_total NUMERIC;
    v_saldo_cantidad INTEGER;
    v_inv_valor NUMERIC;
    v_inv_unidades INTEGER;
BEGIN
    -- Daily sales: UNION ALL of source-of-truth tables
    -- Cost = real-cost from dispensacion_items (fallback 0 if no items)
    -- Excludes cancelled (Anulado) records
    WITH daily_ventas AS (
        SELECT d.monto_total,
            COALESCE((
                SELECT SUM(
                    COALESCE(di.costo_real_od, 0) +
                    COALESCE(di.costo_real_oi, 0) +
                    COALESCE(di.costo_real_montura, 0) +
                    COALESCE(di.costo_real_biselado, 0) +
                    COALESCE(di.costo_real_lc, 0)
                ) FROM public.dispensacion_items di
                WHERE di.dispensacion_id = d.id
            ), 0) AS costo
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id AND d.fecha = p_fecha
          AND d.estado_entrega IS DISTINCT FROM 'Anulado'
        UNION ALL
        SELECT se.monto_total, 0::numeric AS costo
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id AND se.fecha = p_fecha
          AND se.estado IS DISTINCT FROM 'Anulado'
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(monto_total), 0),
           COALESCE(SUM(costo), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM daily_ventas;

    -- Daily payments (exclude Anulaciones)
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha
      AND tipo IS DISTINCT FROM 'Anulación';

    -- Accumulated pending balance via namespace-keyed LEFT JOIN
    -- Excludes cancelled (Anulado) ventas
    WITH pagos_dedup AS (
        SELECT COALESCE(pg.venta_id,
               'v_disp_' || pg.dispensacion_id,
               'v_serv_' || pg.servicio_extra_id) AS venta_id_match,
               pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
          AND pg.tipo IS DISTINCT FROM 'Anulación'
    ), all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND estado_entrega IS DISTINCT FROM 'Anulado'
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND estado IS DISTINCT FROM 'Anulado'
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_saldo_cantidad, v_saldo_total
    FROM all_ventas v
    LEFT JOIN (
        SELECT venta_id_match, SUM(monto) AS total_pagado
        FROM pagos_dedup
        GROUP BY venta_id_match
    ) pd ON pd.venta_id_match = v.venta_id
    WHERE v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    -- Inventory snapshot (unchanged)
    SELECT COALESCE(SUM(costo * stock_actual), 0),
           COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas
    WHERE optica_id = p_optica_id;

    -- Idempotent upsert
    INSERT INTO public.resumen_diario (
        optica_id, fecha,
        ventas_cantidad, ventas_monto_total, ventas_costo_total,
        cobros_cantidad, cobros_monto_total,
        saldo_pendiente_total, saldo_pendiente_cantidad,
        inventario_valor, inventario_unidades
    ) VALUES (
        p_optica_id, p_fecha,
        v_ventas_cantidad, v_ventas_monto, v_ventas_costo,
        v_cobros_cantidad, v_cobros_monto,
        v_saldo_total, v_saldo_cantidad,
        v_inv_valor, v_inv_unidades
    )
    ON CONFLICT (optica_id, fecha) DO UPDATE SET
        ventas_cantidad = EXCLUDED.ventas_cantidad,
        ventas_monto_total = EXCLUDED.ventas_monto_total,
        ventas_costo_total = EXCLUDED.ventas_costo_total,
        cobros_cantidad = EXCLUDED.cobros_cantidad,
        cobros_monto_total = EXCLUDED.cobros_monto_total,
        saldo_pendiente_total = EXCLUDED.saldo_pendiente_total,
        saldo_pendiente_cantidad = EXCLUDED.saldo_pendiente_cantidad,
        inventario_valor = EXCLUDED.inventario_valor,
        inventario_unidades = EXCLUDED.inventario_unidades,
        calculado_en = now();
END;
$$;

ALTER FUNCTION public.recalcular_resumen_diario(TEXT, DATE) OWNER TO postgres;

REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;
