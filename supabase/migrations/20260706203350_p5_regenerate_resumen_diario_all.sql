DO $$
DECLARE
    r RECORD;
    cnt INTEGER := 0;
BEGIN
    FOR r IN (
        SELECT DISTINCT optica_id, fecha
        FROM (
            SELECT optica_id, fecha FROM public.ventas
            UNION
            SELECT optica_id, fecha FROM public.pagos
        ) fechas
    ) LOOP
        PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha);
        cnt := cnt + 1;
    END LOOP;

    RAISE NOTICE 'P5: resumen_diario regenerated for % (optica_id, fecha) pairs', cnt;
END;
$$;;
