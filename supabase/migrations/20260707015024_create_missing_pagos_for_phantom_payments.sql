-- Create missing pago records for dispensaciones that have monto_pagado > 0 but no actual pagos.
-- These are "phantom payments" from the old system where monto_pagado was set directly
-- without creating corresponding pago records.
INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, venta_id, updated_at)
SELECT
    gen_random_uuid()::text,
    d.id,
    d.fecha,
    'Abono',
    d.monto_pagado,
    'Efectivo',
    d.optica_id,
    'v_disp_' || d.id,
    now()
FROM public.dispensaciones d
WHERE d.monto_pagado > 0
  AND NOT EXISTS (
    SELECT 1 FROM public.pagos pg 
    WHERE pg.dispensacion_id = d.id AND pg.tipo IS DISTINCT FROM 'Anulación'
  );

-- Verify: should be 0 remaining
DO $$
DECLARE
    remaining INTEGER;
BEGIN
    SELECT COUNT(*) INTO remaining FROM public.dispensaciones d
    WHERE d.monto_pagado > 0 AND NOT EXISTS (
        SELECT 1 FROM public.pagos pg WHERE pg.dispensacion_id = d.id AND pg.tipo IS DISTINCT FROM 'Anulación'
    );
    RAISE NOTICE 'Remaining phantom payments: %', remaining;
END;
$$;;
