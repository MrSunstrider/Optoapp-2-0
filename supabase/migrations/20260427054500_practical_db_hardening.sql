-- Practical hardening (safe + idempotent)
-- 1) Composite indexes by tenant + date/updated_at
-- 2) Explicit domain constraints for delivery states
-- 3) Consistent updated_at default in UTC
-- 4) Keep uniqueness by optica for OT/HO and remove redundant OT indexes
-- 5) Optional sync telemetry table (server observability)
-- 6) Document optional FK servicios_extra.paciente_id

-- ---------------------------------------------------------------------------
-- 1) Composite indexes by tenant + date / updated_at
-- ---------------------------------------------------------------------------
create index if not exists idx_pacientes_optica_updated_at
  on public.pacientes (optica_id, updated_at desc);
create index if not exists idx_evaluaciones_optica_fecha
  on public.evaluaciones (optica_id, fecha desc);
create index if not exists idx_evaluaciones_optica_updated_at
  on public.evaluaciones (optica_id, updated_at desc);
create index if not exists idx_dispensaciones_optica_fecha
  on public.dispensaciones (optica_id, fecha desc);
create index if not exists idx_dispensaciones_optica_updated_at
  on public.dispensaciones (optica_id, updated_at desc);
create index if not exists idx_servicios_extra_optica_fecha
  on public.servicios_extra (optica_id, fecha desc);
create index if not exists idx_servicios_extra_optica_updated_at
  on public.servicios_extra (optica_id, updated_at desc);
create index if not exists idx_pagos_optica_fecha
  on public.pagos (optica_id, fecha desc);
create index if not exists idx_pagos_optica_updated_at
  on public.pagos (optica_id, updated_at desc);
-- ---------------------------------------------------------------------------
-- 2) Explicit domain constraints (delivery states)
-- ---------------------------------------------------------------------------
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'dispensaciones_estado_entrega_domain_chk'
      and conrelid = 'public.dispensaciones'::regclass
  ) then
    alter table public.dispensaciones
      add constraint dispensaciones_estado_entrega_domain_chk
      check (estado_entrega in ('Pendiente', 'Entregado'))
      not valid;
  end if;
end$$;
alter table public.dispensaciones
  validate constraint dispensaciones_estado_entrega_domain_chk;
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_estado_domain_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_estado_domain_chk
      check (estado in ('Pendiente', 'Entregado'))
      not valid;
  end if;
end$$;
alter table public.servicios_extra
  validate constraint servicios_extra_estado_domain_chk;
-- ---------------------------------------------------------------------------
-- 3) Consistent updated_at default in UTC
-- ---------------------------------------------------------------------------
alter table public.pacientes
  alter column updated_at set default timezone('utc', now());
alter table public.evaluaciones
  alter column updated_at set default timezone('utc', now());
alter table public.dispensaciones
  alter column updated_at set default timezone('utc', now());
alter table public.servicios_extra
  alter column updated_at set default timezone('utc', now());
alter table public.pagos
  alter column updated_at set default timezone('utc', now());
-- ---------------------------------------------------------------------------
-- 4) Keep uniqueness by optica and remove redundant OT indexes (dispensaciones)
-- Canonical OT uniqueness index is: dispensaciones_optica_ot_uq_v2
-- ---------------------------------------------------------------------------
drop index if exists public.idx_dispensaciones_optica_ot_unique;
drop index if exists public.idx_dispensaciones_ot_unique_per_optica;
-- ---------------------------------------------------------------------------
-- 5) Optional sync telemetry table (server-side observability)
-- ---------------------------------------------------------------------------
create table if not exists public.sync_telemetry_optica (
  optica_id text primary key references public.opticas(id) on delete cascade,
  last_sync_at timestamptz,
  last_status text not null default 'idle'
    check (last_status in ('idle', 'ok', 'error')),
  last_stage text not null default '',
  last_error text not null default '',
  last_actor uuid references auth.users(id) on delete set null,
  updated_at timestamptz not null default timezone('utc', now())
);
create or replace function public.set_sync_telemetry_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;
drop trigger if exists trg_sync_telemetry_optica_updated_at on public.sync_telemetry_optica;
create trigger trg_sync_telemetry_optica_updated_at
before update on public.sync_telemetry_optica
for each row
execute function public.set_sync_telemetry_updated_at();
alter table public.sync_telemetry_optica enable row level security;
revoke all on table public.sync_telemetry_optica from anon, authenticated;
drop policy if exists sync_telemetry_optica_no_client_access on public.sync_telemetry_optica;
create policy sync_telemetry_optica_no_client_access
on public.sync_telemetry_optica
for all
to authenticated
using (false)
with check (false);
comment on table public.sync_telemetry_optica is
'Telemetría resumida de última sincronización por óptica (uso operativo/soporte).';
-- ---------------------------------------------------------------------------
-- 6) Document optional FK in servicios_extra
-- ---------------------------------------------------------------------------
comment on column public.servicios_extra.paciente_id is
'FK opcional: puede ser NULL para servicios no asociados a un paciente.';
