-- 2.1: DROP triggers
DROP TRIGGER IF EXISTS trg_dispensacion_to_venta ON public.dispensaciones;
DROP FUNCTION IF EXISTS public.fn_upsert_venta_from_dispensacion();
DROP TRIGGER IF EXISTS trg_servicio_to_venta ON public.servicios_extra;
DROP FUNCTION IF EXISTS public.fn_upsert_venta_from_servicio_extra();

-- 2.6: DROP deprecated
DROP FUNCTION IF EXISTS public.rpc_resumen_financiero(TEXT, DATE, DATE);

-- 2.2: REWRITE recalcular_resumen_diario
CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(p_optica_id TEXT, p_fecha DATE) RETURNS void LANGUAGE plpgsql SET search_path = public AS $$
DECLARE
    v_ventas_cantidad INTEGER; v_ventas_monto NUMERIC; v_ventas_costo NUMERIC;
    v_cobros_cantidad INTEGER; v_cobros_monto NUMERIC;
    v_saldo_total NUMERIC; v_saldo_cantidad INTEGER;
    v_inv_valor NUMERIC; v_inv_unidades INTEGER;
BEGIN
    WITH daily_ventas AS (
        SELECT monto_total, 0::numeric AS costo_unitario_snapshot FROM public.dispensaciones WHERE optica_id = p_optica_id AND fecha = p_fecha
        UNION ALL
        SELECT monto_total, 0::numeric AS costo_unitario_snapshot FROM public.servicios_extra WHERE optica_id = p_optica_id AND fecha = p_fecha
    )
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(monto_total),0), COALESCE(SUM(costo_unitario_snapshot),0) INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo FROM daily_ventas;
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(monto),0) INTO v_cobros_cantidad, v_cobros_monto FROM public.pagos WHERE optica_id=p_optica_id AND fecha=p_fecha AND tipo IS DISTINCT FROM 'Anulación';
    WITH pagos_dedup AS (
        SELECT COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match, pg.monto FROM public.pagos pg WHERE pg.optica_id=p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'
    ), all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total FROM public.dispensaciones WHERE optica_id = p_optica_id
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total FROM public.servicios_extra WHERE optica_id = p_optica_id
    )
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(v.monto_total-COALESCE(pd.total_pagado,0)),0) INTO v_saldo_cantidad, v_saldo_total
    FROM all_ventas v LEFT JOIN (SELECT venta_id_match,SUM(monto) AS total_pagado FROM pagos_dedup GROUP BY venta_id_match) pd ON pd.venta_id_match=v.venta_id
    WHERE v.monto_total-COALESCE(pd.total_pagado,0)>0.005;
    SELECT COALESCE(SUM(costo*stock_actual),0), COALESCE(SUM(stock_actual),0) INTO v_inv_valor, v_inv_unidades FROM public.monturas WHERE optica_id=p_optica_id;
    INSERT INTO public.resumen_diario (optica_id,fecha,ventas_cantidad,ventas_monto_total,ventas_costo_total,cobros_cantidad,cobros_monto_total,saldo_pendiente_total,saldo_pendiente_cantidad,inventario_valor,inventario_unidades)
    VALUES (p_optica_id,p_fecha,v_ventas_cantidad,v_ventas_monto,v_ventas_costo,v_cobros_cantidad,v_cobros_monto,v_saldo_total,v_saldo_cantidad,v_inv_valor,v_inv_unidades)
    ON CONFLICT (optica_id,fecha) DO UPDATE SET ventas_cantidad=EXCLUDED.ventas_cantidad, ventas_monto_total=EXCLUDED.ventas_monto_total, ventas_costo_total=EXCLUDED.ventas_costo_total, cobros_cantidad=EXCLUDED.cobros_cantidad, cobros_monto_total=EXCLUDED.cobros_monto_total, saldo_pendiente_total=EXCLUDED.saldo_pendiente_total, saldo_pendiente_cantidad=EXCLUDED.saldo_pendiente_cantidad, inventario_valor=EXCLUDED.inventario_valor, inventario_unidades=EXCLUDED.inventario_unidades, calculado_en=now();
END; $$;

-- 2.3: REWRITE rpc_deudores
DROP FUNCTION IF EXISTS public.rpc_deudores(TEXT);
CREATE OR REPLACE FUNCTION public.rpc_deudores(p_optica_id TEXT) RETURNS TABLE(paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE, monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER, paciente_id TEXT) LANGUAGE plpgsql STABLE SET search_path = public AS $$
BEGIN RETURN QUERY
WITH pagos_dedup AS (SELECT COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match, pg.monto FROM public.pagos pg WHERE pg.optica_id = p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'),
all_ventas AS (SELECT 'v_disp_' || d.id AS venta_id, d.paciente_id, d.fecha, d.monto_total FROM public.dispensaciones d WHERE d.optica_id = p_optica_id UNION ALL SELECT 'v_serv_' || se.id AS venta_id, se.paciente_id, se.fecha, se.monto_total FROM public.servicios_extra se WHERE se.optica_id = p_optica_id)
SELECT COALESCE(p.nombre_completo, 'Sin paciente'), p.telefono, v.venta_id, v.fecha, v.monto_total, COALESCE(SUM(pd.monto), 0) AS total_pagado, v.monto_total - COALESCE(SUM(pd.monto), 0) AS saldo, CURRENT_DATE - v.fecha AS dias_deuda, v.paciente_id
FROM all_ventas v LEFT JOIN public.pacientes p ON p.id = v.paciente_id LEFT JOIN pagos_dedup pd ON pd.venta_id_match = v.venta_id
GROUP BY v.venta_id, v.fecha, v.monto_total, p.nombre_completo, p.telefono, v.paciente_id
HAVING v.monto_total - COALESCE(SUM(pd.monto), 0) > 0.005 ORDER BY dias_deuda DESC;
END; $$;

-- 2.4: REWRITE rpc_count_pendientes
CREATE OR REPLACE FUNCTION public.rpc_count_pendientes(p_optica_id text) RETURNS jsonb LANGUAGE plpgsql SECURITY INVOKER SET search_path = public AS $$
DECLARE v_entregas integer; v_servicios integer;
BEGIN
    WITH pendientes AS (SELECT 1 FROM public.dispensaciones WHERE optica_id = p_optica_id AND estado_entrega = 'Pendiente' AND fecha < CURRENT_DATE UNION ALL SELECT 1 FROM public.servicios_extra WHERE optica_id = p_optica_id AND estado = 'Pendiente' AND fecha < CURRENT_DATE)
    SELECT COUNT(*) INTO v_entregas FROM pendientes;
    WITH all_ventas AS (SELECT 'v_disp_' || id AS venta_id, monto_total FROM public.dispensaciones WHERE optica_id = p_optica_id UNION ALL SELECT 'v_serv_' || id AS venta_id, monto_total FROM public.servicios_extra WHERE optica_id = p_optica_id),
    pagos_agrupados AS (SELECT COALESCE(venta_id, 'v_disp_'||dispensacion_id, 'v_serv_'||servicio_extra_id) AS venta_id_match, SUM(monto) AS total_pagado FROM public.pagos WHERE optica_id = p_optica_id GROUP BY venta_id_match)
    SELECT COUNT(*) INTO v_servicios FROM all_ventas v LEFT JOIN pagos_agrupados pg ON pg.venta_id_match = v.venta_id WHERE v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;
    RETURN jsonb_build_object('entregas_pendientes', v_entregas, 'servicios_pendientes', v_servicios);
END; $$;

-- 2.5: REWRITE rpc_analisis_mensual (proyeccion only)
CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(p_optica_id TEXT, p_mes DATE) RETURNS jsonb LANGUAGE plpgsql STABLE SET search_path = public AS $$
DECLARE v_ventas_mes NUMERIC; v_cobros_mes NUMERIC; v_costo_mes NUMERIC; v_gastos_mes NUMERIC; v_saldo_pendiente NUMERIC; v_margen_neto_pct NUMERIC; v_ticket_promedio NUMERIC; v_cantidad_ventas INTEGER; v_mes_anterior DATE; v_ventas_mes_anterior NUMERIC; v_margen_categoria jsonb; v_deudores_resumen jsonb; v_proyeccion jsonb; v_stock_estancado jsonb; v_valor_inventario NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';
    SELECT COALESCE(SUM(ventas_monto_total),0), COALESCE(SUM(cobros_monto_total),0), COALESCE(SUM(ventas_costo_total),0), COALESCE(SUM(ventas_cantidad),0) INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas FROM public.resumen_diario WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    v_ticket_promedio:=CASE WHEN v_cantidad_ventas>0 THEN v_ventas_mes/v_cantidad_ventas ELSE 0 END;
    SELECT COALESCE(SUM(monto),0) INTO v_gastos_mes FROM public.gastos_operativos WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    SELECT COALESCE(saldo_pendiente_total,0) INTO v_saldo_pendiente FROM public.resumen_diario WHERE optica_id=p_optica_id AND fecha<p_mes+INTERVAL'1 month' ORDER BY fecha DESC LIMIT 1;
    v_margen_neto_pct:=CASE WHEN v_ventas_mes>0 THEN ROUND(((v_ventas_mes-v_costo_mes-v_gastos_mes)/v_ventas_mes)*100,1) ELSE 0 END;
    SELECT COALESCE(SUM(ventas_monto_total),0) INTO v_ventas_mes_anterior FROM public.resumen_diario WHERE optica_id=p_optica_id AND fecha>=v_mes_anterior AND fecha<p_mes;
    SELECT COALESCE(jsonb_agg(jsonb_build_object('categoria',cat.nombre,'ventas',COALESCE(mc.ventas_totales,0),'costos',COALESCE(mc.costo_total,0),'margen_pct',COALESCE(mc.margen_porcentaje,0))),'[]'::jsonb) INTO v_margen_categoria FROM public.categorias_producto cat LEFT JOIN public.margen_por_categoria mc ON mc.categoria_producto_id=cat.id AND mc.optica_id=p_optica_id AND mc.periodo=to_char(p_mes,'YYYY-MM') AND mc.tipo_periodo='mensual';
    SELECT jsonb_build_object('cantidad',COUNT(*),'saldo_total',COALESCE(SUM(saldo),0)) INTO v_deudores_resumen FROM public.rpc_deudores(p_optica_id);
    WITH pagos_dedup AS (SELECT COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match, pg.monto FROM public.pagos pg WHERE pg.optica_id=p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'),
    all_ventas AS (SELECT 'v_disp_' || id AS venta_id, monto_total FROM public.dispensaciones WHERE optica_id = p_optica_id UNION ALL SELECT 'v_serv_' || id AS venta_id, monto_total FROM public.servicios_extra WHERE optica_id = p_optica_id)
    SELECT jsonb_build_object('ingresos_esperados', COALESCE(SUM(v.monto_total-COALESCE(pd_total.total_pagado,0)),0), 'egresos_programados',COALESCE((SELECT SUM(monto) FROM public.gastos_operativos WHERE optica_id=p_optica_id AND fecha_programada>=CURRENT_DATE),0),'saldo_neto',0) INTO v_proyeccion FROM all_ventas v LEFT JOIN (SELECT venta_id_match,SUM(monto) AS total_pagado FROM pagos_dedup GROUP BY venta_id_match) pd_total ON pd_total.venta_id_match=v.venta_id WHERE v.monto_total-COALESCE(pd_total.total_pagado,0)>0.005;
    v_proyeccion:=jsonb_set(v_proyeccion,'{saldo_neto}',to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric,0)-COALESCE((v_proyeccion->>'egresos_programados')::numeric,0)));
    SELECT COALESCE(jsonb_agg(jsonb_build_object('montura_id',m.id,'sku',m.sku,'modelo',m.modelo,'costo',COALESCE(m.costo,0),'stock_actual',m.stock_actual,'ultima_venta',NULL,'dias_sin_venta',999)),'[]'::jsonb) INTO v_stock_estancado FROM public.monturas m WHERE m.optica_id=p_optica_id AND m.activo=true AND m.stock_actual<=m.stock_minimo AND m.stock_actual>0;
    SELECT COALESCE(SUM(costo*stock_actual),0) INTO v_valor_inventario FROM public.monturas WHERE optica_id=p_optica_id AND activo=true;
    RETURN jsonb_build_object('ventas_mes',v_ventas_mes,'cobros_mes',v_cobros_mes,'costo_mes',v_costo_mes,'gastos_mes',v_gastos_mes,'saldo_pendiente',v_saldo_pendiente,'margen_neto_pct',v_margen_neto_pct,'ticket_promedio',v_ticket_promedio,'cantidad_ventas',v_cantidad_ventas,'ventas_mes_anterior',v_ventas_mes_anterior,'variacion_ventas_pct',CASE WHEN v_ventas_mes_anterior>0 THEN ROUND(((v_ventas_mes-v_ventas_mes_anterior)/v_ventas_mes_anterior)*100,1) ELSE NULL END,'margen_por_categoria',v_margen_categoria,'deudores',v_deudores_resumen,'proyeccion_caja',v_proyeccion,'stock_estancado',v_stock_estancado,'valor_inventario',v_valor_inventario);
END; $$;

-- 2.7: DROP ventas
DROP TABLE IF EXISTS public.ventas CASCADE;

-- Re-grant permissions
REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(TEXT, DATE) TO authenticated, service_role;
REVOKE EXECUTE ON FUNCTION public.rpc_deudores(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_deudores(TEXT) TO authenticated, service_role;
REVOKE EXECUTE ON FUNCTION public.rpc_count_pendientes(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_count_pendientes(TEXT) TO authenticated, service_role;
REVOKE EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) TO authenticated, service_role;;
