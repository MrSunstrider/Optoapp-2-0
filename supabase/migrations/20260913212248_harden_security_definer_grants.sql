-- Harden EXECUTE grants on SECURITY DEFINER RPCs flagged by advisors 0028/0029.
--
-- Fixes:
-- 1) paciente_eliminaciones_restantes_hoy — stale EXECUTE for anon (membership-gated; clients must be signed in)
-- 2) recalcular_resumen_diario_admin — no membership guard; service_role only (unused by app clients)
--
-- Intentional (kept, still warned by 0028/0029):
--   check_rate_limit → anon (pre-auth PIN throttle for web companion)
--   create_optica_for_current_user / recalcular_resumen_diario /
--   paciente_eliminaciones_restantes_hoy / rpc_adjust_montura_stock → authenticated

-- 1. paciente_eliminaciones_restantes_hoy: authenticated + service_role only
REVOKE ALL ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) TO service_role;

-- 2. recalcular_resumen_diario_admin: service_role only (no tenant membership check)
REVOKE ALL ON FUNCTION public.recalcular_resumen_diario_admin(text, date) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.recalcular_resumen_diario_admin(text, date) FROM anon;
REVOKE ALL ON FUNCTION public.recalcular_resumen_diario_admin(text, date) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario_admin(text, date) TO service_role;

-- 3. Drop accidental PUBLIC default on check_rate_limit; keep intentional anon + authenticated
REVOKE ALL ON FUNCTION public.check_rate_limit(text, integer, integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO anon;
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO service_role;

COMMENT ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) IS
  'SECURITY DEFINER intentional — delete-rate RPC for authenticated members; EXECUTE revoked from anon.';
COMMENT ON FUNCTION public.recalcular_resumen_diario_admin(text, date) IS
  'SECURITY DEFINER admin backfill without membership guard — EXECUTE service_role only.';
COMMENT ON FUNCTION public.check_rate_limit(text, integer, integer) IS
  'SECURITY DEFINER intentional — PIN rate limit; EXECUTE granted to anon for pre-auth web login.';
