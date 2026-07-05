-- ============================================================================
-- Fase 7 — Motor de 8 indicadores: RPC functions for business analytics
-- ============================================================================
-- 1. rpc_analisis_mensual — computes all 8 indicators for a given month
-- 2. rpc_deudores — lists debtors with aging from ventas + pagos + pacientes
-- 3. GRANT EXECUTE on both new RPCs
-- 4. Fix missing GRANT on recalcular_resumen_diario (from Fase 6)
-- 5. Rewrite rpc_count_pendientes to query ventas table
-- 6. Deprecation comments on rpc_resumen_financiero and rpc_saldo_pendiente
--
-- DEPRECATED (still callable for backward compatibility):
--   rpc_resumen_financiero → use rpc_analisis_mensual instead
--   rpc_saldo_pendiente    → use rpc_analisis_mensual instead
-- ============================================================================

-- ============================================================================
-- 1. rpc_analisis_mensual — 8-indicator monthly analysis
-- ============================================================================

CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(
    p_optica_id TEXT,
    p_mes DATE  -- primer dia del mes, ej. '2026-07-01'
) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER STABLE
SET search_path = public
AS $$
DECLARE
    v_ventas_mes NUMERIC;
    v_cobros_mes NUMERIC;
    v_costo_mes NUMERIC;
    v_gastos_mes NUMERIC;
    v_saldo_pendiente NUMERIC;
    v_margen_neto_pct NUMERIC;
    v_ticket_promedio NUMERIC;
    v_cantidad_ventas INTEGER;
    v_mes_anterior DATE;
    v_ventas_mes_anterior NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';

    -- Ventas, cobros y costos del mes desde resumen_diario
    SELECT COALESCE(SUM(ventas_monto_total), 0),
           COALESCE(SUM(cobros_monto_total), 0),
           COALESCE(SUM(ventas_costo_total), 0),
           COALESCE(SUM(ventas_cantidad), 0)
    INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= p_mes
      AND fecha < p_mes + INTERVAL '1 month';

    -- Ticket promedio
    v_ticket_promedio := CASE WHEN v_cantidad_ventas > 0
        THEN v_ventas_mes / v_cantidad_ventas ELSE 0 END;

    -- Gastos del mes
    SELECT COALESCE(SUM(monto), 0) INTO v_gastos_mes
    FROM public.gastos_operativos
    WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month';

    -- Saldo pendiente al cierre del mes
    SELECT COALESCE(saldo_pendiente_total, 0) INTO v_saldo_pendiente
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id AND fecha < p_mes + INTERVAL '1 month'
    ORDER BY fecha DESC LIMIT 1;

    -- Margen neto
    v_margen_neto_pct := CASE WHEN v_ventas_mes > 0
        THEN ROUND(((v_ventas_mes - v_costo_mes - v_gastos_mes) / v_ventas_mes) * 100, 1)
        ELSE 0 END;

    -- Ventas mes anterior para comparacion
    SELECT COALESCE(SUM(ventas_monto_total), 0) INTO v_ventas_mes_anterior
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= v_mes_anterior
      AND fecha < p_mes;

    RETURN jsonb_build_object(
        'ventas_mes', v_ventas_mes,
        'cobros_mes', v_cobros_mes,
        'costo_mes', v_costo_mes,
        'gastos_mes', v_gastos_mes,
        'saldo_pendiente', v_saldo_pendiente,
        'margen_neto_pct', v_margen_neto_pct,
        'ticket_promedio', v_ticket_promedio,
        'cantidad_ventas', v_cantidad_ventas,
        'ventas_mes_anterior', v_ventas_mes_anterior,
        'variacion_ventas_pct', CASE WHEN v_ventas_mes_anterior > 0
            THEN ROUND(((v_ventas_mes - v_ventas_mes_anterior) / v_ventas_mes_anterior) * 100, 1)
            ELSE NULL END
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;

-- ============================================================================
-- 2. rpc_deudores — debtor list with aging
-- ============================================================================

CREATE OR REPLACE FUNCTION public.rpc_deudores(
    p_optica_id TEXT
) RETURNS TABLE(
    paciente_nombre TEXT,
    paciente_telefono TEXT,
    venta_id TEXT,
    venta_fecha DATE,
    monto_total NUMERIC,
    total_pagado NUMERIC,
    saldo NUMERIC,
    dias_deuda INTEGER
)
LANGUAGE sql SECURITY INVOKER STABLE
AS $$
    SELECT
        COALESCE(p.nombre_completo, 'Sin paciente'),
        p.telefono,
        v.id,
        v.fecha,
        v.monto_total,
        COALESCE(SUM(pg.monto), 0) AS total_pagado,
        v.monto_total - COALESCE(SUM(pg.monto), 0) AS saldo,
        CURRENT_DATE - v.fecha AS dias_deuda
    FROM public.ventas v
    LEFT JOIN public.pacientes p ON p.id = v.paciente_id
    LEFT JOIN public.pagos pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
    GROUP BY v.id, v.fecha, v.monto_total, p.nombre_completo, p.telefono
    HAVING v.monto_total - COALESCE(SUM(pg.monto), 0) > 0.005
    ORDER BY dias_deuda DESC;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_deudores(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_deudores(TEXT) TO authenticated, service_role;

-- ============================================================================
-- 3/4. Fix: GRANT EXECUTE on recalcular_resumen_diario (missing from Fase 6)
-- ============================================================================

REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;

-- ============================================================================
-- 5. Rewrite rpc_count_pendientes to query ventas (canonical ledger)
--    Replaces old queries against dispensaciones + servicios_extra.
-- ============================================================================

CREATE OR REPLACE FUNCTION public.rpc_count_pendientes(p_optica_id text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_entregas integer;
    v_servicios integer;
BEGIN
    -- Overdue deliveries: ventas with estado 'Pendiente' whose date has passed
    SELECT COUNT(*)
    INTO v_entregas
    FROM public.ventas
    WHERE optica_id = p_optica_id
      AND estado = 'Pendiente'
      AND fecha < CURRENT_DATE;

    -- Unpaid balance: ventas (not anulado) with payments not covering the total
    SELECT COUNT(*)
    INTO v_servicios
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos
        WHERE optica_id = p_optica_id
        GROUP BY venta_id
    ) pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
      AND v.estado IS DISTINCT FROM 'Anulado'
      AND v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;

    RETURN jsonb_build_object(
        'entregas_pendientes', v_entregas,
        'servicios_pendientes', v_servicios
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_count_pendientes(text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_count_pendientes(text) TO authenticated, service_role;

-- ============================================================================
-- 6. Deprecation comments on rpc_resumen_financiero and rpc_saldo_pendiente
--    These functions remain callable for backward compatibility but new code
--    should use rpc_analisis_mensual instead.
-- ============================================================================

COMMENT ON FUNCTION public.rpc_resumen_financiero(text, date, date)
    IS 'DEPRECATED: Use rpc_analisis_mensual instead. This function remains for backward compatibility.';

COMMENT ON FUNCTION public.rpc_saldo_pendiente(text)
    IS 'DEPRECATED: Use rpc_analisis_mensual instead. This function remains for backward compatibility.';
