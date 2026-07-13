-- Corrige recursión infinita de RLS en public.usuario_optica.
-- Problema: políticas que consultaban la misma tabla en EXISTS causaban
-- "infinite recursion detected in policy for relation usuario_optica"
-- cuando el rol authenticated evaluaba SELECT/UPDATE.

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
  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = p_user_id
      and uo.optica_id = p_optica_id
      and lower(trim(uo.rol)) = any (p_roles)
  );
$$;
revoke all on function public.has_optica_role(uuid, text, text[]) from public;
grant execute on function public.has_optica_role(uuid, text, text[]) to authenticated;
drop policy if exists usuario_optica_select_member_scope on public.usuario_optica;
create policy usuario_optica_select_member_scope on public.usuario_optica
for select
using (
  user_id = auth.uid()
  or public.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
);
drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
create policy usuario_optica_insert_admin_optica on public.usuario_optica
for insert to authenticated
with check (
  public.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
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
  public.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
)
with check (
  public.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente'])
);
