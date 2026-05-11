-- Performance hardening para cierres_caja y optica_settings.
create index if not exists idx_cierres_caja_closed_by
  on public.cierres_caja (closed_by);

create index if not exists idx_cierres_caja_reopened_by
  on public.cierres_caja (reopened_by);

drop policy if exists cierres_caja_select_member on public.cierres_caja;
drop policy if exists cierres_caja_upsert_admin_manager on public.cierres_caja;

create policy cierres_caja_select_member
on public.cierres_caja
for select
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = (select auth.uid())
  )
);

create policy cierres_caja_insert_admin_manager
on public.cierres_caja
for insert
to authenticated
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

create policy cierres_caja_update_admin_manager
on public.cierres_caja
for update
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

create policy cierres_caja_delete_admin_manager
on public.cierres_caja
for delete
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

drop policy if exists optica_settings_select_member on public.optica_settings;
drop policy if exists optica_settings_upsert_admin_manager on public.optica_settings;

create policy optica_settings_select_member
on public.optica_settings
for select
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = (select auth.uid())
  )
);

create policy optica_settings_insert_admin_manager
on public.optica_settings
for insert
to authenticated
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

create policy optica_settings_update_admin_manager
on public.optica_settings
for update
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

create policy optica_settings_delete_admin_manager
on public.optica_settings
for delete
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = (select auth.uid())
      and lower(uo.rol) in ('admin', 'gerente')
  )
);
