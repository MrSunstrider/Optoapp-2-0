-- ============================================================================
-- Migration: Fix rpc_analisis_mensual — restore 16-field rich version
--
-- Merges the July 10 UNION ALL architecture (15 fields) with the July 13
-- meses_historicos addition. No references to the dropped ventas table.
--
-- Fields restored from July 10:
--   margen_por_categoria, deudores, proyeccion_caja,
--   stock_estancado, valor_inventario
--
-- Fields preserved from July 13:
--   meses_historicos (COUNT DISTINCT months from resumen_diario)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(
    p_optica_id TEXT,
    p_mes DATE
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
    v_meses_historicos INTEGER;
    v_margen_categoria jsonb;
    v_deudores_resumen jsonb;
    v_proyeccion jsonb;
    v_stock_estancado jsonb;
    v_valor_inventario NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';

    -- Core monthly indicators from resumen_diario
    SELECT COALESCE(SUM(ventas_monto_total), 0),
           COALESCE(SUM(cobros_monto_total), 0),
           COALESCE(SUM(ventas_costo_total), 0),
           COALESCE(SUM(ventas_cantidad), 0)
    INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= p_mes
      AND fecha < p_mes + INTERVAL '1 month';

    v_ticket_promedio := CASE WHEN v_cantidad_ventas > 0
        THEN v_ventas_mes / v_cantidad_ventas ELSE 0 END;

    -- Monthly expenses
    SELECT COALESCE(SUM(monto), 0) INTO v_gastos_mes
    FROM public.gastos_operativos
    WHERE optica_id = p_optica_id
      AND fecha >= p_mes
      AND fecha < p_mes + INTERVAL '1 month';

    -- Ending pending balance
    SELECT COALESCE(saldo_pendiente_total, 0) INTO v_saldo_pendiente
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha < p_mes + INTERVAL '1 month'
    ORDER BY fecha DESC LIMIT 1;

    -- Net margin percentage
    v_margen_neto_pct := CASE WHEN v_ventas_mes > 0
        THEN ROUND(((v_ventas_mes - v_costo_mes - v_gastos_mes) / v_ventas_mes) * 100, 1)
        ELSE 0 END;

    -- Previous month sales for comparison
    SELECT COALESCE(SUM(ventas_monto_total), 0) INTO v_ventas_mes_anterior
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= v_mes_anterior
      AND fecha < p_mes;

    -- Months with historical data (preserved from July 13)
    SELECT COALESCE(COUNT(DISTINCT DATE_TRUNC('month', fecha)), 0)
    INTO v_meses_historicos
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id;

    -- Margin by category (restored from July 10)
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'categoria', cat.nombre,
        'ventas', COALESCE(mc.ventas_totales, 0),
        'costos', COALESCE(mc.costo_total, 0),
        'margen_pct', COALESCE(mc.margen_porcentaje, 0)
    )), '[]'::jsonb) INTO v_margen_categoria
    FROM public.categorias_producto cat
    LEFT JOIN public.margen_por_categoria mc
        ON mc.categoria_producto_id = cat.id
        AND mc.optica_id = p_optica_id
        AND mc.periodo = to_char(p_mes, 'YYYY-MM')
        AND mc.tipo_periodo = 'mensual';

    -- Debtor summary (restored from July 10)
    SELECT jsonb_build_object(
        'cantidad', COUNT(*),
        'saldo_total', COALESCE(SUM(saldo), 0)
    ) INTO v_deudores_resumen
    FROM public.rpc_deudores(p_optica_id);

    -- Cash flow projection (restored from July 10, UNION ALL — no ventas)
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
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
    )
    SELECT jsonb_build_object(
        'ingresos_esperados',
            COALESCE(SUM(v.monto_total - COALESCE(pd_total.total_pagado, 0)), 0),
        'egresos_programados',
            COALESCE((SELECT SUM(monto) FROM public.gastos_operativos
                      WHERE optica_id = p_optica_id AND fecha_programada >= CURRENT_DATE), 0),
        'saldo_neto', 0
    ) INTO v_proyeccion
    FROM all_ventas v
    LEFT JOIN (
        SELECT venta_id_match, SUM(monto) AS total_pagado
        FROM pagos_dedup
        GROUP BY venta_id_match
    ) pd_total ON pd_total.venta_id_match = v.venta_id
    WHERE v.monto_total - COALESCE(pd_total.total_pagado, 0) > 0.005;

    v_proyeccion := jsonb_set(v_proyeccion, '{saldo_neto}',
        to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric, 0)
               - COALESCE((v_proyeccion->>'egresos_programados')::numeric, 0)));

    -- Stagnant stock (restored from July 10)
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'montura_id', m.id, 'sku', m.sku, 'modelo', m.modelo,
        'costo', COALESCE(m.costo, 0),
        'stock_actual', m.stock_actual,
        'ultima_venta', NULL,
        'dias_sin_venta', 999
    )), '[]'::jsonb) INTO v_stock_estancado
    FROM public.monturas m
    WHERE m.optica_id = p_optica_id
      AND m.activo = true
      AND m.stock_actual <= m.stock_minimo
      AND m.stock_actual > 0;

    -- Inventory value (restored from July 10)
    SELECT COALESCE(SUM(costo * stock_actual), 0) INTO v_valor_inventario
    FROM public.monturas
    WHERE optica_id = p_optica_id AND activo = true;

    -- Return full 16-field JSON (15 restored + meses_historicos preserved)
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
            ELSE NULL END,
        'meses_historicos', v_meses_historicos,
        'margen_por_categoria', v_margen_categoria,
        'deudores', v_deudores_resumen,
        'proyeccion_caja', v_proyeccion,
        'stock_estancado', v_stock_estancado,
        'valor_inventario', v_valor_inventario
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;
