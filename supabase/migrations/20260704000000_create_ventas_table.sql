-- Migration: Create canonical ventas table (ledger financiero)
-- Phase 1 of ingresos-analisis-financiero module
-- This table is the single source of truth for all income calculations.
-- Maintained via triggers on dispensaciones and servicios_extra.

CREATE TABLE IF NOT EXISTS public.ventas (
    id                      TEXT PRIMARY KEY,
    optica_id               TEXT NOT NULL,
    origen                  TEXT NOT NULL CHECK (origen IN ('dispensacion', 'servicio_extra')),
    origen_id               TEXT NOT NULL,
    paciente_id             TEXT NOT NULL DEFAULT '',
    fecha                   DATE NOT NULL,
    fecha_entrega           DATE,
    monto_total             NUMERIC NOT NULL,
    costo_unitario_snapshot NUMERIC,
    estado                  TEXT NOT NULL DEFAULT 'Pendiente'
        CHECK (estado IN ('Pendiente', 'Entregado', 'Anulado')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_by              UUID
);

-- Indexes for frequent query patterns
CREATE INDEX IF NOT EXISTS idx_ventas_optica_fecha
    ON public.ventas (optica_id, fecha);

CREATE INDEX IF NOT EXISTS idx_ventas_origen
    ON public.ventas (origen, origen_id);

CREATE INDEX IF NOT EXISTS idx_ventas_paciente
    ON public.ventas (paciente_id);

-- RLS: enable row-level security
ALTER TABLE public.ventas ENABLE ROW LEVEL SECURITY;

-- SELECT: any member of the optica can read
CREATE POLICY ventas_select ON public.ventas FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

-- INSERT: roles with write access
CREATE POLICY ventas_insert ON public.ventas FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']));

-- UPDATE: roles with write access
CREATE POLICY ventas_update ON public.ventas FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']));

-- DELETE: admin/gerente only
CREATE POLICY ventas_delete ON public.ventas FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente']));
