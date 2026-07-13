-- Base de planes escalable (incluye pro_multisite_15 y enterprise).
-- Mantiene compatibilidad con columna legacy `opticas.plan`.

alter table public.opticas add column if not exists plan_code text not null default 'free';
alter table public.opticas add column if not exists max_opticas integer;
alter table public.opticas add column if not exists max_pacientes_por_optica integer;
alter table public.opticas add column if not exists max_usuarios_por_optica integer;
alter table public.opticas add column if not exists plan_source text not null default 'manual';
alter table public.opticas add column if not exists plan_status text not null default 'active';
alter table public.opticas add column if not exists current_period_end timestamptz;
update public.opticas
set plan_code = case
  when lower(trim(coalesce(plan, 'free'))) in ('pro', 'paid', 'premium') then 'pro_individual'
  else 'free'
end
where plan_code is null or btrim(plan_code) = '' or plan_code = 'free';
update public.opticas
set max_opticas = case
  when plan_code = 'free' then 1
  when plan_code = 'pro_individual' then 1
  when plan_code = 'pro_multisite_15' then 15
  when plan_code = 'enterprise' then null
  else 1
end,
max_pacientes_por_optica = case
  when plan_code = 'free' then 20
  else null
end,
max_usuarios_por_optica = case
  when plan_code = 'free' then 2
  when plan_code = 'pro_individual' then 5
  when plan_code = 'pro_multisite_15' then 100
  else null
end
where max_opticas is null
   or max_pacientes_por_optica is null
   or max_usuarios_por_optica is null;
alter table public.opticas
  drop constraint if exists opticas_plan_code_chk;
alter table public.opticas
  add constraint opticas_plan_code_chk
  check (lower(trim(plan_code)) in ('free','pro_individual','pro_multisite_15','enterprise'));
alter table public.opticas
  drop constraint if exists opticas_plan_source_chk;
alter table public.opticas
  add constraint opticas_plan_source_chk
  check (lower(trim(plan_source)) in ('playstore','web','manual'));
alter table public.opticas
  drop constraint if exists opticas_plan_status_chk;
alter table public.opticas
  add constraint opticas_plan_status_chk
  check (lower(trim(plan_status)) in ('active','grace','canceled'));
comment on column public.opticas.plan_code is
'Plan comercial: free | pro_individual | pro_multisite_15 | enterprise';
comment on column public.opticas.max_opticas is
'Límite de ópticas por cuenta/organización para el plan';
comment on column public.opticas.max_pacientes_por_optica is
'Límite de pacientes por óptica (NULL = ilimitado)';
comment on column public.opticas.max_usuarios_por_optica is
'Límite de usuarios por óptica (NULL = ilimitado)';
