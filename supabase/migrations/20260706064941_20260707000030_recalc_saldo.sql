-- Safe for empty databases: only runs if the optica exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM public.opticas WHERE id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8') THEN
    PERFORM public.recalcular_resumen_diario('25af5a92-4a2d-4e7a-957f-61bec87a07d8', '2026-06-01');
    PERFORM public.recalcular_resumen_diario('25af5a92-4a2d-4e7a-957f-61bec87a07d8', '2026-05-01');
    PERFORM public.recalcular_resumen_diario('25af5a92-4a2d-4e7a-957f-61bec87a07d8', '2026-07-01');
  END IF;
END;
$$;
