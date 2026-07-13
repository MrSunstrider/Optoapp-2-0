-- F1-T1: Elimina la unicidad de OT para permitir múltiples dispensaciones
-- con el mismo número de OT (ej. varios lentes en un mismo trabajo de laboratorio).
--
-- Se dropean AMBOS índices únicos existentes:
--   1. idx_dispensaciones_optica_ot_unique (partial, trim)
--   2. dispensaciones_optica_ot_uq_v2 (partial, btrim)
-- Ambos imponian (optica_id, ot) único, impidiendo registrar dos lentes
-- con la misma OT para un mismo paciente.

DROP INDEX IF EXISTS public.idx_dispensaciones_optica_ot_unique;
DROP INDEX IF EXISTS public.dispensaciones_optica_ot_uq_v2;
COMMENT ON TABLE public.dispensaciones IS 'Dispensaciones / Órdenes de trabajo. OT ya no es única por óptica.';
