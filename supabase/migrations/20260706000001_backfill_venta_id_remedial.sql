-- ============================================================================
-- Remedial: Re-backfill pagos.venta_id for any rows that arrived
-- from the Android client before the ventaId assignment fix was deployed.
--
-- The original backfill ran in 20260704000001 but pagos created by the buggy
-- Android client (which never set ventaId) continued syncing with NULL values.
--
-- This migration re-derives venta_id from the existing dispensacion_id
-- and servicio_extra_id columns, and is SAFE to run on rows that already
-- have a venta_id (the WHERE clause skips them).
-- ============================================================================

-- Backfill for dispensacion payments
UPDATE public.pagos
SET venta_id = 'v_disp_' || dispensacion_id
WHERE dispensacion_id IS NOT NULL
  AND venta_id IS NULL;

-- Backfill for servicio_extra payments
UPDATE public.pagos
SET venta_id = 'v_serv_' || servicio_extra_id
WHERE servicio_extra_id IS NOT NULL
  AND venta_id IS NULL;
