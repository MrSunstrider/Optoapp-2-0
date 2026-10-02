-- =============================================================================
-- Test: anulacion server guards — one claim per original, traced deletes
-- Feature: odd/tasks/anulaciones-server-guards.md (T1)
--
-- SAFETY CONTRACT
--   * Everything runs inside ONE transaction that ends in ROLLBACK. Nothing is
--     ever committed, so it is safe against any database including production.
--   * Every fixture id is prefixed `zzt_guard_` — a namespace no client emits.
--   * Expected failures are caught in plpgsql EXCEPTION blocks (implicit
--     savepoints), so a rejection does not abort the surrounding transaction.
--   * Must run as a role that bypasses RLS and owns public.pacientes
--     (postgres): G5 disables trg_guard_pacientes_delete inside the
--     transaction because it requires an authenticated admin session.
--
-- Requires migration 20261002034600_anulacion_server_guards.sql.
--
-- Run: psql -h localhost -p 54322 -U postgres -d postgres \
--        -v ON_ERROR_STOP=1 -f supabase/tests/test_anulacion_server_guards.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_guard_optica', 'ZZT Guard');

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id) VALUES
    ('zzt_guard_pac', 'ZZT Paciente', DATE '2026-01-01', 'zzt_guard_optica');

INSERT INTO public.monturas (id, optica_id, stock_actual) VALUES
    ('zzt_guard_mon', 'zzt_guard_optica', 10);

INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total) VALUES
    ('zzt_guard_orig',   'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500),
    ('zzt_guard_clean',  'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500),
    ('zzt_guard_paid',   'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500),
    ('zzt_guard_sold',   'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500),
    ('zzt_guard_restk',  'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500),
    ('zzt_guard_gift',   'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500);

-- ----------------------------------------------------------------------------
-- G1: one replacement per claimed original; a second one hits the index.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT;
BEGIN
    INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total, reclamo_origen_id)
    VALUES ('zzt_guard_r1', 'zzt_guard_pac', DATE '2026-01-11', 'zzt_guard_optica', 500, 'zzt_guard_orig');
    BEGIN
        INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total, reclamo_origen_id)
        VALUES ('zzt_guard_r2', 'zzt_guard_pac', DATE '2026-01-11', 'zzt_guard_optica', 500, 'zzt_guard_orig');
        RAISE EXCEPTION 'G1 FAIL: a second replacement for the same original was accepted';
    EXCEPTION WHEN unique_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'dispensaciones_reclamo_origen_uidx',
            'G1 FAIL: expected dispensaciones_reclamo_origen_uidx, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'G1 PASS: duplicate claim rejected by %', v_constraint;
END;
$$;

-- ----------------------------------------------------------------------------
-- G2: a dispensacion without any trace can still be deleted directly.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    DELETE FROM public.dispensaciones WHERE id = 'zzt_guard_clean';
    ASSERT NOT EXISTS (SELECT 1 FROM public.dispensaciones WHERE id = 'zzt_guard_clean'),
        'G2 FAIL: a traceless dispensacion must stay deletable';
    RAISE NOTICE 'G2 PASS: traceless delete allowed';
END;
$$;

-- ----------------------------------------------------------------------------
-- G3: each trace kind blocks a direct delete with SQLSTATE P0001 and the
--     stable `dispensacion_has_trace` token the Android client matches.
--     G3a pago, G3b sale movimiento, G3c '<id>:anul:...' restock,
--     G3d regalo movimiento.
-- ----------------------------------------------------------------------------
CREATE FUNCTION pg_temp.zzt_assert_delete_refused(p_id TEXT, p_label TEXT) RETURNS VOID
LANGUAGE plpgsql AS $$
DECLARE
    v_state TEXT;
    v_message TEXT;
BEGIN
    BEGIN
        DELETE FROM public.dispensaciones WHERE id = p_id;
        RAISE EXCEPTION '% FAIL: traced dispensacion % was deleted', p_label, p_id;
    EXCEPTION WHEN raise_exception THEN
        GET STACKED DIAGNOSTICS v_state = RETURNED_SQLSTATE, v_message = MESSAGE_TEXT;
        ASSERT v_state = 'P0001', p_label || ' FAIL: expected P0001, got ' || v_state;
        ASSERT v_message LIKE 'dispensacion_has_trace%',
            p_label || ' FAIL: expected dispensacion_has_trace token, got ' || v_message;
    END;
    ASSERT EXISTS (SELECT 1 FROM public.dispensaciones WHERE id = p_id),
        p_label || ' FAIL: the refused dispensacion must survive';
    RAISE NOTICE '% PASS: delete of % refused with dispensacion_has_trace', p_label, p_id;
END;
$$;

INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
VALUES ('zzt_guard_p1', 'zzt_guard_paid', DATE '2026-01-11', 'Abono', 100, 'Efectivo', 'zzt_guard_optica');

INSERT INTO public.regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
VALUES ('zzt_guard_reg', 'zzt_guard_gift', 'zzt_guard_mon', 1, 10, 'ZZT regalo', 'zzt_guard_optica');

INSERT INTO public.montura_movimientos (id, montura_id, fecha, tipo, cantidad, stock_previo, stock_nuevo, referencia_id, nota, optica_id) VALUES
    ('zzt_guard_m1', 'zzt_guard_mon', DATE '2026-01-11', 'SALIDA_VENTA', 1, 10, 9, 'zzt_guard_sold', '', 'zzt_guard_optica'),
    ('zzt_guard_m2', 'zzt_guard_mon', DATE '2026-01-11', 'ENTRADA', 1, 9, 10, 'zzt_guard_restk:anul:item1', '', 'zzt_guard_optica'),
    ('zzt_guard_m3', 'zzt_guard_mon', DATE '2026-01-11', 'SALIDA_VENTA', 1, 10, 9, 'zzt_guard_reg', '', 'zzt_guard_optica');

SELECT pg_temp.zzt_assert_delete_refused('zzt_guard_paid', 'G3a pago');
SELECT pg_temp.zzt_assert_delete_refused('zzt_guard_sold', 'G3b sale movimiento');
SELECT pg_temp.zzt_assert_delete_refused('zzt_guard_restk', 'G3c anulacion restock');
SELECT pg_temp.zzt_assert_delete_refused('zzt_guard_gift', 'G3d regalo movimiento');

DO $$
BEGIN
    ASSERT EXISTS (SELECT 1 FROM public.pagos WHERE id = 'zzt_guard_p1'),
        'G3 FAIL: a refused delete must not cascade to pagos';
END;
$$;

-- ----------------------------------------------------------------------------
-- G4: a movimiento of ANOTHER optica with a colliding reference does not block.
-- ----------------------------------------------------------------------------
INSERT INTO public.opticas (id, nombre) VALUES ('zzt_guard_optica_b', 'ZZT Guard B');
INSERT INTO public.monturas (id, optica_id, stock_actual) VALUES ('zzt_guard_mon_b', 'zzt_guard_optica_b', 5);
INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total) VALUES
    ('zzt_guard_other', 'zzt_guard_pac', DATE '2026-01-10', 'zzt_guard_optica', 500);
INSERT INTO public.montura_movimientos (id, montura_id, fecha, tipo, cantidad, stock_previo, stock_nuevo, referencia_id, nota, optica_id)
VALUES ('zzt_guard_mb', 'zzt_guard_mon_b', DATE '2026-01-11', 'SALIDA_VENTA', 1, 5, 4, 'zzt_guard_other', '', 'zzt_guard_optica_b');

DO $$
BEGIN
    DELETE FROM public.dispensaciones WHERE id = 'zzt_guard_other';
    ASSERT NOT EXISTS (SELECT 1 FROM public.dispensaciones WHERE id = 'zzt_guard_other'),
        'G4 FAIL: a foreign-optica movimiento must not block the delete';
    RAISE NOTICE 'G4 PASS: trace check is optica-scoped';
END;
$$;

-- ----------------------------------------------------------------------------
-- G5: a paciente delete still cascades through traced dispensaciones,
--     including one with pagos (zzt_guard_paid / zzt_guard_p1).
-- ----------------------------------------------------------------------------
ALTER TABLE public.pacientes DISABLE TRIGGER trg_guard_pacientes_delete;

DO $$
BEGIN
    DELETE FROM public.pacientes WHERE id = 'zzt_guard_pac';
    ASSERT NOT EXISTS (SELECT 1 FROM public.dispensaciones WHERE paciente_id = 'zzt_guard_pac'),
        'G5 FAIL: the paciente cascade must remove its dispensaciones';
    ASSERT NOT EXISTS (SELECT 1 FROM public.pagos WHERE id = 'zzt_guard_p1'),
        'G5 FAIL: the paciente cascade must remove the dispensacion pagos';
    RAISE NOTICE 'G5 PASS: paciente cascade allowed through the guard';
END;
$$;

ROLLBACK;
