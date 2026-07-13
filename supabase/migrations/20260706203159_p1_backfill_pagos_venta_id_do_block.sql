DO $$
DECLARE
    disp_count INTEGER;
    serv_count INTEGER;
BEGIN
    UPDATE public.pagos
    SET venta_id = 'v_disp_' || dispensacion_id
    WHERE dispensacion_id IS NOT NULL AND venta_id IS NULL;
    GET DIAGNOSTICS disp_count = ROW_COUNT;

    UPDATE public.pagos
    SET venta_id = 'v_serv_' || servicio_extra_id
    WHERE servicio_extra_id IS NOT NULL AND venta_id IS NULL;
    GET DIAGNOSTICS serv_count = ROW_COUNT;

    RAISE NOTICE 'P1 backfill: % dispensacion pagos updated, % servicio_extra pagos updated', disp_count, serv_count;
END;
$$;;
