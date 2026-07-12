-- Costos y Gastos — matrix cost table for per-eye/unit/pair cost lookup
-- + biselado cost table + new columns on dispensaciones / dispensacion_items
-- See openspec/changes/costos-y-gastos/design.md D1

-- DROP old flat costos_productos schema (safe: 0 rows pre-production, no RPCs reference it)
DROP TABLE IF EXISTS public.costos_productos CASCADE;

-- Recreate with matrix columns
CREATE TABLE IF NOT EXISTS public.costos_productos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id         TEXT NOT NULL REFERENCES public.opticas(id),
    material          TEXT NOT NULL,
    tipo_lente        TEXT NOT NULL,
    stock_o_fabricacion TEXT NOT NULL CHECK (stock_o_fabricacion IN ('stock','fabricacion','montura')),
    tratamiento       TEXT,
    serie             INTEGER,
    costo_unitario    NUMERIC NOT NULL,
    laboratorio_id    TEXT,
    vigente_desde     DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta     DATE
);

CREATE INDEX IF NOT EXISTS idx_costos_productos_lookup
    ON public.costos_productos (optica_id, material, tipo_lente, stock_o_fabricacion, serie)
    WHERE vigente_hasta IS NULL;

-- New costos_biselado table
CREATE TABLE IF NOT EXISTS public.costos_biselado (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id         TEXT NOT NULL REFERENCES public.opticas(id),
    material          TEXT NOT NULL,
    tipo_aro          TEXT NOT NULL CHECK (tipo_aro IN ('aro_completo','ranurado','al_aire','taladro')),
    stock_o_fabricacion TEXT NOT NULL CHECK (stock_o_fabricacion IN ('stock','fabricacion')),
    serie             INTEGER,
    alto_indice       TEXT,
    costo_por_par     NUMERIC NOT NULL,
    proveedor         TEXT,
    vigente_desde     DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta     DATE
);

-- RLS for costos_productos (gastos_operativos pattern)
ALTER TABLE public.costos_productos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS costos_productos_select ON public.costos_productos;
CREATE POLICY costos_productos_select ON public.costos_productos FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS costos_productos_insert ON public.costos_productos;
CREATE POLICY costos_productos_insert ON public.costos_productos FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_productos_update ON public.costos_productos;
CREATE POLICY costos_productos_update ON public.costos_productos FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_productos_delete ON public.costos_productos;
CREATE POLICY costos_productos_delete ON public.costos_productos FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin']));

-- RLS for costos_biselado (same pattern)
ALTER TABLE public.costos_biselado ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS costos_biselado_select ON public.costos_biselado;
CREATE POLICY costos_biselado_select ON public.costos_biselado FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS costos_biselado_insert ON public.costos_biselado;
CREATE POLICY costos_biselado_insert ON public.costos_biselado FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_biselado_update ON public.costos_biselado;
CREATE POLICY costos_biselado_update ON public.costos_biselado FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_biselado_delete ON public.costos_biselado;
CREATE POLICY costos_biselado_delete ON public.costos_biselado FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin']));

-- ALTER existing tables for cost tracking
ALTER TABLE public.dispensaciones ADD COLUMN IF NOT EXISTS evaluacion_id TEXT;

ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS alto_indice TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS reduccion_diametro TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS lenticular TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS curva_base TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_od NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_oi NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_montura NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_biselado NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_lc NUMERIC;
