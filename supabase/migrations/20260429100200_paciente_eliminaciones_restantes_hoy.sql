-- Consulta cuántas eliminaciones de paciente le quedan hoy al usuario en la óptica
-- (mismo límite diario que guard_pacientes_delete: 10).

create or replace function public.paciente_eliminaciones_restantes_hoy(p_optica_id text)
returns integer
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_uid uuid;
  v_count int;
  v_daily_limit int := 10;
begin
  v_uid := auth.uid();
  if v_uid is null then
    return 0;
  end if;

  select count(*)::int into v_count
  from public.pacientes_delete_audit a
  where a.optica_id = p_optica_id
    and a.deleted_by = v_uid
    and a.deleted_at >= date_trunc('day', timezone('utc', now()))
    and a.deleted_at < date_trunc('day', timezone('utc', now())) + interval '1 day';

  return greatest(0, v_daily_limit - v_count);
end;
$$;

revoke all on function public.paciente_eliminaciones_restantes_hoy(text) from public;
grant execute on function public.paciente_eliminaciones_restantes_hoy(text) to authenticated;
