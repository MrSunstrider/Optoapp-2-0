
-- Crear pagos para deudas antiguas (antes de 19-03-2026) que no tienen pago vinculado
INSERT INTO public.pagos (id, dispensacion_id, servicio_extra_id, fecha, tipo, monto, metodo_pago, nota, optica_id, venta_id)
SELECT 
    gen_random_uuid()::text,
    CASE WHEN v.origen = 'dispensacion' THEN v.origen_id ELSE NULL END,
    CASE WHEN v.origen = 'servicio_extra' THEN v.origen_id ELSE NULL END,
    v.fecha,
    'Abono',
    v.monto_total - COALESCE(
        (SELECT SUM(pg2.monto) FROM public.pagos pg2 
         WHERE (pg2.venta_id = v.id 
            OR (v.origen = 'dispensacion' AND pg2.dispensacion_id = v.origen_id)
            OR (v.origen = 'servicio_extra' AND pg2.servicio_extra_id = v.origen_id))
         AND pg2.optica_id = v.optica_id), 0),
    'Efectivo',
    'Cancelacion deuda historica',
    v.optica_id,
    v.id
FROM public.ventas v
WHERE v.optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8'
  AND v.fecha <= '2026-03-19'
  AND v.monto_total - COALESCE(
        (SELECT SUM(pg2.monto) FROM public.pagos pg2 
         WHERE (pg2.venta_id = v.id 
            OR (v.origen = 'dispensacion' AND pg2.dispensacion_id = v.origen_id)
            OR (v.origen = 'servicio_extra' AND pg2.servicio_extra_id = v.origen_id))
         AND pg2.optica_id = v.optica_id), 0) > 0.005;
;
