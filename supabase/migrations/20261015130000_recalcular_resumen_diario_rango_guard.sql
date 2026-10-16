-- ============================================================================
-- recalcular_resumen_diario_rango: guarded multi-day recalculation for one optica
-- Change: costos-margenes-reales / S23
--
-- Members recalculate their own optica over a bounded window so resumen_diario
-- picks up the real costs from S19 without an operator backfill. The function
-- is SECURITY INVOKER: every guard runs with the caller's identity, and each day
-- delegates to recalcular_resumen_diario, which keeps its own membership guard.
-- Bounds: at most 366 days, no later than tomorrow (timezone slack between the
-- device and the server), and windows over 31 days only for admin and gerente,
-- so an asesor cannot trigger long recalculation loops.
-- Rollback (manual, never auto-applied):
--   DROP FUNCTION IF EXISTS public.recalcular_resumen_diario_rango(TEXT, DATE, DATE);
-- ============================================================================
CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario_rango(
    p_optica_id TEXT,
    p_desde DATE,
    p_hasta DATE
) RETURNS integer
LANGUAGE plpgsql SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    v_dias integer;
    v_dia date;
BEGIN
    IF auth.uid() IS NULL OR NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied' USING ERRCODE = '42501';
    END IF;
    IF p_desde IS NULL OR p_hasta IS NULL OR p_hasta < p_desde THEN
        RAISE EXCEPTION 'Invalid range' USING ERRCODE = '22023';
    END IF;
    IF p_hasta > CURRENT_DATE + 1 THEN
        RAISE EXCEPTION 'Range ends in the future' USING ERRCODE = '22023';
    END IF;
    v_dias := p_hasta - p_desde + 1;
    IF v_dias > 366 THEN
        RAISE EXCEPTION 'Range too long' USING ERRCODE = '22023';
    END IF;
    IF v_dias > 31 AND NOT app_private.has_optica_role(auth.uid(), p_optica_id, ARRAY['admin', 'gerente']) THEN
        RAISE EXCEPTION 'Access denied' USING ERRCODE = '42501';
    END IF;

    FOR v_dia IN SELECT generate_series(p_desde, p_hasta, interval '1 day')::date LOOP
        PERFORM public.recalcular_resumen_diario(p_optica_id, v_dia);
    END LOOP;

    RETURN v_dias;
END $$;

REVOKE EXECUTE ON FUNCTION public.recalcular_resumen_diario_rango(TEXT, DATE, DATE) FROM PUBLIC, anon, service_role;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario_rango(TEXT, DATE, DATE) TO authenticated;
