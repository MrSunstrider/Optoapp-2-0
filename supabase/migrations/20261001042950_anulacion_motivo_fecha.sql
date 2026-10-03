-- Cancellation metadata for dispensaciones and servicios_extra (anulaciones-devoluciones-reclamos).
-- Additive and nullable: no default, no backfill, no constraint or RLS change. Legacy Anulado rows stay null.
-- Rollout: apply to production BEFORE any client that sends motivo_anulacion / fecha_anulacion (PGRST204 otherwise).

ALTER TABLE public.dispensaciones
  ADD COLUMN IF NOT EXISTS motivo_anulacion text,
  ADD COLUMN IF NOT EXISTS fecha_anulacion date;

ALTER TABLE public.servicios_extra
  ADD COLUMN IF NOT EXISTS motivo_anulacion text,
  ADD COLUMN IF NOT EXISTS fecha_anulacion date;

COMMENT ON COLUMN public.dispensaciones.motivo_anulacion IS 'Reason of the terminal transition (Anulado or Reclamada)';
COMMENT ON COLUMN public.dispensaciones.fecha_anulacion IS 'Date of the terminal transition (Anulado or Reclamada)';
COMMENT ON COLUMN public.servicios_extra.motivo_anulacion IS 'Reason of the cancellation (Anulado)';
COMMENT ON COLUMN public.servicios_extra.fecha_anulacion IS 'Date of the cancellation (Anulado)';
