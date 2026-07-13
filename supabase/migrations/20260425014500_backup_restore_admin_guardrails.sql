-- Guardrails de respaldo/restauración:
-- - Solo admin de la óptica puede exportar/restaurar.
-- - Restore solo permitido cuando source_optica_id == target_optica_id.

create or replace function public.assert_backup_operation_allowed(
  p_action text,
  p_source_optica_id text,
  p_target_optica_id text
)
returns void
language plpgsql
security definer
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
grant execute on function public.assert_backup_operation_allowed(text, text, text) to authenticated;
