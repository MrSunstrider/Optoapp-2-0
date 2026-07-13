CREATE OR REPLACE FUNCTION public.create_optica_for_current_user(
  p_optica_id text,
  p_nombre text,
  p_fiscal_doc_tipo text DEFAULT '',
  p_fiscal_doc_numero text DEFAULT '',
  p_razon_social text DEFAULT '',
  p_direccion_fiscal text DEFAULT ''
) RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
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
    id, nombre, plan, plan_code,
    plan_source, plan_status,
    fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal
  ) values (
    p_optica_id, v_nombre, 'free', 'free', 'manual', 'active',
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
$$;;
