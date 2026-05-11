-- P3-T1: OT única por óptica (ignorando vacíos legacy).

UPDATE public.dispensaciones d
SET ot = d.ot || '-d' || substr(d.id, 1, 8)
WHERE d.id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY optica_id, lower(trim(ot))
                   ORDER BY id
               ) AS rn
        FROM public.dispensaciones
        WHERE trim(ot) <> ''
    ) t
    WHERE t.rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dispensaciones_optica_ot_unique
ON public.dispensaciones (optica_id, lower(trim(ot)))
WHERE trim(ot) <> '';

COMMENT ON INDEX public.idx_dispensaciones_optica_ot_unique IS 'Una OT no puede repetirse en la misma óptica (P3-T1)';
