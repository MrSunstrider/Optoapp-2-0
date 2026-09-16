-- servicio_extra_items + regalos_servicio_extra for multi-product and gifts on servicios_extra
-- Mirrors dispensacion_items / regalos_dispensacion patterns with optica_id RLS.

CREATE TABLE IF NOT EXISTS public.servicio_extra_items (
    id TEXT PRIMARY KEY,
    servicio_extra_id TEXT NOT NULL REFERENCES public.servicios_extra(id) ON DELETE CASCADE,
    montura_id TEXT,
    descripcion TEXT NOT NULL DEFAULT '',
    monto NUMERIC NOT NULL DEFAULT 0,
    optica_id TEXT NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_servicio_extra_items_servicio
    ON public.servicio_extra_items(servicio_extra_id);
CREATE INDEX IF NOT EXISTS idx_servicio_extra_items_optica
    ON public.servicio_extra_items(optica_id);

ALTER TABLE public.servicio_extra_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "servicio_extra_items_select" ON public.servicio_extra_items
FOR SELECT USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY "servicio_extra_items_insert" ON public.servicio_extra_items
FOR INSERT WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
    AND servicio_extra_id IN (
        SELECT id FROM public.servicios_extra WHERE optica_id = servicio_extra_items.optica_id
    )
);

CREATE POLICY "servicio_extra_items_update" ON public.servicio_extra_items
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
)
WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
    AND servicio_extra_id IN (
        SELECT id FROM public.servicios_extra WHERE optica_id = servicio_extra_items.optica_id
    )
);

CREATE POLICY "servicio_extra_items_delete" ON public.servicio_extra_items
FOR DELETE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.servicio_extra_items TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.servicio_extra_items TO service_role;

CREATE TABLE IF NOT EXISTS public.regalos_servicio_extra (
    id TEXT PRIMARY KEY,
    servicio_extra_id TEXT NOT NULL REFERENCES public.servicios_extra(id) ON DELETE CASCADE,
    producto_id TEXT NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    costo_unitario NUMERIC NOT NULL DEFAULT 0,
    descripcion TEXT NOT NULL DEFAULT '',
    motivo TEXT NOT NULL DEFAULT '',
    optica_id TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_regalos_servicio_extra_servicio
    ON public.regalos_servicio_extra(servicio_extra_id);
CREATE INDEX IF NOT EXISTS idx_regalos_servicio_extra_optica
    ON public.regalos_servicio_extra(optica_id);

ALTER TABLE public.regalos_servicio_extra ENABLE ROW LEVEL SECURITY;

CREATE POLICY "regalos_servicio_extra_select" ON public.regalos_servicio_extra
FOR SELECT USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY "regalos_servicio_extra_insert" ON public.regalos_servicio_extra
FOR INSERT WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
    AND servicio_extra_id IN (
        SELECT id FROM public.servicios_extra WHERE optica_id = regalos_servicio_extra.optica_id
    )
);

CREATE POLICY "regalos_servicio_extra_update" ON public.regalos_servicio_extra
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
)
WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
    AND servicio_extra_id IN (
        SELECT id FROM public.servicios_extra WHERE optica_id = regalos_servicio_extra.optica_id
    )
);

CREATE POLICY "regalos_servicio_extra_delete" ON public.regalos_servicio_extra
FOR DELETE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.regalos_servicio_extra TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.regalos_servicio_extra TO service_role;

-- Backfill one item per existing servicio (item id = servicio id when montura linked for stock refs)
INSERT INTO public.servicio_extra_items (id, servicio_extra_id, montura_id, descripcion, monto, optica_id, updated_at, updated_by)
SELECT
    CASE
        WHEN montura_id IS NOT NULL AND TRIM(montura_id) <> '' THEN id
        ELSE id || ':item'
    END,
    id,
    NULLIF(TRIM(COALESCE(montura_id, '')), ''),
    COALESCE(descripcion, ''),
    COALESCE(monto_total, 0),
    optica_id,
    updated_at,
    updated_by
FROM public.servicios_extra
ON CONFLICT (id) DO NOTHING;
