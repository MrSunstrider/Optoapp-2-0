-- Montura child FKs: ON DELETE CASCADE for OC items + IF detalle.
-- Soft-delete (activo=false) remains the UI path; CASCADE is hard-delete safety only.
-- Do NOT apply remotely until GGA passes.

ALTER TABLE public.orden_compra_items
  DROP CONSTRAINT IF EXISTS orden_compra_items_montura_id_fkey;

ALTER TABLE public.orden_compra_items
  ADD CONSTRAINT orden_compra_items_montura_id_fkey
  FOREIGN KEY (montura_id) REFERENCES public.monturas(id) ON DELETE CASCADE;

ALTER TABLE public.inventario_fisico_detalle
  DROP CONSTRAINT IF EXISTS inventario_fisico_detalle_montura_id_fkey;

ALTER TABLE public.inventario_fisico_detalle
  ADD CONSTRAINT inventario_fisico_detalle_montura_id_fkey
  FOREIGN KEY (montura_id) REFERENCES public.monturas(id) ON DELETE CASCADE;
