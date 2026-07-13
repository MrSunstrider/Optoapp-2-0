-- Drop the FK that blocks async sync uploads.
-- Data integrity is enforced at the application level (ventaId is set in all ViewModels).
-- The pagos and ventas arrive in separate HTTP requests, so a DB-level FK can't work.
ALTER TABLE public.pagos DROP CONSTRAINT IF EXISTS fk_pagos_venta;

-- Keep optica FK (it references a stable table, not dependent on sync order)
-- fk_ventas_optica remains — opticas are created before any data is synced;
