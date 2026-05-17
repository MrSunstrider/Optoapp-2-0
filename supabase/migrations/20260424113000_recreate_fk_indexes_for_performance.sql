-- Reponer índices de cobertura para foreign keys reportadas por advisor.
-- Objetivo: mejorar performance en joins, filtros y cascadas sobre FK.
-- Diseño: idempotente con IF NOT EXISTS, sin cambios de datos.

create index if not exists idx_dispensaciones_paciente_id
  on public.dispensaciones using btree (paciente_id);

create index if not exists idx_evaluaciones_paciente_id
  on public.evaluaciones using btree (paciente_id);

create index if not exists idx_servicios_extra_paciente_id
  on public.servicios_extra using btree (paciente_id);

create index if not exists idx_pagos_dispensacion_id
  on public.pagos using btree (dispensacion_id);

create index if not exists idx_pagos_servicio_extra_id
  on public.pagos using btree (servicio_extra_id);

create index if not exists idx_montura_movimientos_montura_id
  on public.montura_movimientos using btree (montura_id);
