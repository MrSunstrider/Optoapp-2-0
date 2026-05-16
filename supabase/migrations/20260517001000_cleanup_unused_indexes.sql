-- Limpia índices no utilizados reportados por Security Advisor
DROP INDEX IF EXISTS public.idx_invitaciones_codigo;
DROP INDEX IF EXISTS public.idx_servicios_extra_optica_updated_at;
DROP INDEX IF EXISTS public.idx_evaluaciones_optica_updated_at;
DROP INDEX IF EXISTS public.idx_dispensaciones_optica_updated_at;
DROP INDEX IF EXISTS public.idx_pagos_optica_updated_at;
DROP INDEX IF EXISTS public.idx_sync_telemetry_optica_last_actor;
DROP INDEX IF EXISTS public.idx_pacientes_delete_audit_deleted_by;
DROP INDEX IF EXISTS public.idx_cierres_caja_closed_by;
DROP INDEX IF EXISTS public.idx_cierres_caja_reopened_by;
