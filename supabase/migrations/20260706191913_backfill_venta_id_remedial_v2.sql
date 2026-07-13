UPDATE public.pagos
SET venta_id = 'v_disp_' || dispensacion_id
WHERE dispensacion_id IS NOT NULL AND venta_id IS NULL;

UPDATE public.pagos
SET venta_id = 'v_serv_' || servicio_extra_id
WHERE servicio_extra_id IS NOT NULL AND venta_id IS NULL;;
