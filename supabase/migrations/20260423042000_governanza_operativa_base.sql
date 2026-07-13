-- Gobernanza de datos:
-- 1) Unicidad por óptica para HO (pacientes) y OT (servicios_extra + dispensaciones)
-- 2) Normalización trim
-- 3) Auditoría básica updated_at / updated_by

-- Normalización básica de textos clave
update public.pacientes
set historia_optometrica = nullif(btrim(coalesce(historia_optometrica, '')), '')
where historia_optometrica is distinct from nullif(btrim(coalesce(historia_optometrica, '')), '');
update public.dispensaciones
set ot = btrim(coalesce(ot, ''))
where ot is distinct from btrim(coalesce(ot, ''));
update public.servicios_extra
set ot = btrim(coalesce(ot, ''))
where ot is distinct from btrim(coalesce(ot, ''));
-- Desambiguar duplicados HO por óptica antes del índice único
with dup as (
  select id, row_number() over (
    partition by optica_id, upper(btrim(historia_optometrica))
    order by id
  ) as rn
  from public.pacientes
  where nullif(btrim(coalesce(historia_optometrica, '')), '') is not null
)
update public.pacientes p
set historia_optometrica = p.historia_optometrica || '-DUP-' || right(p.id, 4)
from dup
where dup.id = p.id
  and dup.rn > 1;
-- Desambiguar duplicados OT en servicios_extra
with dup as (
  select id, row_number() over (
    partition by optica_id, upper(btrim(ot))
    order by id
  ) as rn
  from public.servicios_extra
  where nullif(btrim(coalesce(ot, '')), '') is not null
)
update public.servicios_extra s
set ot = s.ot || '-DUP-' || right(s.id, 4)
from dup
where dup.id = s.id
  and dup.rn > 1;
-- Desambiguar duplicados OT en dispensaciones
with dup as (
  select id, row_number() over (
    partition by optica_id, upper(btrim(ot))
    order by id
  ) as rn
  from public.dispensaciones
  where nullif(btrim(coalesce(ot, '')), '') is not null
)
update public.dispensaciones d
set ot = d.ot || '-DUP-' || right(d.id, 4)
from dup
where dup.id = d.id
  and dup.rn > 1;
-- Índices únicos por óptica
create unique index if not exists pacientes_optica_historia_optometrica_uq
on public.pacientes(optica_id, upper(btrim(historia_optometrica)))
where nullif(btrim(coalesce(historia_optometrica, '')), '') is not null;
create unique index if not exists dispensaciones_optica_ot_uq_v2
on public.dispensaciones(optica_id, upper(btrim(ot)))
where nullif(btrim(coalesce(ot, '')), '') is not null;
create unique index if not exists servicios_extra_optica_ot_uq
on public.servicios_extra(optica_id, upper(btrim(ot)))
where nullif(btrim(coalesce(ot, '')), '') is not null;
-- Auditoría básica
create or replace function public.set_updated_audit_fields()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := timezone('utc', now());
  begin
    new.updated_by := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;
do $$
declare
  t text;
begin
  foreach t in array array[
    'pacientes',
    'evaluaciones',
    'dispensaciones',
    'servicios_extra',
    'pagos'
  ]
  loop
    execute format('alter table public.%I add column if not exists updated_at timestamptz not null default timezone(''utc'', now())', t);
    execute format('alter table public.%I add column if not exists updated_by uuid', t);
    execute format('drop trigger if exists trg_%I_set_updated_audit on public.%I', t, t);
    execute format(
      'create trigger trg_%I_set_updated_audit before update on public.%I for each row execute function public.set_updated_audit_fields()',
      t, t
    );
  end loop;
end $$;
comment on function public.set_updated_audit_fields is
'Actualiza updated_at y updated_by en modificaciones de tablas operativas.';
