
DO $$ DECLARE r RECORD; BEGIN
FOR r IN SELECT DISTINCT optica_id, fecha FROM public.ventas WHERE costo_unitario_snapshot IS NOT NULL
LOOP PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha); END LOOP;
END $$;
;
