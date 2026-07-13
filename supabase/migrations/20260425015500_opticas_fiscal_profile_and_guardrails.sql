-- Datos fiscales base por óptica para facturación futura.
-- Requisito: solo admin o gerente pueden modificar estos campos.

alter table public.opticas
  add column if not exists fiscal_doc_tipo text not null default '',
  add column if not exists fiscal_doc_numero text not null default '',
  add column if not exists razon_social text not null default '',
  add column if not exists direccion_fiscal text not null default '';
comment on column public.opticas.fiscal_doc_tipo is 'Tipo de documento fiscal: RUC o RUS';
comment on column public.opticas.fiscal_doc_numero is 'Número del documento fiscal';
comment on column public.opticas.razon_social is 'Razón social para emisión de comprobantes';
comment on column public.opticas.direccion_fiscal is 'Dirección fiscal de la óptica';
create or replace function public.guard_opticas_fiscal_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_can_edit boolean;
begin
  if new.fiscal_doc_tipo is not distinct from old.fiscal_doc_tipo
     and new.fiscal_doc_numero is not distinct from old.fiscal_doc_numero
     and new.razon_social is not distinct from old.razon_social
     and new.direccion_fiscal is not distinct from old.direccion_fiscal then
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
    raise exception 'Solo admin/gerente puede actualizar datos fiscales de la óptica';
  end if;

  new.fiscal_doc_tipo := upper(trim(coalesce(new.fiscal_doc_tipo, '')));
  if new.fiscal_doc_tipo not in ('RUC', 'RUS') then
    raise exception 'Tipo fiscal inválido. Use RUC o RUS';
  end if;

  new.fiscal_doc_numero := trim(coalesce(new.fiscal_doc_numero, ''));
  new.razon_social := trim(coalesce(new.razon_social, ''));
  new.direccion_fiscal := trim(coalesce(new.direccion_fiscal, ''));

  if new.fiscal_doc_numero = '' or new.razon_social = '' or new.direccion_fiscal = '' then
    raise exception 'RUC/RUS, razón social y dirección fiscal son obligatorios';
  end if;

  return new;
end;
$$;
drop trigger if exists trg_guard_opticas_fiscal_update on public.opticas;
create trigger trg_guard_opticas_fiscal_update
before update of fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal
on public.opticas
for each row
execute function public.guard_opticas_fiscal_update();
