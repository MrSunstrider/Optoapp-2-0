-- Security hardening:
-- Remove EXECUTE from PUBLIC on SECURITY DEFINER functions.
-- Keep explicit EXECUTE for authenticated and service_role to avoid app breakage.

revoke execute on function public.assert_backup_operation_allowed(text, text, text) from public;
grant execute on function public.assert_backup_operation_allowed(text, text, text) to authenticated, service_role;
revoke execute on function public.assign_optica_role_by_email(text, text, text) from public;
grant execute on function public.assign_optica_role_by_email(text, text, text) to authenticated, service_role;
revoke execute on function public.enforce_admin_role_assignment_guard() from public;
grant execute on function public.enforce_admin_role_assignment_guard() to authenticated, service_role;
revoke execute on function public.enforce_dev_owner_guard() from public;
grant execute on function public.enforce_dev_owner_guard() to authenticated, service_role;
revoke execute on function public.enforce_dev_owner_membership_guard() from public;
grant execute on function public.enforce_dev_owner_membership_guard() to authenticated, service_role;
revoke execute on function public.enforce_optica_limit_for_creator() from public;
grant execute on function public.enforce_optica_limit_for_creator() to authenticated, service_role;
revoke execute on function public.guard_opticas_business_profile_optional_update() from public;
grant execute on function public.guard_opticas_business_profile_optional_update() to authenticated, service_role;
revoke execute on function public.guard_opticas_fiscal_update() from public;
grant execute on function public.guard_opticas_fiscal_update() to authenticated, service_role;
revoke execute on function public.guard_pacientes_delete() from public;
grant execute on function public.guard_pacientes_delete() to authenticated, service_role;
revoke execute on function public.has_optica_role(uuid, text, text[]) from public;
grant execute on function public.has_optica_role(uuid, text, text[]) to authenticated, service_role;
revoke execute on function public.is_internal_owner() from public;
grant execute on function public.is_internal_owner() to authenticated, service_role;
revoke execute on function public.opticas_lock_plan_from_clients() from public;
grant execute on function public.opticas_lock_plan_from_clients() to authenticated, service_role;
revoke execute on function public.rls_auto_enable() from public;
grant execute on function public.rls_auto_enable() to authenticated, service_role;
revoke execute on function public.sync_user_profiles_from_auth() from public;
grant execute on function public.sync_user_profiles_from_auth() to authenticated, service_role;
