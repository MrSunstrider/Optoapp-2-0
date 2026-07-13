
UPDATE public.pagos pg SET venta_id = v.id
FROM public.ventas v
WHERE pg.venta_id IS NULL AND pg.dispensacion_id IS NOT NULL AND v.origen_id = pg.dispensacion_id AND v.origen = 'dispensacion';

UPDATE public.pagos pg SET venta_id = v.id
FROM public.ventas v
WHERE pg.venta_id IS NULL AND pg.servicio_extra_id IS NOT NULL AND v.origen_id = pg.servicio_extra_id AND v.origen = 'servicio_extra';
;
