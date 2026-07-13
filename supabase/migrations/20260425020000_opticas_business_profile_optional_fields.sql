-- Campos opcionales de perfil de óptica/sucursal para contexto de negocio.
-- Se mantienen opcionales, pero su edición se restringe a admin/gerente.

alter table public.opticas
  add column if not exists distrito_ciudad_departamento text not null default '',
  add column if not exists moneda text not null default '',
  add column if not exists pais text not null default '',
  add column if not exists contacto_whatsapp_telefono text not null default '';
comment on column public.opticas.distrito_ciudad_departamento is 'Ubicación operativa resumida';
comment on column public.opticas.moneda is 'Moneda principal de operación';
comment on column public.opticas.pais is 'País principal de operación';
comment on column public.opticas.contacto_whatsapp_telefono is 'WhatsApp o teléfono comercial de contacto';
create or replace function public.guard_opticas_business_profile_optional_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_can_edit boolean;
begin
  if new.distrito_ciudad_departamento is not distinct from old.distrito_ciudad_departamento
     and new.moneda is not distinct from old.moneda
     and new.pais is not distinct from old.pais
     and new.contacto_whatsapp_telefono is not distinct from old.contacto_whatsapp_telefono then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
      and uo.optica_id = old.id
      and lower(trim(uo.rol)) in ('admin', 'gerente')
  ) into v_can_edit;

  if not v_can_edit then
    raise exception 'Solo admin/gerente puede actualizar perfil de óptica';
  end if;

  new.distrito_ciudad_departamento := trim(coalesce(new.distrito_ciudad_departamento, ''));
  new.moneda := trim(coalesce(new.moneda, ''));
  new.pais := trim(coalesce(new.pais, ''));
  new.contacto_whatsapp_telefono := trim(coalesce(new.contacto_whatsapp_telefono, ''));

  return new;
end;
$$;
drop trigger if exists trg_guard_opticas_business_profile_optional_update on public.opticas;
create trigger trg_guard_opticas_business_profile_optional_update
before update of distrito_ciudad_departamento, moneda, pais, contacto_whatsapp_telefono
on public.opticas
for each row
execute function public.guard_opticas_business_profile_optional_update();
