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
