-- Fix: Remove 'montura' from costos_productos.stock_o_fabricacion CHECK
-- Monturas cost is managed in inventory (monturas.costo), not in cost matrix.
-- 0 rows in table pre-production — safe to alter.

ALTER TABLE public.costos_productos
  DROP CONSTRAINT IF EXISTS costos_productos_stock_o_fabricacion_check;

ALTER TABLE public.costos_productos
  ADD CONSTRAINT costos_productos_stock_o_fabricacion_check
  CHECK (stock_o_fabricacion IN ('stock', 'fabricacion'));
