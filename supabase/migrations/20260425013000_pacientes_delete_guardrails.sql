-- Guardrails de borrado de pacientes:
-- 1) Solo admin/gerente pueden eliminar pacientes (RLS server-side).
-- 2) Límite diario por usuario y óptica para evitar borrado masivo.

create table if not exists public.pacientes_delete_audit (
  id bigserial primary key,
  optica_id text not null references public.opticas(id) on delete cascade,
  paciente_id text not null,
  deleted_by uuid not null references auth.users(id) on delete cascade,
  deleted_at timestamptz not null default timezone('utc', now())
);

create index if not exists idx_pacientes_delete_audit_optica_day
  on public.pacientes_delete_audit (optica_id, deleted_by, deleted_at);

revoke all on table public.pacientes_delete_audit from anon, authenticated;

create or replace function public.guard_pacientes_delete()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid;
  v_is_allowed boolean;
  v_deleted_today int;
  v_daily_limit int := 10;
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sesión inválida para eliminación de paciente.';
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = v_uid
      and uo.optica_id = old.optica_id
      and lower(trim(uo.rol)) in ('admin', 'gerente')
  ) into v_is_allowed;

  if not v_is_allowed then
    raise exception 'Solo admin o gerente pueden eliminar pacientes.';
  end if;

  select count(*)
  into v_deleted_today
  from public.pacientes_delete_audit a
  where a.optica_id = old.optica_id
    and a.deleted_by = v_uid
    and a.deleted_at >= date_trunc('day', timezone('utc', now()))
    and a.deleted_at < date_trunc('day', timezone('utc', now())) + interval '1 day';

  if v_deleted_today >= v_daily_limit then
    raise exception 'Límite diario de eliminaciones alcanzado (%).', v_daily_limit;
  end if;

  insert into public.pacientes_delete_audit (optica_id, paciente_id, deleted_by)
  values (old.optica_id, old.id, v_uid);

  return old;
end;
$$;

drop trigger if exists trg_guard_pacientes_delete on public.pacientes;
create trigger trg_guard_pacientes_delete
before delete on public.pacientes
for each row
execute function public.guard_pacientes_delete();

drop policy if exists pacientes_delete on public.pacientes;
create policy pacientes_delete on public.pacientes
for delete
using (
  exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = pacientes.optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);
