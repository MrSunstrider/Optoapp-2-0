-- Remove free-plan restrictions EXCEPT: max 2 opticas per user.
-- Root cause: migration 20260619000000 dropped max_opticas / max_pacientes_por_optica /
-- max_usuarios_por_optica CASCADE, breaking the trigger and RPC.

-- 1. Drop the broken limit guard (references dropped columns)
DROP TRIGGER IF EXISTS trg_opticas_limit_guard ON public.opticas;
DROP FUNCTION IF EXISTS enforce_optica_limit_for_creator();

-- 2. Replace with a clean 2-optica limit
CREATE OR REPLACE FUNCTION public.enforce_optica_limit_for_creator()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_count bigint;
BEGIN
  select count(*) into v_count
  from public.usuario_optica uo
  where uo.user_id = auth.uid();

  if v_count >= 2 then
    raise exception 'Has alcanzado el límite de 2 ópticas del plan gratuito.';
  end if;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_opticas_limit_guard
  BEFORE INSERT ON public.opticas
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_optica_limit_for_creator();

-- 3. Drop the plan-lock trigger (no longer needed)
DROP TRIGGER IF EXISTS trg_opticas_lock_plan ON public.opticas;
DROP FUNCTION IF EXISTS opticas_lock_plan_from_clients();

-- 4. Relax RLS INSERT — only require auth + non-empty id/nombre
DROP POLICY IF EXISTS opticas_insert_authenticated ON public.opticas;
CREATE POLICY opticas_insert_authenticated ON public.opticas
FOR INSERT TO authenticated
WITH CHECK (
  (select auth.uid()) is not null
  and nullif(btrim(id), '') is not null
  and nullif(btrim(nombre), '') is not null
);

-- 5. Fix RPC: remove dropped columns from INSERT
CREATE OR REPLACE FUNCTION public.create_optica_for_current_user(
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
$$;

grant execute on function public.create_optica_for_current_user(text, text, text, text, text, text) to authenticated;;
