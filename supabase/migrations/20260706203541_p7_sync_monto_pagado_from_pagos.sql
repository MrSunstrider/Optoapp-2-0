DO $$
DECLARE
    disp_cnt INTEGER;
    serv_cnt INTEGER;
BEGIN
    -- Sync dispensaciones.monto_pagado from actual pagos
    UPDATE public.dispensaciones d
    SET monto_pagado = (
        SELECT COALESCE(SUM(monto), 0)
        FROM public.pagos pg
        WHERE pg.dispensacion_id = d.id
    );
    GET DIAGNOSTICS disp_cnt = ROW_COUNT;

    -- Sync servicios_extra.a_cuenta from actual pagos
    UPDATE public.servicios_extra s
    SET a_cuenta = (
        SELECT COALESCE(SUM(monto), 0)
        FROM public.pagos pg
        WHERE pg.servicio_extra_id = s.id
    );
    GET DIAGNOSTICS serv_cnt = ROW_COUNT;

    RAISE NOTICE 'monto_pagado synced for % dispensaciones, a_cuenta for % servicios_extra', disp_cnt, serv_cnt;
END;
$$;;
