
UPDATE public.pagos pg SET venta_id = v.id
FROM public.ventas v
WHERE pg.venta_id IS NULL
  AND pg.dispensacion_id IS NOT NULL
  AND v.origen_id = pg.dispensacion_id
  AND v.origen = 'dispensacion'
  AND v.id IS NOT NULL;
;
