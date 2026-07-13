-- Plan interno para cuentas de desarrollo/owner.
-- Objetivo: exención explícita de facturación y límites ilimitados.

update public.opticas
set plan_code = 'dev_owner'
where lower(trim(plan_code)) = 'internal_owner';
update public.opticas
set plan_source = 'internal'
where lower(trim(plan_code)) = 'dev_owner'
  and lower(trim(plan_source)) not in ('internal');
update public.opticas
set max_opticas = null,
    max_pacientes_por_optica = null,
    max_usuarios_por_optica = null,
    plan_status = 'active'
where lower(trim(plan_code)) = 'dev_owner';
alter table public.opticas
  drop constraint if exists opticas_plan_code_chk;
alter table public.opticas
  add constraint opticas_plan_code_chk
  check (lower(trim(plan_code)) in ('free','pro_individual','pro_multisite_15','enterprise','dev_owner'));
alter table public.opticas
  drop constraint if exists opticas_plan_source_chk;
alter table public.opticas
  add constraint opticas_plan_source_chk
  check (lower(trim(plan_source)) in ('playstore','web','manual','internal'));
comment on column public.opticas.plan_code is
'Plan comercial/interno: free | pro_individual | pro_multisite_15 | enterprise | dev_owner';
