
INSERT INTO public.pagos (id, dispensacion_id, servicio_extra_id, fecha, tipo, monto, metodo_pago, nota, optica_id, venta_id)
SELECT 
    gen_random_uuid()::text,
    (SELECT id FROM public.dispensaciones WHERE optica_id = v.optica_id LIMIT 1),
    NULL,
    v.fecha,
    'Abono',
    3.00,
    'Efectivo',
    'Cancelacion servicio huerfano',
    v.optica_id,
    v.id
FROM public.ventas v
WHERE v.id IN ('v_serv_90580361-6055-4b19-9278-e86488dad8bf', 'v_serv_9401d598-223a-4b90-81a2-3c03d0472ca3')
  AND v.optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8';
;
