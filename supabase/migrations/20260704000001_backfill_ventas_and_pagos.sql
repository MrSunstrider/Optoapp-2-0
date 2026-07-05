-- Migration: Backfill ventas from existing dispensaciones and servicios_extra
-- Also add venta_id column to pagos for canonical payment reference.
-- Uses ON CONFLICT DO NOTHING for idempotent re-runs.

-- Step 1: Backfill ventas from dispensaciones
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

-- Step 2: Backfill ventas from servicios_extra
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

-- Step 3: Add venta_id column to pagos (coexists with dispensacion_id / servicio_extra_id)
ALTER TABLE public.pagos ADD COLUMN IF NOT EXISTS venta_id TEXT;

CREATE INDEX IF NOT EXISTS idx_pagos_venta ON public.pagos (venta_id);

-- Step 4: Backfill pagos.venta_id for dispensacion payments
UPDATE public.pagos
SET venta_id = 'v_disp_' || dispensacion_id
WHERE dispensacion_id IS NOT NULL
  AND venta_id IS NULL;

-- Step 5: Backfill pagos.venta_id for servicio_extra payments
UPDATE public.pagos
SET venta_id = 'v_serv_' || servicio_extra_id
WHERE servicio_extra_id IS NOT NULL
  AND venta_id IS NULL;
