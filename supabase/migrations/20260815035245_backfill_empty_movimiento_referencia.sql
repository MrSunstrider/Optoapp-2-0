-- Backfill blank montura_movimientos.referencia_id to the row's own id.
-- Manual entrada/salida historically wrote ''. The unique index
-- (referencia_id, tipo, montura_id) then rejects a second manual move on the
-- same montura. Using id is safe: it is the primary key, so the triple stays unique.
-- Change: fix-inventory-movement-referencia-identity

UPDATE public.montura_movimientos
SET referencia_id = id
WHERE referencia_id IS NULL OR referencia_id = '';
