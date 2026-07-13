-- Drop the immediate FK and recreate as DEFERRABLE
-- This allows pagos to reference ventas that are created in the same transaction
ALTER TABLE public.pagos DROP CONSTRAINT IF EXISTS fk_pagos_venta;

ALTER TABLE public.pagos ADD CONSTRAINT fk_pagos_venta
    FOREIGN KEY (venta_id) REFERENCES public.ventas(id)
    DEFERRABLE INITIALLY DEFERRED;;
