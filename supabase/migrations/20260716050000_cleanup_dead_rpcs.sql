-- ============================================================================
-- Migration: Cleanup dead RPCs
--
-- Drops functions that are no longer used by the Android app, confirmed by
-- grep across the codebase. These were either superseded by newer RPCs or
-- were never called from client code.
--
-- NOTE: rpc_count_pendientes is NOT included — already dropped separately.
-- ============================================================================

DROP FUNCTION IF EXISTS public.rpc_pacientes_con_saldo(TEXT);
DROP FUNCTION IF EXISTS public.rpc_pacientes_con_entrega_pendiente(TEXT);
DROP FUNCTION IF EXISTS public.rpc_adjust_montura_stock(TEXT, TEXT, INT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.suggest_next_ho(UUID);
DROP FUNCTION IF EXISTS public.sync_snapshot(UUID);
DROP FUNCTION IF EXISTS public.check_rate_limit(TEXT, INT, INT);
DROP FUNCTION IF EXISTS public.paciente_eliminaciones_restantes_hoy(UUID);
