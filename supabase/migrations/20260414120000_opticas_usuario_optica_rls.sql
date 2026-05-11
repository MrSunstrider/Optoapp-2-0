-- OptoApp: ópticas, membresía N:N y RLS por pertenencia.
-- Ejecutar con supabase db push o pegar en SQL Editor tras revisar.

-- ---------------------------------------------------------------------------
-- 1) Tablas de control
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.opticas (
    id          TEXT PRIMARY KEY,
    nombre      TEXT NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.usuario_optica (
    user_id     UUID NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
    optica_id   TEXT NOT NULL REFERENCES public.opticas (id) ON DELETE CASCADE,
    rol         TEXT NOT NULL DEFAULT 'admin',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, optica_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_optica_user ON public.usuario_optica (user_id);
CREATE INDEX IF NOT EXISTS idx_usuario_optica_optica ON public.usuario_optica (optica_id);

COMMENT ON TABLE public.opticas IS 'Tenant (óptica); optica_id en tablas de negocio referencia opticas.id';
COMMENT ON TABLE public.usuario_optica IS 'Membresía usuario ↔ óptica con rol';

-- ---------------------------------------------------------------------------
-- 2) Poblar opticas desde datos existentes + fila por defecto
-- ---------------------------------------------------------------------------

INSERT INTO public.opticas (id, nombre)
SELECT DISTINCT optica_id, optica_id
FROM public.pacientes
WHERE optica_id IS NOT NULL AND optica_id <> ''
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.opticas (id, nombre)
VALUES ('mi_optica_base', 'Óptica base (legacy)')
ON CONFLICT (id) DO NOTHING;

-- Vincular cada usuario de auth a su óptica cuando optica_id en datos = uid (modelo antiguo 1:1)
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
SELECT u.id, u.id::text, 'admin'
FROM auth.users u
INNER JOIN public.opticas o ON o.id = u.id::text
ON CONFLICT (user_id, optica_id) DO NOTHING;

-- Nota: si un usuario debe acceder solo a mi_optica_base, insertar manualmente:
-- INSERT INTO usuario_optica (user_id, optica_id, rol) VALUES ('<uuid>','mi_optica_base','admin')
-- ON CONFLICT (user_id, optica_id) DO NOTHING;

-- Asegurar al menos una membresía por usuario que ya tenga filas en pacientes con su uid
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
SELECT DISTINCT u.id, p.optica_id, 'admin'
FROM auth.users u
INNER JOIN public.pacientes p ON p.optica_id = u.id::text
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuario_optica uo WHERE uo.user_id = u.id AND uo.optica_id = p.optica_id
)
ON CONFLICT (user_id, optica_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3) pagos.optica_id + backfill
-- ---------------------------------------------------------------------------

ALTER TABLE public.pagos ADD COLUMN IF NOT EXISTS optica_id TEXT NOT NULL DEFAULT 'mi_optica_base';

UPDATE public.pagos p
SET optica_id = d.optica_id
FROM public.dispensaciones d
WHERE p.dispensacion_id = d.id AND (p.optica_id IS NULL OR p.optica_id = 'mi_optica_base');

UPDATE public.pagos p
SET optica_id = s.optica_id
FROM public.servicios_extra s
WHERE p.servicio_extra_id = s.id AND (p.optica_id IS NULL OR p.optica_id = 'mi_optica_base');

CREATE INDEX IF NOT EXISTS idx_pagos_optica_id ON public.pagos (optica_id);

-- ---------------------------------------------------------------------------
-- 4) RLS: tablas de control
-- ---------------------------------------------------------------------------

ALTER TABLE public.opticas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usuario_optica ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "opticas_select_member" ON public.opticas;
CREATE POLICY "opticas_select_member" ON public.opticas
    FOR SELECT USING (
        id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid())
    );

DROP POLICY IF EXISTS "usuario_optica_select_own" ON public.usuario_optica;
CREATE POLICY "usuario_optica_select_own" ON public.usuario_optica
    FOR SELECT USING (user_id = auth.uid());

DROP POLICY IF EXISTS "usuario_optica_insert_service" ON public.usuario_optica;
-- Solo administración vía SQL/Service role; la app no inserta membresías por ahora

-- ---------------------------------------------------------------------------
-- 5) Eliminar políticas antiguas conocidas en tablas de negocio (ajusta nombres si difieren)
-- ---------------------------------------------------------------------------

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT policyname, tablename FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('pacientes','evaluaciones','dispensaciones','servicios_extra','pagos')
    ) LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', r.policyname, r.tablename);
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 6) Políticas por membresía (una por comando para claridad)
-- ---------------------------------------------------------------------------

-- PACIENTES
CREATE POLICY "pacientes_select" ON public.pacientes FOR SELECT
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pacientes_insert" ON public.pacientes FOR INSERT
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pacientes_update" ON public.pacientes FOR UPDATE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pacientes_delete" ON public.pacientes FOR DELETE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

-- EVALUACIONES
CREATE POLICY "evaluaciones_select" ON public.evaluaciones FOR SELECT
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "evaluaciones_insert" ON public.evaluaciones FOR INSERT
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "evaluaciones_update" ON public.evaluaciones FOR UPDATE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "evaluaciones_delete" ON public.evaluaciones FOR DELETE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

-- DISPENSACIONES
CREATE POLICY "dispensaciones_select" ON public.dispensaciones FOR SELECT
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "dispensaciones_insert" ON public.dispensaciones FOR INSERT
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "dispensaciones_update" ON public.dispensaciones FOR UPDATE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "dispensaciones_delete" ON public.dispensaciones FOR DELETE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

-- SERVICIOS_EXTRA
CREATE POLICY "servicios_extra_select" ON public.servicios_extra FOR SELECT
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "servicios_extra_insert" ON public.servicios_extra FOR INSERT
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "servicios_extra_update" ON public.servicios_extra FOR UPDATE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "servicios_extra_delete" ON public.servicios_extra FOR DELETE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

-- PAGOS
CREATE POLICY "pagos_select" ON public.pagos FOR SELECT
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pagos_insert" ON public.pagos FOR INSERT
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pagos_update" ON public.pagos FOR UPDATE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
    WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
CREATE POLICY "pagos_delete" ON public.pagos FOR DELETE
    USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
