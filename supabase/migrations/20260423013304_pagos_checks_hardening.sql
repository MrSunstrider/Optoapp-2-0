-- P3-T3: Endurecer datos/constraints de pagos para sync robusta.
-- Objetivo:
--   1) Normalizar legacy (vacíos/espacios) antes de checks.
--   2) Evitar strings vacíos en FK UUID opcionales (usar NULL).
--   3) Garantizar campos críticos no vacíos.

begin;

-- 1) Normalización de datos existentes (idempotente)
update public.pagos
set
  metodo_pago = case when btrim(coalesce(metodo_pago, '')) = '' then 'Efectivo' else btrim(metodo_pago) end,
  tipo = case when btrim(coalesce(tipo, '')) = '' then 'Abono' else btrim(tipo) end,
  optica_id = case when btrim(coalesce(optica_id, '')) = '' then 'mi_optica_base' else btrim(optica_id) end,
  nota = btrim(coalesce(nota, ''));

-- FK opcionales: limpiar blancos -> NULL respetando tipo real de columna (uuid o text)
do $$
declare
  disp_type text;
  serv_type text;
begin
  select data_type into disp_type
  from information_schema.columns
  where table_schema = 'public' and table_name = 'pagos' and column_name = 'dispensacion_id';

  if disp_type = 'uuid' then
    execute $sql$
      update public.pagos
      set dispensacion_id = nullif(btrim(coalesce(dispensacion_id::text, '')), '')::uuid
    $sql$;
  else
    execute $sql$
      update public.pagos
      set dispensacion_id = nullif(btrim(coalesce(dispensacion_id::text, '')), '')
    $sql$;
  end if;

  select data_type into serv_type
  from information_schema.columns
  where table_schema = 'public' and table_name = 'pagos' and column_name = 'servicio_extra_id';

  if serv_type = 'uuid' then
    execute $sql$
      update public.pagos
      set servicio_extra_id = nullif(btrim(coalesce(servicio_extra_id::text, '')), '')::uuid
    $sql$;
  else
    execute $sql$
      update public.pagos
      set servicio_extra_id = nullif(btrim(coalesce(servicio_extra_id::text, '')), '')
    $sql$;
  end if;
end $$;

-- 2) Constraints explícitos de no-vacío
do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'pagos_metodo_pago_not_blank_chk'
      and conrelid = 'public.pagos'::regclass
  ) then
    alter table public.pagos
      add constraint pagos_metodo_pago_not_blank_chk
      check (btrim(metodo_pago) <> '');
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'pagos_tipo_not_blank_chk'
      and conrelid = 'public.pagos'::regclass
  ) then
    alter table public.pagos
      add constraint pagos_tipo_not_blank_chk
      check (btrim(tipo) <> '');
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'pagos_optica_id_not_blank_chk'
      and conrelid = 'public.pagos'::regclass
  ) then
    alter table public.pagos
      add constraint pagos_optica_id_not_blank_chk
      check (btrim(optica_id) <> '');
  end if;
end $$;

comment on constraint pagos_metodo_pago_not_blank_chk on public.pagos
  is 'Evita método de pago vacío en pagos (P3-T3).';
comment on constraint pagos_tipo_not_blank_chk on public.pagos
  is 'Evita tipo vacío en pagos (P3-T3).';
comment on constraint pagos_optica_id_not_blank_chk on public.pagos
  is 'Evita tenant vacío en pagos (P3-T3).';

commit;
