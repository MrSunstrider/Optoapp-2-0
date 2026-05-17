-- La funcion update_updated_at() se define aqui porque las triggers de esta migracion la referencian.
-- Se definio originalmente en 20260510000000 como fix, pero necesita existir antes para CI / preview branches.
create or replace function public.update_updated_at()
returns trigger
language plpgsql as $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;

-- Cierre de caja formal por optica y fecha operativa.
create table if not exists public.cierres_caja (
  id uuid primary key default gen_random_uuid(),
  optica_id text not null references public.opticas(id) on delete cascade,
  fecha_operativa date not null,
  estado text not null check (estado in ('abierto', 'cerrado')),
  total_efectivo_cent integer not null default 0 check (total_efectivo_cent >= 0),
  total_tarjeta_cent integer not null default 0 check (total_tarjeta_cent >= 0),
  total_transferencia_cent integer not null default 0 check (total_transferencia_cent >= 0),
  total_yape_cent integer not null default 0 check (total_yape_cent >= 0),
  total_plin_cent integer not null default 0 check (total_plin_cent >= 0),
  total_general_cent integer not null default 0 check (total_general_cent >= 0),
  observaciones text null,
  closed_by uuid null references auth.users(id) on delete set null,
  closed_at timestamptz null,
  reopened_by uuid null references auth.users(id) on delete set null,
  reopened_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (optica_id, fecha_operativa)
);

create index if not exists idx_cierres_caja_optica_fecha
  on public.cierres_caja (optica_id, fecha_operativa desc);

drop trigger if exists trg_cierres_caja_updated_at on public.cierres_caja;
create trigger trg_cierres_caja_updated_at
before update on public.cierres_caja
for each row execute function public.update_updated_at();

alter table public.cierres_caja enable row level security;

drop policy if exists cierres_caja_select_member on public.cierres_caja;
create policy cierres_caja_select_member
on public.cierres_caja
for select
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = auth.uid()
  )
);

drop policy if exists cierres_caja_upsert_admin_manager on public.cierres_caja;
create policy cierres_caja_upsert_admin_manager
on public.cierres_caja
for all
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = cierres_caja.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
);

-- Configuracion operativa compartida por optica (multiusuario).
create table if not exists public.optica_settings (
  optica_id text primary key references public.opticas(id) on delete cascade,
  config_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists trg_optica_settings_updated_at on public.optica_settings;
create trigger trg_optica_settings_updated_at
before update on public.optica_settings
for each row execute function public.update_updated_at();

alter table public.optica_settings enable row level security;

drop policy if exists optica_settings_select_member on public.optica_settings;
create policy optica_settings_select_member
on public.optica_settings
for select
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = auth.uid()
  )
);

drop policy if exists optica_settings_upsert_admin_manager on public.optica_settings;
create policy optica_settings_upsert_admin_manager
on public.optica_settings
for all
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = optica_settings.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
);
