
-- Backfill costo_unitario_snapshot from monturas.costo for dispensacion ventas
UPDATE public.ventas v SET costo_unitario_snapshot = m.costo
FROM public.dispensaciones d
JOIN public.monturas m ON m.id = d.montura_id AND m.optica_id = d.optica_id
WHERE v.origen = 'dispensacion'
  AND v.origen_id = d.id
  AND v.optica_id = d.optica_id
  AND (v.costo_unitario_snapshot IS NULL OR v.costo_unitario_snapshot = 0)
  AND m.costo IS NOT NULL
  AND m.optica_id = v.optica_id;
;
