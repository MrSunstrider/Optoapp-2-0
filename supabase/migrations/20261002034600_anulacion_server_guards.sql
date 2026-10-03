-- ============================================================================
-- Anulacion server guards: one claim per original + traced dispensacion deletes
-- Feature: odd/tasks/anulaciones-server-guards.md (T1)
--
-- The Android client works offline, so two races are invisible to it:
--   * Two devices claim the same delivered order; each uploads a replacement
--     with reclamo_origen_id = original.id. The first to reach the server wins.
--   * A device hard-deletes an order whose pagos or montura movimientos were
--     recorded on another device and never downloaded; ON DELETE CASCADE on
--     pagos / dispensacion_items / regalos_dispensacion would silently erase
--     them.
--
-- Contract consumed by the client (keep these tokens stable):
--   * Unique violation (23505) on index dispensaciones_reclamo_origen_uidx.
--   * Delete refusal: SQLSTATE P0001, message starts with
--     'dispensacion_has_trace'.
--
-- Idempotent: CREATE ... IF NOT EXISTS, CREATE OR REPLACE and DROP TRIGGER IF
-- EXISTS make the file safe to re-run. Transaction-safe: no CONCURRENTLY.
-- Production preflight (2026-10-01): 0 rows with reclamo_origen_id, so the
-- unique index builds without conflicts.
-- Rollout: apply BEFORE clients that recognise these refusals are relied on;
-- older clients keep working (a refused delete is retried and logged).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. At most one replacement per claimed original.
--    Deliberately GLOBAL, not per-optica: dispensacion ids are globally unique,
--    and adding optica_id would let a mismatched-tenant upload create a second
--    replacement for the same original. Tenant scoping stays with RLS.
-- ----------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS dispensaciones_reclamo_origen_uidx
    ON public.dispensaciones (reclamo_origen_id)
    WHERE reclamo_origen_id IS NOT NULL;

COMMENT ON INDEX public.dispensaciones_reclamo_origen_uidx IS
'A dispensacion can be claimed (reclamada) at most once: one replacement per reclamo_origen_id. The Android sync quarantines the losing offline claim on this violation.';

-- ----------------------------------------------------------------------------
-- 2. Refuse a DIRECT delete of a dispensacion that already has a cash or stock
--    trace. Mirrors the Android local check (MonturaMovimientoDao trace query):
--    pagos.dispensacion_id = id, or a montura movimiento of the same optica
--    whose referencia_id is the sale id, a '<id>:...' cancel/restock reference,
--    or the id of one of its regalos.
--
--    Cascades from a paciente delete stay allowed: paciente deletes have their
--    own guardrails (trg_guard_pacientes_delete) and must remove the history.
--    Detection relies on ON DELETE CASCADE semantics: the RI cascade runs as an
--    AFTER trigger on pacientes, i.e. after the paciente row was deleted in the
--    same transaction, and every statement in this VOLATILE function takes a
--    fresh snapshot that includes that deletion. So during a cascade the parent
--    paciente is no longer visible, while for a direct delete it still is (the
--    FK guarantees a non-null paciente_id points at an existing paciente).
--
--    SECURITY DEFINER so the guard fails closed: the existence checks must not
--    depend on what the caller's RLS lets it see (a hidden pago or paciente
--    would otherwise turn the guard into a silent allow). Trigger functions
--    cannot be invoked through PostgREST, and EXECUTE is revoked below.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.guard_dispensaciones_delete()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF OLD.paciente_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM public.pacientes p WHERE p.id = OLD.paciente_id
    ) THEN
        RETURN OLD;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.pagos pg WHERE pg.dispensacion_id = OLD.id
    ) OR EXISTS (
        SELECT 1
        FROM public.montura_movimientos m
        WHERE m.optica_id = OLD.optica_id
          AND (
              m.referencia_id = OLD.id
           OR starts_with(m.referencia_id, OLD.id || ':')
           OR m.referencia_id IN (
                  SELECT r.id FROM public.regalos_dispensacion r
                  WHERE r.dispensacion_id = OLD.id
              )
          )
    ) THEN
        RAISE EXCEPTION 'dispensacion_has_trace: %', OLD.id
            USING ERRCODE = 'P0001',
                  DETAIL = 'The dispensacion has pagos or montura movimientos.',
                  HINT = 'Cancel (anular) the dispensacion instead of deleting it.';
    END IF;

    RETURN OLD;
END;
$$;

COMMENT ON FUNCTION public.guard_dispensaciones_delete() IS
'BEFORE DELETE guard: refuses a direct delete of a dispensacion with pagos or montura movimientos (SQLSTATE P0001, message token dispensacion_has_trace). Paciente cascades are allowed.';

DROP TRIGGER IF EXISTS trg_guard_dispensaciones_delete ON public.dispensaciones;
CREATE TRIGGER trg_guard_dispensaciones_delete
    BEFORE DELETE ON public.dispensaciones
    FOR EACH ROW
    EXECUTE FUNCTION public.guard_dispensaciones_delete();

REVOKE EXECUTE ON FUNCTION public.guard_dispensaciones_delete()
    FROM PUBLIC, anon, authenticated;
