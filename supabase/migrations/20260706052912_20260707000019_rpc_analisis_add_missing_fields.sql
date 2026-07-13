
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
    v_margen_categoria jsonb;
    v_deudores_resumen jsonb;
    v_proyeccion jsonb;
    v_stock_estancado jsonb;
    v_valor_inventario NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';

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

    SELECT COALESCE(SUM(monto), 0) INTO v_gastos_mes
    FROM public.gastos_operativos
    WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month';

    SELECT COALESCE(saldo_pendiente_total, 0) INTO v_saldo_pendiente
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id AND fecha < p_mes + INTERVAL '1 month'
    ORDER BY fecha DESC LIMIT 1;

    v_margen_neto_pct := CASE WHEN v_ventas_mes > 0
        THEN ROUND(((v_ventas_mes - v_costo_mes - v_gastos_mes) / v_ventas_mes) * 100, 1)
        ELSE 0 END;

    SELECT COALESCE(SUM(ventas_monto_total), 0) INTO v_ventas_mes_anterior
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= v_mes_anterior
      AND fecha < p_mes;

    -- margen_por_categoria
    SELECT COALESCE(jsonb_agg(
        jsonb_build_object(
            'categoria', cat.nombre,
            'ventas', COALESCE(mc.ventas_totales, 0),
            'costos', COALESCE(mc.costo_total, 0),
            'margen_pct', COALESCE(mc.margen_porcentaje, 0)
        )
    ), '[]'::jsonb) INTO v_margen_categoria
    FROM public.categorias_producto cat
    LEFT JOIN public.margen_por_categoria mc
      ON mc.categoria_producto_id = cat.id
      AND mc.optica_id = p_optica_id
      AND mc.periodo = to_char(p_mes, 'YYYY-MM')
      AND mc.tipo_periodo = 'mensual';

    -- deudores (resumen)
    SELECT jsonb_build_object(
        'cantidad', COUNT(*),
        'saldo_total', COALESCE(SUM(saldo), 0)
    ) INTO v_deudores_resumen
    FROM public.rpc_deudores(p_optica_id);

    -- proyeccion_caja (simple: cuentas por cobrar - gastos programados)
    SELECT jsonb_build_object(
        'ingresos_esperados', COALESCE(SUM(v.monto_total - COALESCE(pg.total_pagado, 0)), 0),
        'egresos_programados', COALESCE((
            SELECT SUM(monto) FROM public.gastos_operativos
            WHERE optica_id = p_optica_id AND fecha_programada >= CURRENT_DATE
        ), 0),
        'saldo_neto', 0
    ) INTO v_proyeccion
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos WHERE optica_id = p_optica_id GROUP BY venta_id
    ) pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
      AND v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;

    -- saldo_neto = ingresos - egresos
    v_proyeccion := jsonb_set(v_proyeccion, '{saldo_neto}',
        to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric, 0)
               - COALESCE((v_proyeccion->>'egresos_programados')::numeric, 0)));

    -- stock_estancado
    SELECT COALESCE(jsonb_agg(
        jsonb_build_object(
            'montura_id', m.id,
            'sku', m.sku,
            'modelo', m.modelo,
            'costo', COALESCE(m.costo, 0),
            'stock_actual', m.stock_actual,
            'ultima_venta', NULL,
            'dias_sin_venta', 999
        )
    ), '[]'::jsonb) INTO v_stock_estancado
    FROM public.monturas m
    WHERE m.optica_id = p_optica_id
      AND m.activo = true
      AND m.stock_actual <= m.stock_minimo
      AND m.stock_actual > 0;

    -- valor_inventario
    SELECT COALESCE(SUM(costo * stock_actual), 0) INTO v_valor_inventario
    FROM public.monturas WHERE optica_id = p_optica_id AND activo = true;

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
;
