-- Fix: Update costos_biselado.tipo_aro CHECK to match OpticalCatalog
-- Remove: ranurado, taladro (unused)
-- Add: semi_aire
-- 0 rows in table pre-production — safe to alter.

ALTER TABLE public.costos_biselado
  DROP CONSTRAINT IF EXISTS costos_biselado_tipo_aro_check;

ALTER TABLE public.costos_biselado
  ADD CONSTRAINT costos_biselado_tipo_aro_check
  CHECK (tipo_aro IN ('aro_completo', 'semi_aire', 'al_aire'));
