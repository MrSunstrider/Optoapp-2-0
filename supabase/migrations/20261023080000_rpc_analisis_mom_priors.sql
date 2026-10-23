-- MoM priors for volumen/ticket/costo drivers: prior-month qty + COGS + ticket alongside ventas.
-- Body based on 20261018070000_rpc_analisis_exclude_linked_biselado_gastos.sql.

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
    v_cantidad_ventas_mes_anterior INTEGER;
    v_costo_mes_anterior NUMERIC;
    v_ticket_promedio_mes_anterior NUMERIC;
    v_meses_historicos INTEGER;
    v_margen_categoria jsonb;
    v_deudores_resumen jsonb;
    v_proyeccion jsonb;
    v_stock_estancado jsonb;
    v_valor_inventario NUMERIC;
    v_egresos_programados NUMERIC;
    v_costos_por_linea jsonb;
    v_margen_servicios jsonb;
    v_cobertura jsonb;
    v_gastos_por_categoria jsonb;
    v_gastos_fijos NUMERIC;
    v_gastos_variables NUMERIC;
    v_margen_bruto NUMERIC;
    v_margen_contribucion NUMERIC;
    v_margen_contribucion_pct NUMERIC;
    v_punto_equilibrio NUMERIC;
    v_margen_neto_objetivo NUMERIC;
    v_serie_6m jsonb;
BEGIN
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    IF NOT app_private.has_optica_role(auth.uid(), p_optica_id, ARRAY['admin', 'gerente']) THEN
        RAISE EXCEPTION 'BI access requires admin or gerente role';
    END IF;

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
    WHERE optica_id = p_optica_id
      AND fecha >= p_mes
      AND fecha < p_mes + INTERVAL '1 month'
      AND origen_dispensacion_item_id IS NULL;

    SELECT COALESCE(saldo_pendiente_total, 0) INTO v_saldo_pendiente
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha < p_mes + INTERVAL '1 month'
    ORDER BY fecha DESC LIMIT 1;

    v_margen_neto_pct := CASE WHEN v_ventas_mes > 0
        THEN ROUND(((v_ventas_mes - v_costo_mes - v_gastos_mes) / v_ventas_mes) * 100, 1)
        ELSE 0 END;

    SELECT COALESCE(SUM(ventas_monto_total), 0),
           COALESCE(SUM(ventas_cantidad), 0),
           COALESCE(SUM(ventas_costo_total), 0)
    INTO v_ventas_mes_anterior, v_cantidad_ventas_mes_anterior, v_costo_mes_anterior
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= v_mes_anterior
      AND fecha < p_mes;

    v_ticket_promedio_mes_anterior := CASE WHEN v_cantidad_ventas_mes_anterior > 0
        THEN v_ventas_mes_anterior / v_cantidad_ventas_mes_anterior ELSE 0 END;

    SELECT COALESCE(COUNT(DISTINCT DATE_TRUNC('month', fecha)), 0)
    INTO v_meses_historicos
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id;

    -- Same exclusions as recalcular_resumen_diario (Anulado / Reclamada), so the per-line and
    -- per-category costs converge with costo_mes. Category mapping is that of 20260709000003.
    -- The legacy margen_por_categoria table is no longer read: nothing writes to it.
    WITH ot AS (
        SELECT d.id,
               CASE
                   WHEN d.tipo_lente = 'Progresivo' THEN 'lente_progresivo'
                   WHEN d.tipo_lente = 'Bifocal' THEN 'lente_bifocal'
                   WHEN d.tipo_lente = 'Monofocal' AND d.material_lente = 'Resina' THEN 'lente_monofocal'
                   ELSE 'lente_otro'
               END AS categoria_id,
               d.monto_total AS ventas
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id
          AND d.fecha >= p_mes AND d.fecha < p_mes + INTERVAL '1 month'
          AND d.estado_entrega IS DISTINCT FROM 'Anulado'
          AND d.estado_entrega IS DISTINCT FROM 'Reclamada'
    ),
    ot_item AS (
        SELECT di.dispensacion_id AS ot_id,
               COALESCE(di.costo_real_od, 0) + COALESCE(di.costo_real_oi, 0) AS lentes,
               COALESCE(di.costo_real_montura, 0) AS montura,
               COALESCE(di.costo_real_lc, 0) AS lc,
               COALESCE(di.costo_real_biselado, 0) AS biselado,
               di.costo_origen
        FROM public.dispensacion_items di
        JOIN ot ON ot.id = di.dispensacion_id
    ),
    ot_costo AS (
        SELECT ot.categoria_id, ot.ventas,
               COALESCE(SUM(oi.lentes), 0) AS lentes,
               COALESCE(SUM(oi.montura), 0) AS montura,
               COALESCE(SUM(oi.lc), 0) AS lc,
               COALESCE(SUM(oi.biselado), 0) AS biselado,
               COALESCE(MAX(rg.regalo), 0) AS regalo
        FROM ot
        LEFT JOIN ot_item oi ON oi.ot_id = ot.id
        LEFT JOIN LATERAL (
            SELECT SUM(rd.cantidad * rd.costo_unitario::numeric) AS regalo
            FROM public.regalos_dispensacion rd
            WHERE rd.dispensacion_id = ot.id
        ) rg ON true
        GROUP BY ot.id, ot.categoria_id, ot.ventas
    ),
    sv AS (
        SELECT se.id, se.monto_total AS ventas, se.realizado_por, se.servicio_tipo_id, se.costo_real,
               CASE se.realizado_por
                   WHEN 'propio' THEN 'propios'
                   WHEN 'tercero' THEN 'tercerizados'
                   ELSE 'sin_clasificar'
               END AS bucket
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id
          AND se.fecha >= p_mes AND se.fecha < p_mes + INTERVAL '1 month'
          AND se.estado IS DISTINCT FROM 'Anulado'
    ),
    sv_costo AS (
        SELECT sv.bucket, sv.ventas,
               sv.costo_real + COALESCE((
                   SELECT SUM(sei.costo_real) FROM public.servicio_extra_items sei
                   WHERE sei.servicio_extra_id = sv.id), 0) AS costo_servicio,
               COALESCE((
                   SELECT SUM(rse.cantidad * rse.costo_unitario) FROM public.regalos_servicio_extra rse
                   WHERE rse.servicio_extra_id = sv.id), 0) AS regalo
        FROM sv
    ),
    cat_agg AS (
        SELECT x.categoria_id, SUM(x.ventas) AS ventas, SUM(x.costo) AS costo
        FROM (
            SELECT oc.categoria_id, oc.ventas, oc.lentes + oc.montura + oc.lc + oc.biselado + oc.regalo AS costo
            FROM ot_costo oc
            UNION ALL
            SELECT 'servicio_extra', sc.ventas, sc.costo_servicio + sc.regalo
            FROM sv_costo sc
        ) x
        GROUP BY x.categoria_id
    ),
    sv_bucket AS (
        SELECT sc.bucket, COUNT(*) AS cantidad, SUM(sc.ventas) AS ventas, SUM(sc.costo_servicio) AS costo
        FROM sv_costo sc
        GROUP BY sc.bucket
    ),
    linea_costo AS (
        SELECT COALESCE(SUM(oc.lentes), 0) AS lentes,
               COALESCE(SUM(oc.montura), 0) AS montura,
               COALESCE(SUM(oc.lc), 0) AS lc,
               COALESCE(SUM(oc.biselado), 0) AS biselado,
               COALESCE(SUM(oc.regalo), 0) + COALESCE((SELECT SUM(sc.regalo) FROM sv_costo sc), 0) AS regalos
        FROM ot_costo oc
    ),
    clase AS (
        SELECT app_private.cobertura_clase_item(
                   oi.costo_origen, oi.lentes + oi.montura + oi.lc + oi.biselado) AS clase
        FROM ot_item oi
        UNION ALL
        SELECT app_private.cobertura_clase_servicio(sv.realizado_por, sv.costo_real, sv.servicio_tipo_id)
        FROM sv
    ),
    cobertura AS (
        SELECT COUNT(*) AS total,
               COUNT(*) FILTER (WHERE clase = 'real') AS reales,
               COUNT(*) FILTER (WHERE clase = 'estimado') AS estimadas,
               COUNT(*) FILTER (WHERE clase = 'faltante') AS faltantes
        FROM clase
    )
    SELECT
        (SELECT COALESCE(jsonb_agg(jsonb_build_object(
                    'categoria', cat.nombre,
                    'ventas', ROUND(COALESCE(ca.ventas, 0), 2),
                    'costos', ROUND(COALESCE(ca.costo, 0), 2),
                    'margen', ROUND(COALESCE(ca.ventas, 0) - COALESCE(ca.costo, 0), 2),
                    'margen_pct', CASE WHEN COALESCE(ca.ventas, 0) > 0
                        THEN ROUND((ca.ventas - ca.costo) / ca.ventas * 100, 2) END
                ) ORDER BY cat.orden), '[]'::jsonb)
         FROM public.categorias_producto cat
         LEFT JOIN cat_agg ca ON ca.categoria_id = cat.id),
        (SELECT jsonb_build_object(
                    'lentes', ROUND(lco.lentes, 2),
                    'montura', ROUND(lco.montura, 2),
                    'lc', ROUND(lco.lc, 2),
                    'biselado', ROUND(lco.biselado, 2),
                    'servicios_propios', ROUND(COALESCE((
                        SELECT b.costo FROM sv_bucket b WHERE b.bucket = 'propios'), 0), 2),
                    'servicios_tercerizados', ROUND(COALESCE((
                        SELECT b.costo FROM sv_bucket b WHERE b.bucket = 'tercerizados'), 0), 2),
                    'servicios_sin_clasificar', ROUND(COALESCE((
                        SELECT b.costo FROM sv_bucket b WHERE b.bucket = 'sin_clasificar'), 0), 2),
                    'regalos', ROUND(lco.regalos, 2))
         FROM linea_costo lco),
        (SELECT jsonb_object_agg(bk.bucket, jsonb_build_object(
                    'ventas', ROUND(COALESCE(sb.ventas, 0), 2),
                    'costos', ROUND(COALESCE(sb.costo, 0), 2),
                    'margen', ROUND(COALESCE(sb.ventas, 0) - COALESCE(sb.costo, 0), 2),
                    'margen_pct', CASE WHEN COALESCE(sb.ventas, 0) > 0
                        THEN ROUND((sb.ventas - sb.costo) / sb.ventas * 100, 2) END,
                    'cantidad', COALESCE(sb.cantidad, 0)))
         FROM (VALUES ('propios'), ('tercerizados'), ('sin_clasificar')) AS bk(bucket)
         LEFT JOIN sv_bucket sb ON sb.bucket = bk.bucket),
        (SELECT jsonb_build_object(
                    'lineas_total', cb.total,
                    'lineas_reales', cb.reales,
                    'lineas_estimadas', cb.estimadas,
                    'lineas_faltantes', cb.faltantes,
                    'pct_real', CASE WHEN cb.total > 0 THEN ROUND(cb.reales::numeric / cb.total * 100, 2) ELSE 0 END,
                    'pct_estimado', CASE WHEN cb.total > 0 THEN ROUND(cb.estimadas::numeric / cb.total * 100, 2) ELSE 0 END,
                    'pct_faltante', CASE WHEN cb.total > 0 THEN ROUND(cb.faltantes::numeric / cb.total * 100, 2) ELSE 0 END)
         FROM cobertura cb)
    INTO v_margen_categoria, v_costos_por_linea, v_margen_servicios, v_cobertura;

    SELECT jsonb_build_object(
        'cantidad', COUNT(*),
        'saldo_total', COALESCE(SUM(saldo), 0)
    ) INTO v_deudores_resumen
    FROM public.rpc_deudores(p_optica_id);

    SELECT COALESCE(SUM(monto), 0) INTO v_egresos_programados
    FROM public.gastos_operativos
    WHERE optica_id = p_optica_id
      AND fecha_programada >= CURRENT_DATE
      AND origen_dispensacion_item_id IS NULL;

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
    SELECT jsonb_build_object(
        'ingresos_esperados',
            COALESCE(SUM(v.monto_total - COALESCE(pd_total.total_pagado, 0)), 0),
        'egresos_programados', v_egresos_programados,
        'saldo_neto', 0
    ) INTO v_proyeccion
    FROM all_ventas v
    LEFT JOIN (
        SELECT venta_id_match, SUM(efecto) AS total_pagado
        FROM pagos_dedup
        GROUP BY venta_id_match
    ) pd_total ON pd_total.venta_id_match = v.venta_id
    WHERE v.monto_total - COALESCE(pd_total.total_pagado, 0) > 0.005;

    -- Zero unpaid rows leaves SELECT INTO NULL; keep scheduled egresos.
    IF v_proyeccion IS NULL THEN
        v_proyeccion := jsonb_build_object(
            'ingresos_esperados', 0,
            'egresos_programados', v_egresos_programados,
            'saldo_neto', 0
        );
    END IF;

    v_proyeccion := jsonb_set(v_proyeccion, '{saldo_neto}',
        to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric, 0)
               - COALESCE((v_proyeccion->>'egresos_programados')::numeric, 0)));

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

    SELECT COALESCE(SUM(costo * stock_actual), 0) INTO v_valor_inventario
    FROM public.monturas
    WHERE optica_id = p_optica_id AND activo = true;

    -- Totals use every manual gasto row so gastos_fijos + variables always equals gastos_mes; only the
    -- per-category list drops non-positive categories. Linked OT bevel gastos are excluded.
    WITH g AS (
        SELECT gx.categoria, gx.monto,
               app_private.gasto_tipo_efectivo(gx.tipo_gasto, gx.categoria) AS tipo
        FROM public.gastos_operativos gx
        WHERE gx.optica_id = p_optica_id
          AND gx.fecha >= p_mes AND gx.fecha < p_mes + INTERVAL '1 month'
          AND gx.origen_dispensacion_item_id IS NULL
    ),
    por_categoria AS (
        SELECT g.categoria,
               SUM(g.monto) AS monto,
               COALESCE(SUM(g.monto) FILTER (WHERE g.tipo = 'fijo'), 0) AS fijo,
               COALESCE(SUM(g.monto) FILTER (WHERE g.tipo = 'variable'), 0) AS variable
        FROM g
        GROUP BY g.categoria
    )
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
                'categoria', pc.categoria,
                'monto', ROUND(pc.monto, 2),
                'tipo_predominante', CASE WHEN pc.fijo >= pc.variable THEN 'fijo' ELSE 'variable' END
           ) ORDER BY pc.monto DESC, pc.categoria) FILTER (WHERE pc.monto > 0), '[]'::jsonb),
           COALESCE(SUM(pc.fijo), 0),
           COALESCE(SUM(pc.variable), 0)
    INTO v_gastos_por_categoria, v_gastos_fijos, v_gastos_variables
    FROM por_categoria pc;

    v_margen_bruto := v_ventas_mes - v_costo_mes;
    v_margen_contribucion := v_ventas_mes - v_costo_mes - v_gastos_variables;
    v_margen_contribucion_pct := CASE WHEN v_ventas_mes > 0
        THEN v_margen_contribucion / v_ventas_mes * 100 ELSE 0 END;
    v_punto_equilibrio := CASE WHEN v_margen_contribucion_pct > 0
        THEN ROUND(v_gastos_fijos / (v_margen_contribucion_pct / 100), 2) END;

    SELECT COALESCE(
        (SELECT cf.margen_neto_objetivo FROM public.configuracion_financiera cf
         WHERE cf.optica_id = p_optica_id),
        15.0)
    INTO v_margen_neto_objetivo;

    WITH meses AS (
        SELECT (date_trunc('month', p_mes::timestamp) - make_interval(months => n))::date AS mes
        FROM generate_series(0, 5) AS n
    ),
    ventas AS (
        SELECT date_trunc('month', rd.fecha::timestamp)::date AS mes,
               SUM(rd.ventas_monto_total) AS ventas,
               SUM(rd.ventas_costo_total) AS costo
        FROM public.resumen_diario rd
        WHERE rd.optica_id = p_optica_id
          AND rd.fecha >= (SELECT MIN(mes) FROM meses)
          AND rd.fecha < (SELECT MAX(mes) FROM meses) + INTERVAL '1 month'
        GROUP BY 1
    ),
    gastos AS (
        SELECT date_trunc('month', gx.fecha::timestamp)::date AS mes, SUM(gx.monto) AS gastos
        FROM public.gastos_operativos gx
        WHERE gx.optica_id = p_optica_id
          AND gx.fecha >= (SELECT MIN(mes) FROM meses)
          AND gx.fecha < (SELECT MAX(mes) FROM meses) + INTERVAL '1 month'
          AND gx.origen_dispensacion_item_id IS NULL
        GROUP BY 1
    )
    SELECT jsonb_agg(jsonb_build_object(
               'mes', to_char(m.mes, 'YYYY-MM'),
               'ventas', ROUND(COALESCE(v.ventas, 0), 2),
               'costo', ROUND(COALESCE(v.costo, 0), 2),
               'gastos', ROUND(COALESCE(g.gastos, 0), 2),
               'margen', ROUND(COALESCE(v.ventas, 0) - COALESCE(v.costo, 0) - COALESCE(g.gastos, 0), 2),
               'margen_neto_pct', CASE WHEN COALESCE(v.ventas, 0) > 0
                   THEN ROUND((v.ventas - COALESCE(v.costo, 0) - COALESCE(g.gastos, 0)) / v.ventas * 100, 2)
                   ELSE 0 END
           ) ORDER BY m.mes)
    INTO v_serie_6m
    FROM meses m
    LEFT JOIN ventas v ON v.mes = m.mes
    LEFT JOIN gastos g ON g.mes = m.mes;

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
        'cantidad_ventas_mes_anterior', v_cantidad_ventas_mes_anterior,
        'costo_mes_anterior', v_costo_mes_anterior,
        'ticket_promedio_mes_anterior', v_ticket_promedio_mes_anterior,
        'variacion_ventas_pct', CASE WHEN v_ventas_mes_anterior > 0
            THEN ROUND(((v_ventas_mes - v_ventas_mes_anterior) / v_ventas_mes_anterior) * 100, 1)
            ELSE NULL END,
        'meses_historicos', v_meses_historicos,
        'margen_por_categoria', v_margen_categoria,
        'deudores', v_deudores_resumen,
        'proyeccion_caja', v_proyeccion,
        'stock_estancado', v_stock_estancado,
        'valor_inventario', v_valor_inventario,
        'margen_bruto', ROUND(v_margen_bruto, 2),
        'margen_bruto_pct', CASE WHEN v_ventas_mes > 0
            THEN ROUND(v_margen_bruto / v_ventas_mes * 100, 2) ELSE 0 END,
        'costos_por_linea', v_costos_por_linea,
        'margen_servicios', v_margen_servicios,
        'cobertura', v_cobertura,
        'gastos_por_categoria', v_gastos_por_categoria,
        'gastos_por_tipo', jsonb_build_object(
            'fijo', ROUND(v_gastos_fijos, 2),
            'variable', ROUND(v_gastos_variables, 2)),
        'gastos_fijos', ROUND(v_gastos_fijos, 2),
        'margen_contribucion', ROUND(v_margen_contribucion, 2),
        'margen_contribucion_pct', ROUND(v_margen_contribucion_pct, 2),
        'punto_equilibrio', v_punto_equilibrio,
        'margen_neto_objetivo', ROUND(v_margen_neto_objetivo, 2),
        'brecha_margen_neto_pp', ROUND(v_margen_neto_pct - v_margen_neto_objetivo, 2),
        'serie_6m', v_serie_6m
    );
END;
$$;

COMMENT ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) IS
  'Monthly BI JSON. MoM priors include qty/costo/ticket. Excludes OT-linked biselado gastos from gasto aggregates.';

REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;
