
-- Recalculo correcto: ventas y pagos por separado
INSERT INTO public.resumen_diario (optica_id, fecha, ventas_cantidad, ventas_monto_total, ventas_costo_total, cobros_cantidad, cobros_monto_total, saldo_pendiente_total, saldo_pendiente_cantidad)
SELECT 
    v.optica_id, v.fecha,
    v.ventas_cant, v.ventas_monto, v.ventas_costo,
    COALESCE(p.cobros_cant, 0), COALESCE(p.cobros_monto, 0),
    0, 0
FROM (
    SELECT optica_id, fecha, COUNT(*) as ventas_cant, SUM(monto_total) as ventas_monto, COALESCE(SUM(costo_unitario_snapshot), 0) as ventas_costo
    FROM public.ventas WHERE optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8'
    GROUP BY optica_id, fecha
) v
LEFT JOIN (
    SELECT optica_id, fecha, COUNT(*) as cobros_cant, SUM(monto) as cobros_monto
    FROM public.pagos WHERE optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8'
    GROUP BY optica_id, fecha
) p ON p.optica_id = v.optica_id AND p.fecha = v.fecha
ON CONFLICT (optica_id, fecha) DO UPDATE SET
    ventas_cantidad = EXCLUDED.ventas_cantidad,
    ventas_monto_total = EXCLUDED.ventas_monto_total,
    ventas_costo_total = EXCLUDED.ventas_costo_total,
    cobros_cantidad = EXCLUDED.cobros_cantidad,
    cobros_monto_total = EXCLUDED.cobros_monto_total,
    calculado_en = now();
;
