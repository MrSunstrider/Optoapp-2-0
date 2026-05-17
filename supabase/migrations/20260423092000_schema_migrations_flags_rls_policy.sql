-- Seguridad: tabla interna con RLS habilitado pero sin políticas.
-- Definimos política explícita para bloquear acceso desde clientes autenticados.

drop policy if exists schema_migrations_flags_no_client_access on public.schema_migrations_flags;
create policy schema_migrations_flags_no_client_access
on public.schema_migrations_flags
for all
to authenticated
using (false)
with check (false);
