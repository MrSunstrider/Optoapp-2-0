-- Recalculate monto_pagado for the previously overpaid dispensaciones (corrupted pagos now fixed)
UPDATE public.dispensaciones d
SET monto_pagado = (
    SELECT COALESCE(SUM(monto), 0)
    FROM public.pagos pg
    WHERE pg.dispensacion_id = d.id AND pg.tipo IS DISTINCT FROM 'Anulación'
)
WHERE d.id IN ('cfffe1e0-af02-401b-8058-95325f7736a8', '2bd9c8ca-6ec2-4338-a062-fb5399437c73');;
