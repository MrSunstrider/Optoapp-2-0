-- Reduce SECURITY DEFINER exposure in exposed schema (public):
-- - Move RLS helper functions to a private schema not exposed by PostgREST RPC.
-- - Keep backup guard callable by app RPC but as SECURITY INVOKER.

create schema if not exists app_private;
revoke all on schema app_private from public;
grant usage on schema app_private to authenticated, service_role;
create or replace function app_private.has_optica_role(
  p_user_id uuid,
  p_optica_id text,
  p_roles text[] default array['admin', 'gerente']
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
      and lower(trim(uo.rol)) = any (p_roles)
  );
$$;
create or replace function app_private.is_internal_owner()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.user_profiles up
    where up.user_id = auth.uid()
      and lower(trim(up.email)) = 'jaermadera@gmail.com'
  );
$$;
revoke all on function app_private.has_optica_role(uuid, text, text[]) from public;
revoke all on function app_private.is_internal_owner() from public;
grant execute on function app_private.has_optica_role(uuid, text, text[]) to authenticated, service_role;
grant execute on function app_private.is_internal_owner() to authenticated, service_role;
-- Keep public wrappers for backward compatibility in SQL references while delegating.
create or replace function public.has_optica_role(
  p_user_id uuid,
  p_optica_id text,
  p_roles text[] default array['admin', 'gerente']
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select app_private.has_optica_role(p_user_id, p_optica_id, p_roles);
$$;
create or replace function public.is_internal_owner()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select app_private.is_internal_owner();
$$;
-- Remove direct RPC exposure of public SECURITY DEFINER helpers.
revoke execute on function public.has_optica_role(uuid, text, text[]) from authenticated;
revoke execute on function public.is_internal_owner() from authenticated;
grant execute on function public.has_optica_role(uuid, text, text[]) to service_role;
grant execute on function public.is_internal_owner() to service_role;
-- Recreate policies to use private-schema helpers explicitly.
drop policy if exists usuario_optica_select_member_scope on public.usuario_optica;
create policy usuario_optica_select_member_scope on public.usuario_optica
for select
using (
  user_id = auth.uid()
  or app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
);
drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
create policy usuario_optica_insert_admin_optica on public.usuario_optica
for insert to authenticated
with check (
  app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
  or (
    user_id = auth.uid()
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
  app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
)
with check (
  app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
);
drop policy if exists "opticas_select_member" on public.opticas;
create policy "opticas_select_member" on public.opticas
for select
using (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
);
drop policy if exists "opticas_update_member" on public.opticas;
create policy "opticas_update_member" on public.opticas
for update
using (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
)
with check (
  id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
  and (
    lower(trim(plan_code)) <> 'dev_owner'
    or app_private.is_internal_owner()
  )
);
drop policy if exists opticas_insert_authenticated on public.opticas;
create policy opticas_insert_authenticated on public.opticas
for insert to authenticated
with check (
  auth.uid() is not null
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
create or replace function public.enforce_dev_owner_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' and not app_private.is_internal_owner() then
    raise exception 'El plan dev_owner está restringido al owner interno';
  end if;

  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' then
    new.plan_source := 'internal';
  end if;

  return new;
end;
$$;
create or replace function public.enforce_dev_owner_membership_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_is_dev_owner_optica boolean;
begin
  select exists (
    select 1
    from public.opticas o
    where o.id = new.optica_id
      and lower(trim(o.plan_code)) = 'dev_owner'
  ) into v_is_dev_owner_optica;

  if not v_is_dev_owner_optica then
    return new;
  end if;

  if not app_private.is_internal_owner() or new.user_id <> auth.uid() then
    raise exception 'Membresía restringida: la óptica dev_owner solo puede pertenecer al owner interno';
  end if;

  return new;
end;
$$;
-- Backup guard can remain exposed as RPC but no longer SECURITY DEFINER.
create or replace function public.assert_backup_operation_allowed(
  p_action text,
  p_source_optica_id text,
  p_target_optica_id text
)
returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_uid uuid;
  v_is_admin_target boolean;
  v_is_admin_source boolean;
  v_action text := lower(btrim(coalesce(p_action, '')));
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sesión inválida.';
  end if;

  if v_action not in ('export', 'restore') then
    raise exception 'Acción no válida: %', p_action;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = v_uid
      and uo.optica_id = p_target_optica_id
      and lower(trim(uo.rol)) = 'admin'
  ) into v_is_admin_target;

  if not v_is_admin_target then
    raise exception 'Solo admin de la óptica activa puede realizar esta operación.';
  end if;

  if v_action = 'restore' then
    if btrim(coalesce(p_source_optica_id, '')) = '' then
      raise exception 'Respaldo sin óptica de origen.';
    end if;

    if p_source_optica_id <> p_target_optica_id then
      raise exception 'No se permite restaurar respaldos de otra óptica.';
    end if;

    select exists (
      select 1
      from public.usuario_optica uo
      where uo.user_id = v_uid
        and uo.optica_id = p_source_optica_id
        and lower(trim(uo.rol)) = 'admin'
    ) into v_is_admin_source;

    if not v_is_admin_source then
      raise exception 'Sin permisos admin sobre la óptica origen del respaldo.';
    end if;
  end if;
end;
$$;
