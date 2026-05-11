-- Onboarding de óptica:
-- Permite que un usuario autenticado cree su propia óptica y su primera membresía admin.

-- Asegurar columna plan para controlar Free/Pro por óptica
alter table public.opticas
add column if not exists plan text not null default 'free';

update public.opticas
set plan = 'free'
where plan is null or btrim(plan) = '';

-- Políticas de inserción para onboarding
drop policy if exists opticas_insert_authenticated on public.opticas;
create policy opticas_insert_authenticated on public.opticas
for insert to authenticated
with check (true);

drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
create policy usuario_optica_insert_admin_optica on public.usuario_optica
for insert to authenticated
with check (
  -- Caso gestión normal: admin/gerente vigente gestiona su óptica
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = usuario_optica.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
  -- Caso bootstrap: primera membresía de esa óptica, para sí mismo, rol admin
  or (
    usuario_optica.user_id = auth.uid()
    and lower(trim(usuario_optica.rol)) = 'admin'
    and not exists (
      select 1 from public.usuario_optica u where u.optica_id = usuario_optica.optica_id
    )
  )
);

-- Ajustar guard anti-escalación: permitir bootstrap
create or replace function public.enforce_admin_role_assignment_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_actor_is_admin boolean;
  v_bootstrap_allowed boolean;
begin
  if lower(trim(coalesce(new.rol, ''))) <> 'admin' then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = new.optica_id
      and lower(trim(self.rol)) = 'admin'
  ) into v_actor_is_admin;

  if v_actor_is_admin then
    return new;
  end if;

  select (
    new.user_id = auth.uid()
    and not exists (
      select 1 from public.usuario_optica u where u.optica_id = new.optica_id
    )
  ) into v_bootstrap_allowed;

  if v_bootstrap_allowed then
    return new;
  end if;

  raise exception 'Solo un admin actual de la óptica puede asignar rol admin';
end;
$$;
