
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

REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;
;
