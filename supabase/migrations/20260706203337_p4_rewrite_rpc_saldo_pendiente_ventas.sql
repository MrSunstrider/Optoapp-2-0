CREATE OR REPLACE FUNCTION public.rpc_saldo_pendiente(
    p_optica_id TEXT
) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_disp numeric;
    v_serv numeric;
    v_total numeric;
BEGIN
    SELECT COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_disp
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos
        WHERE optica_id = p_optica_id
        GROUP BY venta_id
    ) pd ON pd.venta_id = v.id
    WHERE v.optica_id = p_optica_id AND v.origen = 'dispensacion'
      AND v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    SELECT COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_serv
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos
        WHERE optica_id = p_optica_id
        GROUP BY venta_id
    ) pd ON pd.venta_id = v.id
    WHERE v.optica_id = p_optica_id AND v.origen = 'servicio_extra'
      AND v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    v_total := v_disp + v_serv;

    RETURN jsonb_build_object(
        'saldo_dispensaciones', v_disp,
        'saldo_servicios', v_serv,
        'saldo_total', v_total
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_saldo_pendiente(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_saldo_pendiente(TEXT) TO authenticated, service_role;;
