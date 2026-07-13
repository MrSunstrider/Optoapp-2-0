INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id, fecha,
                            fecha_entrega, monto_total, estado)
SELECT
    'v_disp_' || id,
    optica_id,
    'dispensacion',
    id,
    COALESCE(paciente_id, ''),
    fecha,
    fecha_entrega,
    monto_total,
    estado_entrega
FROM public.dispensaciones
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id, fecha,
                            fecha_entrega, monto_total, estado)
SELECT
    'v_serv_' || id,
    optica_id,
    'servicio_extra',
    id,
    COALESCE(paciente_id, ''),
    fecha,
    fecha_entrega,
    monto_total,
    estado
FROM public.servicios_extra
ON CONFLICT (id) DO NOTHING;

ALTER TABLE public.pagos ADD COLUMN IF NOT EXISTS venta_id TEXT;

CREATE INDEX IF NOT EXISTS idx_pagos_venta ON public.pagos (venta_id);

UPDATE public.pagos
SET venta_id = 'v_disp_' || dispensacion_id
WHERE dispensacion_id IS NOT NULL
  AND venta_id IS NULL;

UPDATE public.pagos
SET venta_id = 'v_serv_' || servicio_extra_id
WHERE servicio_extra_id IS NOT NULL
  AND venta_id IS NULL;;
