-- ============================================================================
-- Migration: Regenerate resumen_diario for all existing (optica_id, fecha)
--
-- Recalculates every day that has transactional data in dispensaciones or
-- servicios_extra. This fixes the stale data since July 11 00:34 UTC caused
-- by the broken recalcular_resumen_diario.
--
-- Re-runnable: calling recalcular_resumen_diario multiple times is idempotent
-- (uses INSERT ... ON CONFLICT DO UPDATE).
-- ============================================================================

DO $$
DECLARE
    r RECORD;
    v_count INTEGER := 0;
BEGIN
    FOR r IN
        SELECT DISTINCT optica_id, fecha FROM public.dispensaciones
        WHERE fecha IS NOT NULL
        UNION
        SELECT DISTINCT optica_id, fecha FROM public.servicios_extra
        WHERE fecha IS NOT NULL
        ORDER BY optica_id, fecha
    LOOP
        PERFORM public.recalcular_resumen_diario(r.optica_id, r.fecha);
        v_count := v_count + 1;
    END LOOP;

    RAISE NOTICE 'Regenerated resumen_diario for % (optica_id, fecha) combinations', v_count;
END;
$$;
