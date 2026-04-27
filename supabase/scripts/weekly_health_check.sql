-- OptoApp weekly health-check
-- Ejecutar con rol de mantenimiento (service role / SQL editor) 1 vez por semana.
-- Secciones:
-- 1) Integridad referencial y nulos críticos
-- 2) Duplicados operativos por óptica
-- 3) Estado de sincronización remota
-- 4) Drift de índices/constraints/trigger de auditoría

-- ============================================================================
-- 1) Integridad referencial y nulos críticos
-- ============================================================================
with checks as (
  select 'pacientes_sin_optica' as check_name, count(*)::bigint as total
  from public.pacientes where optica_id is null
  union all
  select 'evaluaciones_sin_optica', count(*)::bigint
  from public.evaluaciones where optica_id is null
  union all
  select 'dispensaciones_sin_optica', count(*)::bigint
  from public.dispensaciones where optica_id is null
  union all
  select 'servicios_extra_sin_optica', count(*)::bigint
  from public.servicios_extra where optica_id is null
  union all
  select 'pagos_sin_optica', count(*)::bigint
  from public.pagos where optica_id is null
  union all
  select 'evaluaciones_sin_paciente_fk', count(*)::bigint
  from public.evaluaciones e
  left join public.pacientes p on p.id = e.paciente_id
  where p.id is null
  union all
  select 'dispensaciones_sin_paciente_fk', count(*)::bigint
  from public.dispensaciones d
  left join public.pacientes p on p.id = d.paciente_id
  where p.id is null
  union all
  select 'servicios_con_paciente_fk_invalido', count(*)::bigint
  from public.servicios_extra s
  where s.paciente_id is not null
    and not exists (
      select 1 from public.pacientes p where p.id = s.paciente_id
    )
  union all
  select 'pagos_huerfanos', count(*)::bigint
  from public.pagos pg
  left join public.dispensaciones d on d.id = pg.dispensacion_id
  left join public.servicios_extra s on s.id = pg.servicio_extra_id
  where (pg.dispensacion_id is not null and d.id is null)
     or (pg.servicio_extra_id is not null and s.id is null)
)
select * from checks order by check_name;

-- ============================================================================
-- 2) Duplicados operativos por óptica (debe retornar 0 filas)
-- ============================================================================
-- Historia optométrica duplicada por óptica
select
  optica_id,
  upper(btrim(historia_optometrica)) as historia_key,
  count(*) as total
from public.pacientes
where nullif(btrim(coalesce(historia_optometrica, '')), '') is not null
group by optica_id, upper(btrim(historia_optometrica))
having count(*) > 1
order by total desc, optica_id;

-- OT duplicada por óptica en dispensaciones
select
  optica_id,
  upper(btrim(ot)) as ot_key,
  count(*) as total
from public.dispensaciones
where nullif(btrim(coalesce(ot, '')), '') is not null
group by optica_id, upper(btrim(ot))
having count(*) > 1
order by total desc, optica_id;

-- OT duplicada por óptica en servicios_extra
select
  optica_id,
  upper(btrim(ot)) as ot_key,
  count(*) as total
from public.servicios_extra
where nullif(btrim(coalesce(ot, '')), '') is not null
group by optica_id, upper(btrim(ot))
having count(*) > 1
order by total desc, optica_id;

-- ============================================================================
-- 3) Estado de sincronización remota por óptica
-- ============================================================================
select
  o.id as optica_id,
  o.nombre as optica_nombre,
  t.last_status,
  t.last_stage,
  t.last_sync_at,
  t.updated_at as telemetry_updated_at,
  t.last_error
from public.opticas o
left join public.sync_telemetry_optica t on t.optica_id = o.id
order by o.nombre;

-- Detecta ópticas sin telemetría remota
select o.id, o.nombre
from public.opticas o
left join public.sync_telemetry_optica t on t.optica_id = o.id
where t.optica_id is null
order by o.nombre;

-- ============================================================================
-- 4) Drift de estructura esperada (índices/constraints/trigger)
-- ============================================================================
-- Índices compuestos esperados por tenant + fecha/updated_at
select expected_index
from (
  values
    ('idx_pacientes_optica_updated_at'),
    ('idx_evaluaciones_optica_fecha'),
    ('idx_evaluaciones_optica_updated_at'),
    ('idx_dispensaciones_optica_fecha'),
    ('idx_dispensaciones_optica_updated_at'),
    ('idx_servicios_extra_optica_fecha'),
    ('idx_servicios_extra_optica_updated_at'),
    ('idx_pagos_optica_fecha'),
    ('idx_pagos_optica_updated_at')
) as e(expected_index)
where not exists (
  select 1
  from pg_indexes i
  where i.schemaname = 'public'
    and i.indexname = e.expected_index
);

-- Constraints de dominio de estado esperados
select expected_constraint
from (
  values
    ('dispensaciones_estado_entrega_domain_chk'),
    ('servicios_extra_estado_domain_chk')
) as e(expected_constraint)
where not exists (
  select 1
  from pg_constraint c
  where c.conname = e.expected_constraint
);

-- Trigger de auditoría expected en tabla de telemetría remota
select 'trg_sync_telemetry_optica_audit' as expected_trigger
where not exists (
  select 1
  from pg_trigger t
  join pg_class c on c.oid = t.tgrelid
  join pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public'
    and c.relname = 'sync_telemetry_optica'
    and t.tgname = 'trg_sync_telemetry_optica_audit'
    and not t.tgisinternal
);

