-- create_optica_for_current_user: atomic optica creation + owner membership in one transaction.
-- Replaces the two separate upserts in OpticaSettingsDataSource so there is no partial-write window.
create or replace function public.create_optica_for_current_user(
  p_optica_id         text,
  p_nombre            text,
  p_fiscal_doc_tipo   text default '',
  p_fiscal_doc_numero text default '',
  p_razon_social      text default '',
  p_direccion_fiscal  text default ''
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid    uuid;
  v_nombre text;
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sin sesión';
  end if;

  v_nombre := trim(p_nombre);
  if v_nombre = '' then
    raise exception 'Nombre de óptica requerido';
  end if;

  insert into public.opticas (
    id, nombre, plan, plan_code, max_opticas,
    max_pacientes_por_optica, max_usuarios_por_optica,
    plan_source, plan_status,
    fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal
  ) values (
    p_optica_id, v_nombre, 'free', 'free', 1, 20, 2, 'manual', 'active',
    upper(trim(p_fiscal_doc_tipo)),
    trim(p_fiscal_doc_numero),
    trim(p_razon_social),
    trim(p_direccion_fiscal)
  )
  on conflict (id) do update set
    nombre            = excluded.nombre,
    fiscal_doc_tipo   = excluded.fiscal_doc_tipo,
    fiscal_doc_numero = excluded.fiscal_doc_numero,
    razon_social      = excluded.razon_social,
    direccion_fiscal  = excluded.direccion_fiscal;

  insert into public.usuario_optica (user_id, optica_id, rol)
  values (v_uid, p_optica_id, 'admin')
  on conflict (user_id, optica_id) do update set rol = 'admin';
end;
$$;

grant execute on function public.create_optica_for_current_user(text, text, text, text, text, text) to authenticated;

comment on function public.create_optica_for_current_user is
'Crea una óptica y registra al usuario actual como admin en una sola transacción.';

-- All role writes now go through assign_optica_role_by_email (security definer).
-- Direct INSERT/UPDATE on usuario_optica bypassed the role-validation logic in that RPC.
drop policy if exists usuario_optica_insert_admin_optica on public.usuario_optica;
drop policy if exists usuario_optica_update_admin_optica on public.usuario_optica;
