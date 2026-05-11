-- Endurece visibilidad/control del plan interno dev_owner para una cuenta dueña.
-- Solo el owner interno puede ver/editar ópticas dev_owner y gestionar su membresía.

create or replace function public.is_internal_owner()
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

drop policy if exists "opticas_select_member" on public.opticas;
create policy "opticas_select_member" on public.opticas
    for select using (
        id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
        and (
            lower(trim(plan_code)) <> 'dev_owner'
            or public.is_internal_owner()
        )
    );

drop policy if exists "opticas_update_member" on public.opticas;
create policy "opticas_update_member" on public.opticas
    for update using (
        id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
        and (
            lower(trim(plan_code)) <> 'dev_owner'
            or public.is_internal_owner()
        )
    )
    with check (
        id in (select uo.optica_id from public.usuario_optica uo where uo.user_id = auth.uid())
        and (
            lower(trim(plan_code)) <> 'dev_owner'
            or public.is_internal_owner()
        )
    );

create or replace function public.enforce_dev_owner_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' and not public.is_internal_owner() then
    raise exception 'El plan dev_owner está restringido al owner interno';
  end if;

  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' then
    new.plan_source := 'internal';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_opticas_dev_owner_guard on public.opticas;
create trigger trg_opticas_dev_owner_guard
before insert or update of plan_code, plan_source on public.opticas
for each row
execute function public.enforce_dev_owner_guard();

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

  if not public.is_internal_owner() or new.user_id <> auth.uid() then
    raise exception 'Membresía restringida: la óptica dev_owner solo puede pertenecer al owner interno';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_usuario_optica_dev_owner_guard on public.usuario_optica;
create trigger trg_usuario_optica_dev_owner_guard
before insert or update on public.usuario_optica
for each row
execute function public.enforce_dev_owner_membership_guard();

with owner_user as (
  select up.user_id
  from public.user_profiles up
  where lower(trim(up.email)) = 'jaermadera@gmail.com'
  limit 1
),
dev_opticas as (
  select o.id
  from public.opticas o
  where lower(trim(o.plan_code)) = 'dev_owner'
)
delete from public.usuario_optica uo
where uo.optica_id in (select id from dev_opticas)
  and exists (select 1 from owner_user)
  and uo.user_id <> (select user_id from owner_user);

comment on function public.is_internal_owner is
'Retorna true solo para la cuenta interna autorizada para plan dev_owner.';
