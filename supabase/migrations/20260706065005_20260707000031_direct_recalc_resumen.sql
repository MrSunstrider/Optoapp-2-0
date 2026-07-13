
INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total, cobros_cantidad, cobros_monto_total, saldo_pendiente_total, saldo_pendiente_cantidad, inventario_valor, inventario_unidades)
SELECT 
    '25af5a92-4a2d-4e7a-957f-61bec87a07d8',
    v.fecha,
    COUNT(DISTINCT v.id),
    COALESCE(SUM(v.monto_total), 0),
    COALESCE(SUM(v.costo_unitario_snapshot), 0),
    COUNT(DISTINCT pg.id),
    COALESCE(SUM(pg.monto), 0),
    COALESCE(SUM(v.monto_total) - SUM(pg.monto), 0),
    COUNT(DISTINCT v.id) FILTER (WHERE v.monto_total > COALESCE(pg.monto, 0)),
    0, 0
FROM public.ventas v
LEFT JOIN public.pagos pg ON pg.optica_id = v.optica_id AND pg.fecha = v.fecha
WHERE v.optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8'
GROUP BY v.fecha
ON CONFLICT (optica_id, fecha) DO UPDATE SET
    ventas_cantidad = EXCLUDED.ventas_cantidad,
    ventas_monto_total = EXCLUDED.ventas_monto_total,
    cobros_cantidad = EXCLUDED.cobros_cantidad,
    cobros_monto_total = EXCLUDED.cobros_monto_total,
    saldo_pendiente_total = EXCLUDED.saldo_pendiente_total,
    saldo_pendiente_cantidad = EXCLUDED.saldo_pendiente_cantidad,
    calculado_en = now();
;
