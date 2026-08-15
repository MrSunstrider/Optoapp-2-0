-- ============================================================================
-- Converge the daily/cash SQL aggregates on public.pago_effect
-- Change: fix-sync-financial-ledger / WU-1B
--
-- Follows 20260815004921_ledger_pago_effect.sql, which added public.pago_effect
-- and converged the write-path trigger. This slice converges the two read paths
-- that define daily cash before any Android writer can emit Reverso/Reembolso:
--   * public.recalcular_resumen_diario  (cobros total + pending balance)
--   * public.rpc_cierre_caja_resumen    (per-method + total cash)
--
-- Only aggregate expressions and the cancelled/claimed sale filters change.
-- Signatures, security context, search_path, guards, grants and every output
-- field keep their prior contract. Bodies are full CREATE OR REPLACE copies of
-- the latest authoritative versions (20260721000001 / 20260721000000).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. recalcular_resumen_diario
--    cobros_monto_total and per-sale paid totals now sum the signed effect, so
--    Reverso/Reembolso reduce cash and legacy Anulación contributes 0.
--    Reclamada dispensaciones join Anulado as non-active sales (spec
--    servicio-extra: active-sale and debt queries exclude both).
-- ----------------------------------------------------------------------------
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
    -- Verify caller belongs to the optica (prevents cross-tenant writes via SECURITY DEFINER)
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    -- Daily sales: UNION ALL of source-of-truth tables
    -- Cost = real-cost from dispensacion_items (fallback 0 if no items)
    -- Excludes cancelled (Anulado) and claimed (Reclamada) records
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
          AND d.estado_entrega IS DISTINCT FROM 'Reclamada'
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

    -- Daily payments: signed cash effect per tipo (Anulación contributes 0).
    -- The tipo filter is kept so cobros_cantidad keeps counting cash events only.
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(public.pago_effect(tipo, monto)), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha
      AND tipo IS DISTINCT FROM 'Anulación';

    -- Accumulated pending balance via namespace-keyed LEFT JOIN
    -- Excludes cancelled (Anulado) and claimed (Reclamada) ventas
    WITH pagos_dedup AS (
        SELECT COALESCE(pg.venta_id,
               'v_disp_' || pg.dispensacion_id,
               'v_serv_' || pg.servicio_extra_id) AS venta_id_match,
               public.pago_effect(pg.tipo, pg.monto) AS efecto
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
          AND pg.tipo IS DISTINCT FROM 'Anulación'
    ), all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND estado_entrega IS DISTINCT FROM 'Anulado'
          AND estado_entrega IS DISTINCT FROM 'Reclamada'
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
        SELECT venta_id_match, SUM(efecto) AS total_pagado
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

-- ----------------------------------------------------------------------------
-- 2. rpc_cierre_caja_resumen
--    Per-method and total cash now sum the signed effect. Reverso/Reembolso
--    reduce the method they were collected on; Anulación stays cash-neutral.
--    SECURITY INVOKER, so callers need EXECUTE on pago_effect (granted to
--    authenticated/service_role in 20260815004921).
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(
    p_optica_id TEXT,
    p_from DATE,
    p_to DATE
) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_efectivo numeric;
    v_movil_trans numeric;
    v_tarjeta numeric;
    v_total numeric;
BEGIN
    -- Fix 2.13: Verify caller belongs to the optica
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    -- Fix 2.29: Verify caller has BI-level role
    IF NOT app_private.has_optica_role(auth.uid(), p_optica_id, ARRAY['admin', 'gerente']) THEN
        RAISE EXCEPTION 'BI access requires admin or gerente role';
    END IF;

    SELECT
        COALESCE(SUM(CASE WHEN metodo_pago = 'Efectivo' THEN public.pago_effect(tipo, monto) ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN metodo_pago IN ('Transferencia', 'Yape', 'Plin', 'Móvil') THEN public.pago_effect(tipo, monto) ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN metodo_pago = 'Tarjeta' THEN public.pago_effect(tipo, monto) ELSE 0 END), 0),
        COALESCE(SUM(public.pago_effect(tipo, monto)), 0)
    INTO
        v_efectivo,
        v_movil_trans,
        v_tarjeta,
        v_total
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to
      AND tipo IS DISTINCT FROM 'Anulación';

    RETURN jsonb_build_object(
        'efectivo', v_efectivo,
        'movil_trans', v_movil_trans,
        'tarjeta', v_tarjeta,
        'total', v_total
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) TO authenticated, service_role;
