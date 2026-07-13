-- Drop the old text-parameter overload (no SECURITY DEFINER, no search_path)
DROP FUNCTION IF EXISTS public.paciente_eliminaciones_restantes_hoy(text);;
