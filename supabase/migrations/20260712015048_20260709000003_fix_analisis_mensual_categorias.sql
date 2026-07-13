-- ============================================================================
-- Fix Bug 1 & Bug 2 in rpc_analisis_mensual
-- ============================================================================
-- Bug 1: margen_por_categoria shows all zeros because it LEFT JOINs an empty
--         table. Fix: inline CTE computes revenue from dispensaciones +
--         servicios_extra, mapping (tipo_lente, material_lente) to
--         categoria_producto_id via CASE expression.
--
-- Bug 2: stock_estancado uses low-stock filter (stock_actual<=stock_minimo),
--         hides most monturas, and hardcodes ultima_venta=NULL/dias_sin_venta=999.
--         Fix: remove low-stock filter, compute real sales dates from
--         montura_movimientos (SALIDA_VENTA) + dispensaciones.montura_id.
-- ============================================================================

CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(
    p_optica_id TEXT, p_mes DATE
) RETURNS jsonb
LANGUAGE plpgsql STABLE
SET search_path = public
AS $$
DECLARE
    v_ventas_mes NUMERIC; v_cobros_mes NUMERIC; v_costo_mes NUMERIC;
    v_gastos_mes NUMERIC; v_saldo_pendiente NUMERIC;
    v_margen_neto_pct NUMERIC; v_ticket_promedio NUMERIC;
    v_cantidad_ventas INTEGER; v_mes_anterior DATE;
    v_ventas_mes_anterior NUMERIC;
    v_margen_categoria jsonb; v_deudores_resumen jsonb;
    v_proyeccion jsonb; v_stock_estancado jsonb; v_valor_inventario NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';
    SELECT COALESCE(SUM(ventas_monto_total),0), COALESCE(SUM(cobros_monto_total),0),
           COALESCE(SUM(ventas_costo_total),0), COALESCE(SUM(ventas_cantidad),0)
    INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas
    FROM public.resumen_diario WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    v_ticket_promedio:=CASE WHEN v_cantidad_ventas>0 THEN v_ventas_mes/v_cantidad_ventas ELSE 0 END;
    SELECT COALESCE(SUM(monto),0) INTO v_gastos_mes FROM public.gastos_operativos
    WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    SELECT COALESCE(saldo_pendiente_total,0) INTO v_saldo_pendiente FROM public.resumen_diario
    WHERE optica_id=p_optica_id AND fecha<p_mes+INTERVAL'1 month' ORDER BY fecha DESC LIMIT 1;
    v_margen_neto_pct:=CASE WHEN v_ventas_mes>0 THEN ROUND(((v_ventas_mes-v_costo_mes-v_gastos_mes)/v_ventas_mes)*100,1) ELSE 0 END;
    SELECT COALESCE(SUM(ventas_monto_total),0) INTO v_ventas_mes_anterior FROM public.resumen_diario
    WHERE optica_id=p_optica_id AND fecha>=v_mes_anterior AND fecha<p_mes;

    -- ====================================================================
    -- FIXED: margen_por_categoria -- inline revenue from dispensaciones
    --        + servicios_extra, mapped via CASE expression
    -- ====================================================================
    WITH category_revenue AS (
        SELECT
            CASE
                WHEN d.tipo_lente = 'Progresivo' THEN 'lente_progresivo'
                WHEN d.tipo_lente = 'Bifocal' THEN 'lente_bifocal'
                WHEN d.tipo_lente = 'Monofocal' AND d.material_lente = 'Resina' THEN 'lente_monofocal'
                WHEN d.tipo_lente = 'Monofocal' THEN 'lente_otro'
                ELSE 'lente_otro'
            END AS categoria_producto_id,
            SUM(d.monto_total) AS ventas
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id
          AND d.fecha >= p_mes AND d.fecha < p_mes + INTERVAL '1 month'
        GROUP BY categoria_producto_id
        UNION ALL
        SELECT 'servicio_extra', SUM(se.monto_total)
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id
          AND se.fecha >= p_mes AND se.fecha < p_mes + INTERVAL '1 month'
    ),
    aggregated_revenue AS (
        SELECT categoria_producto_id, SUM(ventas) AS ventas
        FROM category_revenue GROUP BY categoria_producto_id
    )
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'categoria', cat.nombre,
        'ventas', COALESCE(ar.ventas, 0),
        'costos', 0,
        'margen_pct', null::numeric
    ) ORDER BY cat.orden), '[]'::jsonb)
    INTO v_margen_categoria
    FROM public.categorias_producto cat
    LEFT JOIN aggregated_revenue ar ON ar.categoria_producto_id = cat.id;

    SELECT jsonb_build_object('cantidad',COUNT(*),'saldo_total',COALESCE(SUM(saldo),0))
    INTO v_deudores_resumen FROM public.rpc_deudores(p_optica_id);

    -- Proyeccion caja: UNION source tables instead of ventas
    WITH pagos_dedup AS (
        SELECT
            COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match,
            pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id=p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulaci�n'
    ),
    all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
    )
    SELECT jsonb_build_object('ingresos_esperados',
        COALESCE(SUM(v.monto_total-COALESCE(pd_total.total_pagado,0)),0),
        'egresos_programados',COALESCE((SELECT SUM(monto) FROM public.gastos_operativos
        WHERE optica_id=p_optica_id AND fecha_programada>=CURRENT_DATE),0),'saldo_neto',0)
    INTO v_proyeccion FROM all_ventas v
    LEFT JOIN (SELECT venta_id_match,SUM(monto) AS total_pagado FROM pagos_dedup GROUP BY venta_id_match) pd_total
    ON pd_total.venta_id_match=v.venta_id
    WHERE v.monto_total-COALESCE(pd_total.total_pagado,0)>0.005;

    v_proyeccion:=jsonb_set(v_proyeccion,'{saldo_neto}',
        to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric,0)
               -COALESCE((v_proyeccion->>'egresos_programados')::numeric,0)));

    -- ====================================================================
    -- FIXED: stock_estancado -- real sales dates from montura_movimientos
    --        + dispensaciones; removed low-stock filter
    -- ====================================================================
    WITH ventas_montura AS (
        SELECT montura_id, MAX(fecha) AS ultima_venta
        FROM public.montura_movimientos
        WHERE optica_id = p_optica_id AND tipo = 'SALIDA_VENTA'
        GROUP BY montura_id
        UNION
        SELECT montura_id, MAX(fecha) AS ultima_venta
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id AND montura_id IS NOT NULL
        GROUP BY montura_id
    ),
    montura_venta_agg AS (
        SELECT montura_id, MAX(ultima_venta) AS ultima_venta
        FROM ventas_montura GROUP BY montura_id
    )
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'montura_id', m.id, 'sku', m.sku, 'modelo', m.modelo,
        'costo', COALESCE(m.costo, 0), 'stock_actual', m.stock_actual,
        'ultima_venta', mva.ultima_venta,
        'dias_sin_venta', CASE WHEN mva.ultima_venta IS NOT NULL
            THEN (CURRENT_DATE - mva.ultima_venta) ELSE 999 END
    ) ORDER BY CASE WHEN mva.ultima_venta IS NULL THEN 0 ELSE 1 END,
        mva.ultima_venta ASC NULLS LAST), '[]'::jsonb)
    INTO v_stock_estancado
    FROM public.monturas m
    LEFT JOIN montura_venta_agg mva ON mva.montura_id = m.id
    WHERE m.optica_id = p_optica_id AND m.activo = true AND m.stock_actual > 0;

    SELECT COALESCE(SUM(costo*stock_actual),0) INTO v_valor_inventario
    FROM public.monturas WHERE optica_id=p_optica_id AND activo=true;
    RETURN jsonb_build_object('ventas_mes',v_ventas_mes,'cobros_mes',v_cobros_mes,
        'costo_mes',v_costo_mes,'gastos_mes',v_gastos_mes,'saldo_pendiente',v_saldo_pendiente,
        'margen_neto_pct',v_margen_neto_pct,'ticket_promedio',v_ticket_promedio,
        'cantidad_ventas',v_cantidad_ventas,'ventas_mes_anterior',v_ventas_mes_anterior,
        'variacion_ventas_pct',CASE WHEN v_ventas_mes_anterior>0
        THEN ROUND(((v_ventas_mes-v_ventas_mes_anterior)/v_ventas_mes_anterior)*100,1) ELSE NULL END,
        'margen_por_categoria',v_margen_categoria,'deudores',v_deudores_resumen,
        'proyeccion_caja',v_proyeccion,'stock_estancado',v_stock_estancado,'valor_inventario',v_valor_inventario);
END;
$$;

-- ============================================================================
-- Re-grant permissions (same as previous migration)
-- ============================================================================

REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;;
