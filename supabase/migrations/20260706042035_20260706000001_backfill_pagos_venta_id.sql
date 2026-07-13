
UPDATE public.pagos SET venta_id = v.id
FROM public.ventas v
WHERE pagos.dispensacion_id IS NOT NULL
  AND pagos.venta_id IS NULL
  AND v.origen_id = pagos.dispensacion_id
  AND v.origen = 'dispensacion';

UPDATE public.pagos SET venta_id = v.id
FROM public.ventas v
WHERE pagos.servicio_extra_id IS NOT NULL
  AND pagos.venta_id IS NULL
  AND v.origen_id = pagos.servicio_extra_id
  AND v.origen = 'servicio_extra';
;
