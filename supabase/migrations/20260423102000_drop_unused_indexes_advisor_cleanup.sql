-- Limpieza de índices sin uso reportados por Supabase advisor.
-- Todos son índices no ligados a constraints (safe drop).

drop index if exists public.idx_evaluaciones_paciente_id;
drop index if exists public.idx_dispensaciones_paciente_id;
drop index if exists public.idx_servicios_extra_paciente_id;
drop index if exists public.idx_pagos_dispensacion_id;
drop index if exists public.idx_pagos_servicio_extra_id;
drop index if exists public.idx_montura_movimientos_montura_id;
drop index if exists public.idx_monturas_optica_id;
drop index if exists public.idx_montura_movimientos_fecha;
