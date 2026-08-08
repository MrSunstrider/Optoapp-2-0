-- ============================================================================
-- Migration: Add pagos UNIQUE constraint for concurrent sync dedup
--
-- Prevent duplicate pagos entries when two offline devices upload the same
-- dispensacion payment simultaneously. The UNIQUE constraint covers the
-- semantic identity of a pago: (dispensacion_id, tipo, monto, metodo_pago,
-- fecha).
--
-- Strategy: Delete duplicates first (keeping earliest row), then add the
-- UNIQUE constraint directly. PostgreSQL does not support NOT VALID on
-- UNIQUE constraints, so cleanup must happen before constraint creation.
-- ============================================================================

-- 1. Remove pre-existing duplicates, keeping the earliest row per group.
--    Loss is minimal — duplicate rows are semantically identical.
DELETE FROM public.pagos
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.pagos
    GROUP BY dispensacion_id, tipo, monto, metodo_pago, fecha
);

-- 2. Add the UNIQUE constraint. After cleanup above, no existing rows
--    should violate it.
ALTER TABLE public.pagos
ADD CONSTRAINT pagos_unique_disp_payment
UNIQUE (dispensacion_id, tipo, monto, metodo_pago, fecha);
