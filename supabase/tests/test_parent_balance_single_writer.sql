-- Dual-writer: parent cache already equals SUM(pago_effect), then INSERT Abono.
-- Incremental += doubles; SUM recompute must keep 100.
--
-- SAFETY CONTRACT
--   * ONE transaction ending in ROLLBACK — safe against production.
--   * Fixture ids prefixed `zzt_dual_` — a namespace no client emits.
--   * Must run as a role that bypasses RLS (postgres / service_role).
--
-- Requires migration 20260818223000_parent_balance_sum_recompute.
--
-- Run: psql -h localhost -p 54322 -U postgres -d postgres \
--        -v ON_ERROR_STOP=1 -f supabase/tests/test_parent_balance_single_writer.sql

\set ON_ERROR_STOP on
BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_dual_opt', 'ZZT Dual Writer');
INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_dual_pac', 'ZZT Dual Pac', DATE '2026-01-01', 'zzt_dual_opt');
INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total, monto_pagado)
VALUES ('zzt_dual_d1', 'zzt_dual_pac', DATE '2026-08-18', 'zzt_dual_opt', 170, 100);
INSERT INTO public.servicios_extra (id, fecha, optica_id, monto_total, a_cuenta, descripcion)
VALUES ('zzt_dual_s1', DATE '2026-08-18', 'zzt_dual_opt', 25, 25, 'ZZT Dual Serv');

DO $$
DECLARE v_d NUMERIC; v_s NUMERIC;
BEGIN
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_dual_p1', 'zzt_dual_d1', DATE '2026-08-18', 'Abono', 100, 'Efectivo', 'zzt_dual_opt');
    INSERT INTO public.pagos (id, servicio_extra_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_dual_p2', 'zzt_dual_s1', DATE '2026-08-18', 'Abono', 25, 'Yape', 'zzt_dual_opt');

    SELECT monto_pagado INTO v_d FROM public.dispensaciones WHERE id = 'zzt_dual_d1';
    SELECT a_cuenta INTO v_s FROM public.servicios_extra WHERE id = 'zzt_dual_s1';
    ASSERT abs(v_d - 100) < 0.005, 'DUAL-D FAIL: expected monto_pagado 100, got ' || v_d;
    ASSERT abs(v_s - 25) < 0.005, 'DUAL-S FAIL: expected a_cuenta 25, got ' || v_s;
    RAISE NOTICE 'DUAL PASS: disp=% serv=%', v_d, v_s;
END;
$$;

ROLLBACK;
