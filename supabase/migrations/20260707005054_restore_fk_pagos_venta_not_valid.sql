-- Restore FK with NOT VALID to skip existing data check, then validate.
-- The sync upload order is already correct: dispensaciones → servicios → pagos.
-- Each is a separate HTTP transaction, so DEFERRABLE is not needed — the venta
-- exists by the time the pago is uploaded.
ALTER TABLE public.pagos ADD CONSTRAINT fk_pagos_venta
    FOREIGN KEY (venta_id) REFERENCES public.ventas(id)
    NOT VALID;

-- Validate immediately since we confirmed 0 orphan pagos
ALTER TABLE public.pagos VALIDATE CONSTRAINT fk_pagos_venta;;
