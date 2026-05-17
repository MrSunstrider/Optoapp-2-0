-- Performance tune for sync telemetry:
-- 1) add covering index for FK last_actor -> auth.users(id)
-- 2) avoid per-row auth.uid() re-evaluation in RLS policies

create index if not exists idx_sync_telemetry_optica_last_actor
on public.sync_telemetry_optica (last_actor);

drop policy if exists sync_telemetry_optica_select_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_select_member
on public.sync_telemetry_optica
for select
to authenticated
using (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = (select auth.uid())
  )
);

drop policy if exists sync_telemetry_optica_insert_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_insert_member
on public.sync_telemetry_optica
for insert
to authenticated
with check (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = (select auth.uid())
  )
);

drop policy if exists sync_telemetry_optica_update_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_update_member
on public.sync_telemetry_optica
for update
to authenticated
using (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = (select auth.uid())
  )
)
with check (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = (select auth.uid())
  )
);
