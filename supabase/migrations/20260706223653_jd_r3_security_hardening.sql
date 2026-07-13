-- FIX #1: Revoke PUBLIC/anon EXECUTE from backfill migration function
REVOKE EXECUTE ON FUNCTION public.backfill_pagos_venta_id_v2() FROM PUBLIC, anon, authenticated;

-- FIX #2: Revoke PUBLIC/anon EXECUTE from trigger functions (should never be callable via REST)
REVOKE EXECUTE ON FUNCTION public.fn_upsert_venta_from_dispensacion() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.fn_upsert_venta_from_servicio_extra() FROM PUBLIC, anon, authenticated;

-- Revoke authenticated EXECUTE from trigger-only SECURITY DEFINER functions
REVOKE EXECUTE ON FUNCTION public.enforce_optica_limit_for_creator() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.enforce_dev_owner_guard() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.enforce_dev_owner_membership_guard() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.guard_pacientes_delete() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.guard_opticas_fiscal_update() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.guard_opticas_business_profile_optional_update() FROM authenticated;

-- FIX W-4: Add SET search_path to functions that are missing it
ALTER FUNCTION public.trg_pagos_set_venta_id() SET search_path = public;
ALTER FUNCTION public.trg_pagos_update_monto_pagado() SET search_path = public;
ALTER FUNCTION public.set_updated_audit_fields() SET search_path = public;;
