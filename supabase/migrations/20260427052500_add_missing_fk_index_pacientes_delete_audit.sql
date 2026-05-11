-- Safe performance hardening:
-- Adds the missing covering index for FK pacientes_delete_audit.deleted_by -> auth.users.id
-- This is non-breaking and reduces lock/contention risk on deletes/updates in auth.users.

create index if not exists idx_pacientes_delete_audit_deleted_by
on public.pacientes_delete_audit (deleted_by);
