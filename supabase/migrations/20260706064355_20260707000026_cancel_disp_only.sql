
INSERT INTO public.pagos (id, dispensacion_id, servicio_extra_id, fecha, tipo, monto, metodo_pago, nota, optica_id, venta_id)
SELECT 
    gen_random_uuid()::text,
    v.origen_id,
    NULL,
    v.fecha,
    'Abono',
    d.saldo,
    'Efectivo',
    'Cancelacion deuda historica',
    v.optica_id,
    v.id
FROM public.rpc_deudores('25af5a92-4a2d-4e7a-957f-61bec87a07d8') d
JOIN public.ventas v ON v.id = d.venta_id
WHERE v.origen = 'dispensacion'
  AND v.origen_id IN (SELECT id FROM public.dispensaciones);
;
