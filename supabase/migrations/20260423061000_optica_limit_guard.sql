-- Enforce de límite de ópticas por plan (max_opticas).
-- Regla:
-- - Si usuario no tiene ópticas admin/gerente aún, puede crear la primera.
-- - Si su plan admin/gerente tiene max_opticas = N, no puede superar N ópticas.
-- - Si tiene al menos un plan con max_opticas NULL (enterprise), no se limita.

create or replace function public.enforce_optica_limit_for_creator()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid;
  v_current_count integer;
  v_allowed integer;
  v_has_unlimited boolean;
begin
  v_uid := auth.uid();

  if v_uid is null then
    raise exception 'Sesión requerida para crear óptica';
  end if;

  -- Cuántas ópticas administra actualmente (admin/gerente).
  select count(distinct uo.optica_id)
  into v_current_count
  from public.usuario_optica uo
  where uo.user_id = v_uid
    and lower(trim(uo.rol)) in ('admin', 'gerente');

  -- Primera óptica: permitir bootstrap.
  if coalesce(v_current_count, 0) = 0 then
    return new;
  end if;

  -- ¿Tiene algún plan sin límite (enterprise)?
  select exists (
    select 1
    from public.usuario_optica uo
    join public.opticas o on o.id = uo.optica_id
    where uo.user_id = v_uid
      and lower(trim(uo.rol)) in ('admin', 'gerente')
      and o.max_opticas is null
  ) into v_has_unlimited;

  if v_has_unlimited then
    return new;
  end if;

  -- Límite vigente (tomamos el máximo disponible entre ópticas que administra).
  select max(o.max_opticas)
  into v_allowed
  from public.usuario_optica uo
  join public.opticas o on o.id = uo.optica_id
  where uo.user_id = v_uid
    and lower(trim(uo.rol)) in ('admin', 'gerente');

  if v_allowed is null then
    -- Seguridad defensiva: si no se pudo resolver, bloquear.
    raise exception 'No se pudo determinar el límite de ópticas de tu plan';
  end if;

  if v_current_count >= v_allowed then
    raise exception 'Has alcanzado el límite de ópticas de tu plan (%). Actualiza a un plan superior.', v_allowed;
  end if;

  return new;
end;
$$;

drop trigger if exists trg_opticas_limit_guard on public.opticas;
create trigger trg_opticas_limit_guard
before insert on public.opticas
for each row
execute function public.enforce_optica_limit_for_creator();

comment on function public.enforce_optica_limit_for_creator is
'Bloquea creación de nuevas ópticas cuando usuario admin/gerente alcanzó max_opticas del plan.';
