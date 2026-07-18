-- Create costos_lc table for contact lens cost matrix
-- Replaces the hack of using costos_productos with stock_o_fabricacion='lente_contacto'

CREATE TABLE IF NOT EXISTS public.costos_lc (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id       TEXT NOT NULL REFERENCES public.opticas(id),
    tipo_lc         TEXT NOT NULL CHECK (tipo_lc IN ('cosmetico', 'graduado', 'terapeutico')),
    material_lc     TEXT NOT NULL,
    modalidad       TEXT NOT NULL CHECK (modalidad IN ('diario', 'quincenal', 'mensual', 'anual')),
    radio_base      TEXT,
    diametro        TEXT,
    laboratorio_id  TEXT,
    costo_unitario  NUMERIC NOT NULL,
    vigente_desde   DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta   DATE
);

CREATE INDEX IF NOT EXISTS idx_costos_lc_lookup
    ON public.costos_lc (optica_id, tipo_lc, material_lc, modalidad)
    WHERE vigente_hasta IS NULL;

-- RLS
ALTER TABLE public.costos_lc ENABLE ROW LEVEL SECURITY;

CREATE POLICY costos_lc_select ON public.costos_lc FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY costos_lc_insert ON public.costos_lc FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

CREATE POLICY costos_lc_update ON public.costos_lc FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

CREATE POLICY costos_lc_delete ON public.costos_lc FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin']));
