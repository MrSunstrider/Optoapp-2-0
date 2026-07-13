-- ============================================================================
-- Migration: Drop rpc_saldo_pendiente (dead code)
--
-- rpc_saldo_pendiente has been DEPRECATED since July 6. It references
-- public.ventas (dropped July 10). Zero callers across the Android codebase
-- (confirmed by grep). The pending balance is available via:
--   - rpc_analisis_mensual() → 'saldo_pendiente' field
--   - rpc_deudores() → per-debtor breakdown
-- ============================================================================

DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT);
