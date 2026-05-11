CREATE TABLE IF NOT EXISTS public.invitaciones (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    optica_id text REFERENCES public.opticas(id) ON DELETE CASCADE,
    codigo text NOT NULL UNIQUE,
    rol text NOT NULL DEFAULT 'colaborador',
    creado_por uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    usado_por uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    usado_en timestamptz,
    expira_en timestamptz NOT NULL DEFAULT (now() + interval '7 days'),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_invitaciones_codigo ON public.invitaciones (codigo);
CREATE INDEX IF NOT EXISTS idx_invitaciones_optica ON public.invitaciones (optica_id);

ALTER TABLE public.invitaciones ENABLE ROW LEVEL SECURITY;

CREATE POLICY "invitaciones_service_role_all" ON public.invitaciones
    FOR ALL TO service_role USING (true) WITH CHECK (true);
