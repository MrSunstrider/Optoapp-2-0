-- Alinea OT vacia en servicios_extra con la estrategia actual de sync.
-- Regla vigente: OT vacia se representa como '' (no como '-').

begin;
update public.servicios_extra
set ot = ''
where btrim(coalesce(ot, '')) = '-';
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_ot_not_dash_placeholder_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_ot_not_dash_placeholder_chk
      check (btrim(coalesce(ot, '')) <> '-');
  end if;
end $$;
comment on constraint servicios_extra_ot_not_dash_placeholder_chk on public.servicios_extra
  is 'Evita usar ''-'' como placeholder de OT vacia; usar cadena vacia.';
commit;
