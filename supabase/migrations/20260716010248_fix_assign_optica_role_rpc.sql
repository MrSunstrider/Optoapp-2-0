-- Fix: Re-grant EXECUTE on assign_optica_role_by_email to authenticated
-- Root cause: Migration 20260427060000 revoked this permission,
-- but the function has internal role verification (only admin/gerente can assign),
-- so re-granting is safe.
GRANT EXECUTE ON FUNCTION public.assign_optica_role_by_email(
    p_optica_id text,
    p_email text,
    p_rol text
) TO authenticated;
