-- Grant anon USAGE on app_private and EXECUTE on RLS helper functions.
--
-- RLS policies on business tables (dispensaciones, pacientes, monturas,
-- dispensacion_items, evaluaciones, pagos, servicios_extra, etc.) call
-- app_private.has_optica_role() and app_private.is_optica_member() as
-- SECURITY DEFINER helpers. When a request arrives without a valid session
-- (anon role), the RLS policy still fires and needs to execute the helper.
-- Without these grants, anon gets "permission denied for function" instead
-- of a normal RLS denial (false).
--
-- Safety: both functions are STABLE, read-only SQL. For anonymous requests
-- auth.uid() is NULL, so the underlying query against usuario_optica
-- returns no rows -> false. No data exposure risk.

grant usage on schema app_private to anon;

grant execute on function app_private.has_optica_role(uuid, text, text[]) to anon;
grant execute on function app_private.is_optica_member(uuid, text) to anon;
