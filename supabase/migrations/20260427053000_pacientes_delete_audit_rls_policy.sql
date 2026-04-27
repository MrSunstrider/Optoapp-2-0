-- Seguridad: la tabla de auditoría de borrados no debe ser accesible desde clientes.
-- Evita warning de "RLS enabled without policy" manteniendo el bloqueo explícito.

drop policy if exists pacientes_delete_audit_no_client_access on public.pacientes_delete_audit;
create policy pacientes_delete_audit_no_client_access
on public.pacientes_delete_audit
for all
to authenticated
using (false)
with check (false);
