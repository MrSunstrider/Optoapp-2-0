-- Gestión de usuarios y roles por óptica.
-- Objetivo:
-- 1) Permitir a admin/gerente de una óptica gestionar membresías (usuario_optica)
-- 2) Resolver asignación por email sin exponer auth.users al cliente
-- 3) Mantener login limpio: autenticación separada de autorización por rol

-- ---------------------------------------------------------------------------
-- 1) Perfiles de usuario (espejo mínimo de auth.users)
-- ---------------------------------------------------------------------------
create table if not exists public.user_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique,
  created_at timestamptz not null default timezone('utc', now())
);

comment on table public.user_profiles is
'Perfil público mínimo para asignación de roles por email (sin exponer auth.users)';

insert into public.user_profiles (user_id, email)
select u.id, lower(trim(u.email))
from auth.users u
where u.email is not null and trim(u.email) <> ''
on conflict (user_id) do update set email = excluded.email;

create or replace function public.sync_user_profiles_from_auth()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if new.email is not null and btrim(new.email) <> '' then
    insert into public.user_profiles (user_id, email)
    values (new.id, lower(btrim(new.email)))
    on conflict (user_id) do update set email = excluded.email;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_auth_users_sync_profile on auth.users;
create trigger trg_auth_users_sync_profile
after insert or update of email on auth.users
for each row execute function public.sync_user_profiles_from_auth();

alter table public.user_profiles enable row level security;

drop policy if exists user_profiles_select_own on public.user_profiles;
create policy user_profiles_select_own on public.user_profiles
for select using (user_id = auth.uid());

drop policy if exists user_profiles_select_admin on public.user_profiles;
create policy user_profiles_select_admin on public.user_profiles
for select using (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);

-- ---------------------------------------------------------------------------
-- 2) Vista operativa de miembros por óptica
-- ---------------------------------------------------------------------------
create or replace view public.optica_members as
select
  uo.optica_id,
  uo.user_id,
  coalesce(up.email, '') as email,
  uo.rol,
  uo.created_at
from public.usuario_optica uo
left join public.user_profiles up on up.user_id = uo.user_id;

comment on view public.optica_members is
'Miembros por óptica con email visible para administración interna';

grant select on public.optica_members to authenticated;

-- ---------------------------------------------------------------------------
-- 3) Políticas para gestionar membresías por admin/gerente
-- ---------------------------------------------------------------------------
drop policy if exists usuario_optica_select_own on public.usuario_optica;
create policy usuario_optica_select_member_scope on public.usuario_optica
for select using (
  user_id = auth.uid()
  or exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = usuario_optica.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);

drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
create policy usuario_optica_insert_admin_optica on public.usuario_optica
for insert with check (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = usuario_optica.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);

drop policy if exists usuario_optica_update_admin_optica on public.usuario_optica;
create policy usuario_optica_update_admin_optica on public.usuario_optica
for update using (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = usuario_optica.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = usuario_optica.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);

-- ---------------------------------------------------------------------------
-- 4) Función RPC para asignar rol por email
-- ---------------------------------------------------------------------------
create or replace function public.assign_optica_role_by_email(
  p_optica_id text,
  p_email text,
  p_rol text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_email text;
  v_uid uuid;
  v_rol text;
  v_is_allowed boolean;
begin
  v_email := lower(btrim(coalesce(p_email, '')));
  v_rol := lower(btrim(coalesce(p_rol, '')));

  if v_email = '' then
    raise exception 'Email requerido';
  end if;

  if v_rol not in ('admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas', 'invitado') then
    raise exception 'Rol no permitido: %', p_rol;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = p_optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  ) into v_is_allowed;

  if not v_is_allowed then
    raise exception 'Sin permisos para gestionar roles en esta óptica';
  end if;

  select up.user_id into v_uid
  from public.user_profiles up
  where up.email = v_email;

  if v_uid is null then
    raise exception 'No existe una cuenta con ese email. Debe registrarse primero.';
  end if;

  insert into public.usuario_optica (user_id, optica_id, rol)
  values (v_uid, p_optica_id, v_rol)
  on conflict (user_id, optica_id)
  do update set rol = excluded.rol;
end;
$$;

grant execute on function public.assign_optica_role_by_email(text, text, text) to authenticated;

comment on function public.assign_optica_role_by_email is
'Permite a admin/gerente asignar membresía/rol en su óptica usando email de cuenta existente.';
