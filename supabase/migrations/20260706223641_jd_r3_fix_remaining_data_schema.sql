-- Re-run fixes that were inside the failed DO block

-- FIX #5: Correct corrupted pagos
UPDATE public.pagos
SET venta_id = 'v_disp_' || dispensacion_id
WHERE id IN ('6590cfb3-9a28-43d3-b274-e17cefbcb46c', '115067dc-f4b7-4c5b-934c-3257c41ea1d8')
  AND dispensacion_id IS NOT NULL
  AND venta_id LIKE 'v_serv_%';

-- FIX #6: Delete orphan ventas
DELETE FROM public.ventas
WHERE id IN ('v_serv_90580361-6055-4b19-9278-e86488dad8bf', 'v_serv_9401d598-223a-4b90-81a2-3c03d0472ca3')
  AND origen = 'servicio_extra'
  AND NOT EXISTS (SELECT 1 FROM public.servicios_extra s WHERE s.id = origen_id);

-- FIX #4a: FK ventas → opticas
ALTER TABLE public.ventas ADD CONSTRAINT fk_ventas_optica
    FOREIGN KEY (optica_id) REFERENCES public.opticas(id);

-- FIX #4b: FK pagos → ventas
ALTER TABLE public.pagos ADD CONSTRAINT fk_pagos_venta
    FOREIGN KEY (venta_id) REFERENCES public.ventas(id);

-- FIX #12: Drop duplicate index
DROP INDEX IF EXISTS public.idx_arqueo_caja_fecha_optica_id;

-- FIX #16: Drop redundant prefix indexes
DROP INDEX IF EXISTS public.idx_dispensaciones_optica_id;
DROP INDEX IF EXISTS public.idx_servicios_extra_optica_id;
DROP INDEX IF EXISTS public.idx_evaluaciones_optica_id;
DROP INDEX IF EXISTS public.idx_pagos_optica_id;;
