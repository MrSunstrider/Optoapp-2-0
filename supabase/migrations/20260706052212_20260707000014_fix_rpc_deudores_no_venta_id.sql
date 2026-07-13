
DROP FUNCTION IF EXISTS public.rpc_deudores(TEXT);

CREATE OR REPLACE FUNCTION public.rpc_deudores(p_optica_id TEXT)
RETURNS TABLE(
    paciente_nombre TEXT, paciente_telefono TEXT, venta_id TEXT, venta_fecha DATE,
    monto_total NUMERIC, total_pagado NUMERIC, saldo NUMERIC, dias_deuda INTEGER,
    paciente_id TEXT
)
LANGUAGE sql SECURITY INVOKER STABLE
AS $$
    SELECT
        COALESCE(p.nombre_completo, 'Sin paciente'), p.telefono, v.id, v.fecha,
        v.monto_total, COALESCE(pg_sum.total_pagado, 0) AS total_pagado,
        v.monto_total - COALESCE(pg_sum.total_pagado, 0) AS saldo,
        CURRENT_DATE - v.fecha AS dias_deuda,
        v.paciente_id
    FROM public.ventas v
    LEFT JOIN public.pacientes p ON p.id = v.paciente_id
    LEFT JOIN LATERAL (
        SELECT SUM(pg.monto) AS total_pagado
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
          AND (
            (v.origen = 'dispensacion' AND pg.dispensacion_id = v.origen_id)
            OR (v.origen = 'servicio_extra' AND pg.servicio_extra_id = v.origen_id)
          )
    ) pg_sum ON true
    WHERE v.optica_id = p_optica_id
    GROUP BY v.id, v.fecha, v.monto_total, p.nombre_completo, p.telefono, v.paciente_id, pg_sum.total_pagado
    HAVING v.monto_total - COALESCE(pg_sum.total_pagado, 0) > 0.005
    ORDER BY dias_deuda DESC;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_deudores(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_deudores(TEXT) TO authenticated, service_role;
;
