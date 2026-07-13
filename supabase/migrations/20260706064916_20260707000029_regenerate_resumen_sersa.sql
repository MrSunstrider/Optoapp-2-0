
DO $$ DECLARE r RECORD; BEGIN
FOR r IN SELECT DISTINCT optica_id, fecha FROM public.pagos WHERE optica_id = '25af5a92-4a2d-4e7a-957f-61bec87a07d8'
LOOP PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha); END LOOP;
END $$;
;
