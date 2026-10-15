-- ============================================================================
-- resumen_diario: one shared core for the member RPC and the operator backfill
-- Change: costos-margenes-reales / S23
--
-- recalcular_resumen_diario_admin (20260802050843) carried its own copy of the
-- daily aggregation and never received later fixes: servicio cost stayed 0,
-- gifts were ignored, Reclamada sales were counted and pagos ignored
-- pago_effect. A backfill through it would write the overstated margins that
-- S19 removed. Both RPCs now delegate to app_private.recalcular_resumen_diario_core,
-- whose body is the S19 aggregation (20261015120000) verbatim, so they cannot
-- drift again.
--
-- Contracts kept: recalcular_resumen_diario stays SECURITY DEFINER with its
-- membership guard and grants (authenticated, service_role); the admin RPC
-- stays SECURITY DEFINER without a guard and EXECUTE for service_role only.
-- The core is SECURITY INVOKER and no API role can execute it; the wrappers
-- reach it as their owner.
-- Rollback (manual, never auto-applied): re-run the recalcular_resumen_diario
-- section of 20261015120000, the admin body of 20260802050843 with the grants of
-- 20260913212248, then DROP FUNCTION app_private.recalcular_resumen_diario_core(TEXT, DATE).
-- ============================================================================
CREATE OR REPLACE FUNCTION app_private.recalcular_resumen_diario_core(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY INVOKER
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
    -- Cost = stored snapshots only (never recomputed from current prices):
    --   dispensacion = item costs (0 if no items) + gifts
    --   servicio     = labor cost + frame item costs + gifts
    -- Gift costo_unitario is REAL on regalos_dispensacion, cast so sums stay exact NUMERIC.
    -- Cancelled (Anulado) and claimed (Reclamada) sales never count.
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
            ), 0)
            + COALESCE((
                SELECT SUM(rd.cantidad * rd.costo_unitario::numeric)
                FROM public.regalos_dispensacion rd
                WHERE rd.dispensacion_id = d.id
            ), 0) AS costo
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id AND d.fecha = p_fecha
          AND d.estado_entrega IS DISTINCT FROM 'Anulado'
          AND d.estado_entrega IS DISTINCT FROM 'Reclamada'
        UNION ALL
        SELECT se.monto_total,
            COALESCE(se.costo_real, 0)
            + COALESCE((
                SELECT SUM(sei.costo_real)
                FROM public.servicio_extra_items sei
                WHERE sei.servicio_extra_id = se.id
            ), 0)
            + COALESCE((
                SELECT SUM(rse.cantidad * rse.costo_unitario)
                FROM public.regalos_servicio_extra rse
                WHERE rse.servicio_extra_id = se.id
            ), 0) AS costo
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id AND se.fecha = p_fecha
          AND se.estado IS DISTINCT FROM 'Anulado'
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(monto_total), 0),
           COALESCE(SUM(costo), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM daily_ventas;

    -- Anulación contributes 0 cash; the tipo filter keeps cobros_cantidad counting cash events only.
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(public.pago_effect(tipo, monto)), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha
      AND tipo IS DISTINCT FROM 'Anulación';

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

    SELECT COALESCE(SUM(costo * stock_actual), 0),
           COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas
    WHERE optica_id = p_optica_id;

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

ALTER FUNCTION app_private.recalcular_resumen_diario_core(TEXT, DATE) OWNER TO postgres;
REVOKE EXECUTE ON FUNCTION app_private.recalcular_resumen_diario_core(TEXT, DATE) FROM PUBLIC, anon, authenticated, service_role;

CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    -- SECURITY DEFINER bypasses RLS, so membership is the only barrier against cross-tenant writes.
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    PERFORM app_private.recalcular_resumen_diario_core(p_optica_id, p_fecha);
END;
$$;

ALTER FUNCTION public.recalcular_resumen_diario(TEXT, DATE) OWNER TO postgres;
REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;

CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario_admin(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM app_private.recalcular_resumen_diario_core(p_optica_id, p_fecha);
END;
$$;

ALTER FUNCTION public.recalcular_resumen_diario_admin(TEXT, DATE) OWNER TO postgres;
REVOKE ALL ON FUNCTION public.recalcular_resumen_diario_admin(TEXT, DATE) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario_admin(TEXT, DATE) TO service_role;
COMMENT ON FUNCTION public.recalcular_resumen_diario_admin(TEXT, DATE) IS
  'SECURITY DEFINER admin backfill without membership guard — EXECUTE service_role only. Shares app_private.recalcular_resumen_diario_core with recalcular_resumen_diario.';
