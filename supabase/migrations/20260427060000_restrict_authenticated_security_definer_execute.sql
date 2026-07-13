-- Security hardening phase 2:
-- Keep authenticated execute only for functions required by app RPC or RLS helper policies.
-- Required:
--   - assert_backup_operation_allowed(text,text,text)  (app RPC)
--   - has_optica_role(uuid,text,text[])                (RLS helper)
--   - is_internal_owner()                              (RLS helper)
-- Revoke authenticated execute from remaining SECURITY DEFINER functions.

revoke execute on function public.assign_optica_role_by_email(text, text, text) from authenticated;
revoke execute on function public.enforce_admin_role_assignment_guard() from authenticated;
revoke execute on function public.enforce_dev_owner_guard() from authenticated;
revoke execute on function public.enforce_dev_owner_membership_guard() from authenticated;
revoke execute on function public.enforce_optica_limit_for_creator() from authenticated;
revoke execute on function public.guard_opticas_business_profile_optional_update() from authenticated;
revoke execute on function public.guard_opticas_fiscal_update() from authenticated;
revoke execute on function public.guard_pacientes_delete() from authenticated;
revoke execute on function public.opticas_lock_plan_from_clients() from authenticated;
revoke execute on function public.rls_auto_enable() from authenticated;
revoke execute on function public.sync_user_profiles_from_auth() from authenticated;
