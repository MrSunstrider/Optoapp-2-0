-- costos-margenes-reales / S1 — server schema foundation (additive).
-- Part 1: per-optica servicio_tipos catalog. Ids are client-generated TEXT UUIDs
-- because the Android app creates rows offline.

CREATE TABLE IF NOT EXISTS public.servicio_tipos (
    id            TEXT PRIMARY KEY,
    optica_id     TEXT NOT NULL REFERENCES public.opticas(id),
    nombre        TEXT NOT NULL CHECK (length(btrim(nombre)) > 0),
    costo_propio  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_propio >= 0),
    costo_tercero NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_tercero >= 0),
    activo        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by    TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_servicio_tipos_optica_nombre
    ON public.servicio_tipos (optica_id, lower(nombre));

ALTER TABLE public.servicio_tipos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS servicio_tipos_select ON public.servicio_tipos;
CREATE POLICY servicio_tipos_select ON public.servicio_tipos FOR SELECT TO authenticated
    USING (app_private.is_optica_member((SELECT auth.uid()), optica_id));

DROP POLICY IF EXISTS servicio_tipos_insert ON public.servicio_tipos;
CREATE POLICY servicio_tipos_insert ON public.servicio_tipos FOR INSERT TO authenticated
    WITH CHECK (app_private.has_optica_role((SELECT auth.uid()), optica_id, ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS servicio_tipos_update ON public.servicio_tipos;
CREATE POLICY servicio_tipos_update ON public.servicio_tipos FOR UPDATE TO authenticated
    USING (app_private.has_optica_role((SELECT auth.uid()), optica_id, ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role((SELECT auth.uid()), optica_id, ARRAY['admin', 'gerente']));

-- No DELETE policy on purpose: types are deactivated (activo = false) so past
-- servicios keep resolving their name; operators delete with service_role.

DROP TRIGGER IF EXISTS trg_servicio_tipos_set_updated_audit ON public.servicio_tipos;
CREATE TRIGGER trg_servicio_tipos_set_updated_audit
    BEFORE UPDATE ON public.servicio_tipos
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_audit_fields();

-- Supabase default privileges grant ALL on new public tables to anon and
-- authenticated; revoke first so DELETE is not left granted by default.
REVOKE ALL ON public.servicio_tipos FROM PUBLIC, anon, authenticated;
GRANT SELECT, INSERT, UPDATE ON public.servicio_tipos TO authenticated;
GRANT ALL ON public.servicio_tipos TO service_role;

-- Part 2: cost snapshot columns. Every new column is additive with a default or
-- NULL so rows written by older clients keep inserting unchanged.

ALTER TABLE public.servicios_extra
    ADD COLUMN IF NOT EXISTS servicio_tipo_id TEXT REFERENCES public.servicio_tipos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS realizado_por TEXT CHECK (realizado_por IN ('propio', 'tercero')),
    ADD COLUMN IF NOT EXISTS realizado_por_nombre TEXT,
    ADD COLUMN IF NOT EXISTS costo_real NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_real >= 0);

CREATE INDEX IF NOT EXISTS idx_servicios_extra_servicio_tipo
    ON public.servicios_extra (servicio_tipo_id);

-- Frame cost lives on the item, labor cost on the servicio: keeping them apart
-- avoids counting the frame twice in the daily cost.
ALTER TABLE public.servicio_extra_items
    ADD COLUMN IF NOT EXISTS costo_real NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_real >= 0);

ALTER TABLE public.dispensacion_items
    ADD COLUMN IF NOT EXISTS costo_origen TEXT CHECK (costo_origen IN ('matriz', 'manual', 'mixto', 'faltante'));

-- Nullable on purpose and never backfilled: NULL resolves by category default
-- on both server and client, so older clients that omit the key keep working.
ALTER TABLE public.gastos_operativos
    ADD COLUMN IF NOT EXISTS tipo_gasto TEXT CHECK (tipo_gasto IN ('fijo', 'variable'));

-- Audit columns give the matrices a last-write-wins timestamp for sync. The
-- shared trigger keeps a client-supplied updated_at and only fills NULLs.
ALTER TABLE public.costos_lc
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_by TEXT;
ALTER TABLE public.costos_productos
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_by TEXT;
ALTER TABLE public.costos_biselado
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_by TEXT;

DROP TRIGGER IF EXISTS trg_costos_lc_set_updated_audit ON public.costos_lc;
CREATE TRIGGER trg_costos_lc_set_updated_audit
    BEFORE UPDATE ON public.costos_lc
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_audit_fields();

DROP TRIGGER IF EXISTS trg_costos_productos_set_updated_audit ON public.costos_productos;
CREATE TRIGGER trg_costos_productos_set_updated_audit
    BEFORE UPDATE ON public.costos_productos
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_audit_fields();

DROP TRIGGER IF EXISTS trg_costos_biselado_set_updated_audit ON public.costos_biselado;
CREATE TRIGGER trg_costos_biselado_set_updated_audit
    BEFORE UPDATE ON public.costos_biselado
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_audit_fields();