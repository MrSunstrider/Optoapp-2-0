-- Security hardening (low risk):
-- Revoke EXECUTE from anon on SECURITY DEFINER functions.
-- Keeps authenticated execution unchanged to avoid breaking app flows.

revoke execute on function public.assert_backup_operation_allowed(text, text, text) from anon;
revoke execute on function public.assign_optica_role_by_email(text, text, text) from anon;
revoke execute on function public.enforce_admin_role_assignment_guard() from anon;
revoke execute on function public.enforce_dev_owner_guard() from anon;
revoke execute on function public.enforce_dev_owner_membership_guard() from anon;
revoke execute on function public.enforce_optica_limit_for_creator() from anon;
revoke execute on function public.guard_opticas_business_profile_optional_update() from anon;
revoke execute on function public.guard_opticas_fiscal_update() from anon;
revoke execute on function public.guard_pacientes_delete() from anon;
revoke execute on function public.has_optica_role(uuid, text, text[]) from anon;
revoke execute on function public.is_internal_owner() from anon;
revoke execute on function public.opticas_lock_plan_from_clients() from anon;
-- rls_auto_enable is a Supabase-managed function; it may not exist in local/CI/preview environments.
-- The revoke is only needed where the function exists (production Supabase).
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid WHERE n.nspname = 'public' AND p.proname = 'rls_auto_enable') THEN
    EXECUTE 'revoke execute on function public.rls_auto_enable() from anon';
  END IF;
END;
$$;
revoke execute on function public.sync_user_profiles_from_auth() from anon;
