-- Performance fix for RLS advisor:
-- avoid per-row re-evaluation of auth.uid() in pacientes_delete policy.

drop policy if exists pacientes_delete on public.pacientes;

create policy pacientes_delete on public.pacientes
for delete
using (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = (select auth.uid())
      and self.optica_id = pacientes.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);
