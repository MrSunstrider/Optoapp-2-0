-- Tune RLS policies to avoid per-row auth.uid() re-evaluation.

drop policy if exists usuario_optica_select_member_scope on public.usuario_optica;
create policy usuario_optica_select_member_scope on public.usuario_optica
for select
using (
  user_id = (select auth.uid())
  or app_private.has_optica_role((select auth.uid()), optica_id, array['admin', 'gerente'])
);
drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
create policy usuario_optica_insert_admin_optica on public.usuario_optica
for insert to authenticated
with check (
  app_private.has_optica_role((select auth.uid()), optica_id, array['admin', 'gerente'])
  or (
    user_id = (select auth.uid())
    and lower(trim(rol)) = 'admin'
    and not exists (
      select 1
      from public.usuario_optica u
      where u.optica_id = usuario_optica.optica_id
    )
  )
);
drop policy if exists usuario_optica_update_admin_optica on public.usuario_optica;
create policy usuario_optica_update_admin_optica on public.usuario_optica
for update
using (
  app_private.has_optica_role((select auth.uid()), optica_id, array['admin', 'gerente'])
)
with check (
  app_private.has_optica_role((select auth.uid()), optica_id, array['admin', 'gerente'])
);
drop policy if exists "opticas_select_member" on public.opticas;
create policy "opticas_select_member" on public.opticas
for select
using (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = (select auth.uid()))
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
);
drop policy if exists "opticas_update_member" on public.opticas;
create policy "opticas_update_member" on public.opticas
for update
using (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = (select auth.uid()))
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
)
with check (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = (select auth.uid()))
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
);
drop policy if exists opticas_insert_authenticated on public.opticas;
create policy opticas_insert_authenticated on public.opticas
for insert to authenticated
with check (
  (select auth.uid()) is not null
  and nullif(btrim(id), '') is not null
  and nullif(btrim(nombre), '') is not null
  and (
    (
      lower(btrim(plan_code)) = 'free'
      and lower(btrim(plan_source)) = 'manual'
      and lower(btrim(plan_status)) = 'active'
    )
    or (
      lower(btrim(plan_code)) = 'dev_owner'
      and app_private.is_internal_owner()
    )
  )
);
