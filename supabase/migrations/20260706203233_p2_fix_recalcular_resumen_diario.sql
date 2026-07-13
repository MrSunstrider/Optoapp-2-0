CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql
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
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto_total), 0), COALESCE(SUM(costo_unitario_snapshot), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM public.ventas WHERE optica_id = p_optica_id AND fecha = p_fecha;

    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos WHERE optica_id = p_optica_id AND fecha = p_fecha;

    -- Saldo pendiente: usa venta_id directo (backfill P1 ya lo pobló) + fallback
    WITH pagos_dedup AS (
        SELECT DISTINCT ON (pg.id)
            CASE
                WHEN pg.venta_id IS NOT NULL THEN pg.venta_id
                WHEN pg.dispensacion_id IS NOT NULL THEN 'v_disp_' || pg.dispensacion_id
                WHEN pg.servicio_extra_id IS NOT NULL THEN 'v_serv_' || pg.servicio_extra_id
            END AS venta_id_match,
            pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_saldo_cantidad, v_saldo_total
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id_match, SUM(monto) AS total_pagado
        FROM pagos_dedup GROUP BY venta_id_match
    ) pd ON pd.venta_id_match = v.id
    WHERE v.optica_id = p_optica_id
      AND v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    SELECT COALESCE(SUM(costo * stock_actual), 0), COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas WHERE optica_id = p_optica_id;

    INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total, cobros_cantidad, cobros_monto_total, saldo_pendiente_total, saldo_pendiente_cantidad, inventario_valor, inventario_unidades)
    VALUES (p_optica_id, p_fecha, v_ventas_cantidad, v_ventas_monto, v_ventas_costo, v_cobros_cantidad, v_cobros_monto, v_saldo_total, v_saldo_cantidad, v_inv_valor, v_inv_unidades)
    ON CONFLICT (optica_id, fecha) DO UPDATE SET
        ventas_cantidad = EXCLUDED.ventas_cantidad, ventas_monto_total = EXCLUDED.ventas_monto_total, ventas_costo_total = EXCLUDED.ventas_costo_total,
        cobros_cantidad = EXCLUDED.cobros_cantidad, cobros_monto_total = EXCLUDED.cobros_monto_total,
        saldo_pendiente_total = EXCLUDED.saldo_pendiente_total, saldo_pendiente_cantidad = EXCLUDED.saldo_pendiente_cantidad,
        inventario_valor = EXCLUDED.inventario_valor, inventario_unidades = EXCLUDED.inventario_unidades,
        calculado_en = now();
END;
$$;

REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;;
