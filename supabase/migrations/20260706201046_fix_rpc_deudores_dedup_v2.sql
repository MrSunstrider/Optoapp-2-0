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
    dias_deuda INTEGER,
    paciente_id TEXT
)
LANGUAGE sql SECURITY INVOKER STABLE
AS $$
    WITH pagos_dedup AS (
        -- Deduplicate: each pago is counted against exactly ONE venta,
        -- preferring direct venta_id match over dispensacion_id / servicio_extra_id fallback.
        SELECT DISTINCT ON (pg.id)
            CASE
                WHEN pg.venta_id IS NOT NULL THEN pg.venta_id
                WHEN pg.dispensacion_id IS NOT NULL THEN 'v_disp_' || pg.dispensacion_id
                WHEN pg.servicio_extra_id IS NOT NULL THEN 'v_serv_' || pg.servicio_extra_id
            END AS venta_id_match,
            pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
        ORDER BY pg.id, pg.venta_id NULLS LAST
    )
    SELECT
        COALESCE(p.nombre_completo, 'Sin paciente'),
        p.telefono,
        v.id,
        v.fecha,
        v.monto_total,
        COALESCE(SUM(pd.monto), 0) AS total_pagado,
        v.monto_total - COALESCE(SUM(pd.monto), 0) AS saldo,
        CURRENT_DATE - v.fecha AS dias_deuda,
        v.paciente_id
    FROM public.ventas v
    LEFT JOIN public.pacientes p ON p.id = v.paciente_id
    LEFT JOIN pagos_dedup pd ON pd.venta_id_match = v.id
    WHERE v.optica_id = p_optica_id
    GROUP BY v.id, v.fecha, v.monto_total, p.nombre_completo, p.telefono, v.paciente_id
    HAVING v.monto_total - COALESCE(SUM(pd.monto), 0) > 0.005
    ORDER BY dias_deuda DESC;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_deudores(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_deudores(TEXT) TO authenticated, service_role;;
