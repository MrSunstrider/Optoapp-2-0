-- Drop the one-off backfill function (already executed, no longer needed)
DROP FUNCTION IF EXISTS public.backfill_pagos_venta_id_v2();

-- rpc_deudores is LANGUAGE sql which can't have SET search_path.
-- It's SECURITY INVOKER and uses fully qualified names, so the risk is minimal.
-- Adding documentation comment.
COMMENT ON FUNCTION public.rpc_deudores(TEXT) IS 'SECURITY INVOKER, uses fully qualified public.* names. LANGUAGE sql does not support SET search_path. Risk is low due to SECURITY INVOKER.';

-- The 3 SECURITY DEFINER functions flagged are intentional:
-- check_rate_limit: needed for PIN brute-force protection
-- create_optica_for_current_user: needed for onboarding flow
-- paciente_eliminaciones_restantes_hoy: needed for delete rate limiting
COMMENT ON FUNCTION public.check_rate_limit(TEXT, INTEGER, INTEGER) IS 'SECURITY DEFINER intentionally — needed for PIN rate limiting. Accesses pin_attempts table.';
COMMENT ON FUNCTION public.create_optica_for_current_user(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) IS 'SECURITY DEFINER intentionally — needed for onboarding. ON CONFLICT DO UPDATE risk mitigated by enforce_admin_role_assignment_guard trigger.';
COMMENT ON FUNCTION public.paciente_eliminaciones_restantes_hoy(UUID) IS 'SECURITY DEFINER intentionally — needed for delete rate limiting.';;
