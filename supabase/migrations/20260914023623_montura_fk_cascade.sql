-- Remote-applied montura FK CASCADE (history sync stub).
-- Soft-delete remains primary; CASCADE only on hard DELETE of monturas.

ALTER TABLE public.orden_compra_items
  DROP CONSTRAINT IF EXISTS orden_compra_items_montura_id_fkey,
  ADD CONSTRAINT orden_compra_items_montura_id_fkey
    FOREIGN KEY (montura_id) REFERENCES public.monturas(id) ON DELETE CASCADE;

ALTER TABLE public.inventario_fisico_detalle
  DROP CONSTRAINT IF EXISTS inventario_fisico_detalle_montura_id_fkey,
  ADD CONSTRAINT inventario_fisico_detalle_montura_id_fkey
    FOREIGN KEY (montura_id) REFERENCES public.monturas(id) ON DELETE CASCADE;
