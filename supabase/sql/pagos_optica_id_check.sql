-- Ejecutar en Supabase: SQL Editor (no puedo conectar desde el asistente).
-- Objetivo: que la tabla public.pagos tenga optica_id coherente para filtros y RLS.

-- 1) Ver si existe la columna
-- SELECT column_name, data_type
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'pagos';

-- 2) Si falta optica_id, añadirla y rellenar desde dispensaciones / servicios_extra
-- ALTER TABLE public.pagos ADD COLUMN IF NOT EXISTS optica_id text;

-- UPDATE public.pagos p
-- SET optica_id = d.optica_id
-- FROM public.dispensaciones d
-- WHERE p.dispensacion_id = d.id AND (p.optica_id IS NULL OR p.optica_id = '');

-- UPDATE public.pagos p
-- SET optica_id = s.optica_id
-- FROM public.servicios_extra s
-- WHERE p.servicio_extra_id = s.id AND (p.optica_id IS NULL OR p.optica_id = '');

-- CREATE INDEX IF NOT EXISTS idx_pagos_optica_id ON public.pagos (optica_id);

-- 3) RLS de ejemplo (ajusta nombres de claims según tu auth.users / JWT)
-- ALTER TABLE public.pagos ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "pagos_select_optica" ON public.pagos
--   FOR SELECT USING (optica_id = (auth.jwt() -> 'user_metadata' ->> 'optica_id'));
-- CREATE POLICY "pagos_write_optica" ON public.pagos
--   FOR ALL USING (optica_id = (auth.jwt() -> 'user_metadata' ->> 'optica_id'));
