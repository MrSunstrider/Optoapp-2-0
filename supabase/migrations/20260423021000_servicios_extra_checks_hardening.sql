-- P3-T2: Endurecer datos/constraints de servicios_extra para sync robusta.
-- Objetivo:
--   1) Normalizar valores legacy (vacíos/espacios) antes de validar.
--   2) Asegurar constraints explícitos NOT BLANK en columnas críticas.
--   3) Mantener migración idempotente para múltiples entornos.

begin;
-- 1) Normalización de datos existentes (idempotente)
update public.servicios_extra
set
  descripcion = case when btrim(coalesce(descripcion, '')) = '' then 'Sin descripción' else btrim(descripcion) end,
  estado = case when btrim(coalesce(estado, '')) = '' then 'Pendiente' else btrim(estado) end,
  metodo_pago = case when btrim(coalesce(metodo_pago, '')) = '' then 'Sin especificar' else btrim(metodo_pago) end,
  optica_id = case when btrim(coalesce(optica_id, '')) = '' then 'mi_optica_base' else btrim(optica_id) end,
  ot = case when btrim(coalesce(ot, '')) = '' then '-' else btrim(ot) end;
-- 2) Constraints explícitos de no-vacío (en bloque dinámico para evitar fallos si ya existen)
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_descripcion_not_blank_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_descripcion_not_blank_chk
      check (btrim(descripcion) <> '');
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_estado_not_blank_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_estado_not_blank_chk
      check (btrim(estado) <> '');
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_metodo_pago_not_blank_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_metodo_pago_not_blank_chk
      check (btrim(metodo_pago) <> '');
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'servicios_extra_optica_id_not_blank_chk'
      and conrelid = 'public.servicios_extra'::regclass
  ) then
    alter table public.servicios_extra
      add constraint servicios_extra_optica_id_not_blank_chk
      check (btrim(optica_id) <> '');
  end if;
end $$;
comment on constraint servicios_extra_descripcion_not_blank_chk on public.servicios_extra
  is 'Evita descripciones vacías o con solo espacios (P3-T2).';
comment on constraint servicios_extra_estado_not_blank_chk on public.servicios_extra
  is 'Evita estado vacío; la app usa Pendiente/Entregado (P3-T2).';
comment on constraint servicios_extra_metodo_pago_not_blank_chk on public.servicios_extra
  is 'Evita método de pago vacío en fila agregada de servicio extra (P3-T2).';
comment on constraint servicios_extra_optica_id_not_blank_chk on public.servicios_extra
  is 'Evita tenant vacío para servicios_extra (P3-T2).';
commit;
