
BEGIN;
WITH disp_matched AS (
  SELECT pg.id as pago_id, v.id as venta_id
  FROM public.pagos pg
  JOIN public.ventas v ON v.origen_id = pg.dispensacion_id AND v.origen = 'dispensacion'
  WHERE pg.venta_id IS NULL AND pg.dispensacion_id IS NOT NULL
)
UPDATE public.pagos SET venta_id = disp_matched.venta_id
FROM disp_matched
WHERE public.pagos.id = disp_matched.pago_id;

WITH serv_matched AS (
  SELECT pg.id as pago_id, v.id as venta_id
  FROM public.pagos pg
  JOIN public.ventas v ON v.origen_id = pg.servicio_extra_id AND v.origen = 'servicio_extra'
  WHERE pg.venta_id IS NULL AND pg.servicio_extra_id IS NOT NULL
)
UPDATE public.pagos SET venta_id = serv_matched.venta_id
FROM serv_matched
WHERE public.pagos.id = serv_matched.pago_id;
COMMIT;
;
