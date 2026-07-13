-- Migracion a fechas puras (LocalDate) en Supabase/Postgres.
-- Objetivo: almacenar fechas clinicas como DATE y evitar desfases por zona horaria.

begin;
create table if not exists public.schema_migrations_flags (
  key text primary key,
  applied_at timestamptz not null default now()
);
do $$
begin
  -- pacientes.fecha_creacion
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'pacientes' and column_name = 'fecha_creacion'
  ) then
    if (select data_type from information_schema.columns
        where table_schema = 'public' and table_name = 'pacientes' and column_name = 'fecha_creacion') <> 'date' then
      alter table public.pacientes
        alter column fecha_creacion type date
        using case
          when fecha_creacion is null then null
          when fecha_creacion::text ~ '^[0-9]+$' and length(fecha_creacion::text) = 13 then to_timestamp((fecha_creacion::double precision / 1000.0))::date
          when fecha_creacion::text ~ '^[0-9]+$' and length(fecha_creacion::text) = 10 then to_timestamp(fecha_creacion::double precision)::date
          when fecha_creacion::text ~ '^[0-9]+$' then to_timestamp((fecha_creacion::double precision / 1000.0))::date
          else fecha_creacion::text::date
        end;
    end if;
  end if;

  -- pacientes.fecha_nacimiento
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'pacientes' and column_name = 'fecha_nacimiento'
  ) then
    if (select data_type from information_schema.columns
        where table_schema = 'public' and table_name = 'pacientes' and column_name = 'fecha_nacimiento') <> 'date' then
      alter table public.pacientes
        alter column fecha_nacimiento type date
        using nullif(fecha_nacimiento::text, '')::date;
    end if;
  end if;

  -- evaluaciones.fecha, evaluaciones.proxima_cita, evaluaciones.lc_fecha_adaptacion
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='fecha') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='fecha') <> 'date' then
      alter table public.evaluaciones
        alter column fecha type date
        using case
          when fecha is null then null
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 13 then to_timestamp((fecha::double precision / 1000.0))::date
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 10 then to_timestamp(fecha::double precision)::date
          when fecha::text ~ '^[0-9]+$' then to_timestamp((fecha::double precision / 1000.0))::date
          else fecha::text::date
        end;
    end if;
  end if;

  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='proxima_cita') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='proxima_cita') <> 'date' then
      alter table public.evaluaciones
        alter column proxima_cita type date
        using case
          when proxima_cita is null then null
          when proxima_cita::text ~ '^[0-9]+$' and length(proxima_cita::text) = 13 then to_timestamp((proxima_cita::double precision / 1000.0))::date
          when proxima_cita::text ~ '^[0-9]+$' and length(proxima_cita::text) = 10 then to_timestamp(proxima_cita::double precision)::date
          when proxima_cita::text ~ '^[0-9]+$' then to_timestamp((proxima_cita::double precision / 1000.0))::date
          else proxima_cita::text::date
        end;
    end if;
  end if;

  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='lc_fecha_adaptacion') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='evaluaciones' and column_name='lc_fecha_adaptacion') <> 'date' then
      alter table public.evaluaciones
        alter column lc_fecha_adaptacion type date
        using case
          when lc_fecha_adaptacion is null then null
          when lc_fecha_adaptacion::text ~ '^[0-9]+$' and length(lc_fecha_adaptacion::text) = 13 then to_timestamp((lc_fecha_adaptacion::double precision / 1000.0))::date
          when lc_fecha_adaptacion::text ~ '^[0-9]+$' and length(lc_fecha_adaptacion::text) = 10 then to_timestamp(lc_fecha_adaptacion::double precision)::date
          when lc_fecha_adaptacion::text ~ '^[0-9]+$' then to_timestamp((lc_fecha_adaptacion::double precision / 1000.0))::date
          else lc_fecha_adaptacion::text::date
        end;
    end if;
  end if;

  -- dispensaciones.fecha y fecha_vencimiento_garantia
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='dispensaciones' and column_name='fecha') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='dispensaciones' and column_name='fecha') <> 'date' then
      alter table public.dispensaciones
        alter column fecha type date
        using case
          when fecha is null then null
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 13 then to_timestamp((fecha::double precision / 1000.0))::date
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 10 then to_timestamp(fecha::double precision)::date
          when fecha::text ~ '^[0-9]+$' then to_timestamp((fecha::double precision / 1000.0))::date
          else fecha::text::date
        end;
    end if;
  end if;

  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='dispensaciones' and column_name='fecha_vencimiento_garantia') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='dispensaciones' and column_name='fecha_vencimiento_garantia') <> 'date' then
      alter table public.dispensaciones
        alter column fecha_vencimiento_garantia type date
        using nullif(fecha_vencimiento_garantia::text, '')::date;
    end if;
  end if;

  -- servicios_extra.fecha
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='servicios_extra' and column_name='fecha') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='servicios_extra' and column_name='fecha') <> 'date' then
      alter table public.servicios_extra
        alter column fecha type date
        using case
          when fecha is null then null
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 13 then to_timestamp((fecha::double precision / 1000.0))::date
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 10 then to_timestamp(fecha::double precision)::date
          when fecha::text ~ '^[0-9]+$' then to_timestamp((fecha::double precision / 1000.0))::date
          else fecha::text::date
        end;
    end if;
  end if;

  -- pagos.fecha
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='pagos' and column_name='fecha') then
    if (select data_type from information_schema.columns where table_schema='public' and table_name='pagos' and column_name='fecha') <> 'date' then
      alter table public.pagos
        alter column fecha type date
        using case
          when fecha is null then null
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 13 then to_timestamp((fecha::double precision / 1000.0))::date
          when fecha::text ~ '^[0-9]+$' and length(fecha::text) = 10 then to_timestamp(fecha::double precision)::date
          when fecha::text ~ '^[0-9]+$' then to_timestamp((fecha::double precision / 1000.0))::date
          else fecha::text::date
        end;
    end if;
  end if;
end $$;
-- Reparacion historica (+1 dia) DESACTIVADA.
-- Motivo: el incidente ya fue corregido y no queremos riesgo de sobrecorreccion masiva.
-- Se conserva este bloque como NO-OP para mantener trazabilidad historica.
do $$
declare
  noop_marked boolean;
begin
  select exists (
    select 1 from public.schema_migrations_flags where key = 'fix_localdate_shift_plus_1_v1_deprecated_noop'
  ) into noop_marked;

  if not noop_marked then
    insert into public.schema_migrations_flags(key) values ('fix_localdate_shift_plus_1_v1_deprecated_noop');
  end if;
end $$;
commit;
