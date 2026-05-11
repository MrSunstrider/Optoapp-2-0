-- Seguridad: evitar que la vista se ejecute con privilegios del creador.
-- Forzamos SECURITY INVOKER para que respeten permisos/RLS del usuario que consulta.

alter view public.optica_members set (security_invoker = true);
