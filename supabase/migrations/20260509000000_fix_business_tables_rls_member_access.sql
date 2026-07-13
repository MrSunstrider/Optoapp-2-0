-- Fix business table RLS policies: allow all optica members to SELECT/INSERT/UPDATE,
-- restrict DELETE to admin/gerente.
-- This replaces the overly-restrictive 20260507000000 which blocked non-admin roles from syncing.

create or replace function app_private.is_optica_member(
  p_user_id uuid,
  p_optica_id text
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = p_user_id
      and uo.optica_id = p_optica_id
  );
$$;
revoke all on function app_private.is_optica_member(uuid, text) from public;
grant execute on function app_private.is_optica_member(uuid, text) to authenticated, service_role;
-- PACIENTES
drop policy if exists pacientes_select on public.pacientes;
create policy pacientes_select on public.pacientes for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pacientes_insert on public.pacientes;
create policy pacientes_insert on public.pacientes for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pacientes_update on public.pacientes;
create policy pacientes_update on public.pacientes for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pacientes_delete on public.pacientes;
create policy pacientes_delete on public.pacientes for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
-- EVALUACIONES
drop policy if exists evaluaciones_select on public.evaluaciones;
create policy evaluaciones_select on public.evaluaciones for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists evaluaciones_insert on public.evaluaciones;
create policy evaluaciones_insert on public.evaluaciones for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists evaluaciones_update on public.evaluaciones;
create policy evaluaciones_update on public.evaluaciones for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists evaluaciones_delete on public.evaluaciones;
create policy evaluaciones_delete on public.evaluaciones for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
-- DISPENSACIONES
drop policy if exists dispensaciones_select on public.dispensaciones;
create policy dispensaciones_select on public.dispensaciones for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists dispensaciones_insert on public.dispensaciones;
create policy dispensaciones_insert on public.dispensaciones for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists dispensaciones_update on public.dispensaciones;
create policy dispensaciones_update on public.dispensaciones for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists dispensaciones_delete on public.dispensaciones;
create policy dispensaciones_delete on public.dispensaciones for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
-- SERVICIOS_EXTRA
drop policy if exists servicios_extra_select on public.servicios_extra;
create policy servicios_extra_select on public.servicios_extra for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists servicios_extra_insert on public.servicios_extra;
create policy servicios_extra_insert on public.servicios_extra for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists servicios_extra_update on public.servicios_extra;
create policy servicios_extra_update on public.servicios_extra for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists servicios_extra_delete on public.servicios_extra;
create policy servicios_extra_delete on public.servicios_extra for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
-- PAGOS
drop policy if exists pagos_select on public.pagos;
create policy pagos_select on public.pagos for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pagos_insert on public.pagos;
create policy pagos_insert on public.pagos for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pagos_update on public.pagos;
create policy pagos_update on public.pagos for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));
drop policy if exists pagos_delete on public.pagos;
create policy pagos_delete on public.pagos for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
