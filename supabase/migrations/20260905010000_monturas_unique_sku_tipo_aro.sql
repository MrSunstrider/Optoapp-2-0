-- Allow same SKU with different tipo_aro as independent inventory rows.
DROP INDEX IF EXISTS public.idx_monturas_sku_optica;

CREATE UNIQUE INDEX IF NOT EXISTS idx_monturas_sku_optica_tipo_aro
  ON public.monturas (optica_id, sku, tipo_aro);
