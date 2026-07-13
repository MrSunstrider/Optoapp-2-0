-- Seguridad: impedir que un usuario no-admin se autoasigne rol admin.
-- Regla:
-- - Para insertar/actualizar una membresía con rol=admin, el actor (auth.uid)
--   debe ser admin actual de esa óptica.

create or replace function public.enforce_admin_role_assignment_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_actor_is_admin boolean;
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

  if not v_actor_is_admin then
    raise exception 'Solo un admin actual de la óptica puede asignar rol admin';
  end if;

  return new;
end;
$$;
drop trigger if exists trg_usuario_optica_admin_role_guard on public.usuario_optica;
create trigger trg_usuario_optica_admin_role_guard
before insert or update on public.usuario_optica
for each row
execute function public.enforce_admin_role_assignment_guard();
comment on function public.enforce_admin_role_assignment_guard is
'Bloquea asignación de rol admin cuando el actor no es admin vigente de la óptica.';
