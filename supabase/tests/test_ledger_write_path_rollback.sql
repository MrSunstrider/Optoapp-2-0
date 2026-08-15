-- =============================================================================
-- Test: ledger WRITE path — trigger effects, origin moves and reversa integrity
-- Change: fix-sync-financial-ledger / WU-1E (GGA correction round 1)
--
-- Companion to test_ledger_aggregate_convergence.sql, which only inspects
-- function/constraint DEFINITIONS. This file proves BEHAVIOR: it inserts,
-- updates, moves and deletes real rows and asserts the resulting parent
-- balances and constraint rejections.
--
-- SAFETY CONTRACT
--   * Everything runs inside ONE transaction that ends in ROLLBACK. Nothing is
--     ever committed, so it is safe against any database including production.
--   * Every fixture id is prefixed `zzt_ledger_` — a namespace no client emits.
--   * Expected failures are caught in plpgsql EXCEPTION blocks (implicit
--     savepoints), so a rejection does not abort the surrounding transaction.
--   * Must run as a role that bypasses RLS (postgres / service_role); the
--     tables have RLS enabled but not FORCEd.
--
-- Requires migrations 20260815004921 (pago_effect, reversa integrity, trigger).
--
-- Run: psql -h localhost -p 54322 -U postgres -d postgres \
--        -v ON_ERROR_STOP=1 -f supabase/tests/test_ledger_write_path_rollback.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ----------------------------------------------------------------------------
-- Fixtures: two opticas so cross-tenant linking can be exercised.
-- ----------------------------------------------------------------------------
INSERT INTO public.opticas (id, nombre) VALUES
    ('zzt_ledger_optica_a', 'ZZT Ledger A'),
    ('zzt_ledger_optica_b', 'ZZT Ledger B');

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id) VALUES
    ('zzt_ledger_pac_a', 'ZZT Paciente A', DATE '2026-01-01', 'zzt_ledger_optica_a'),
    ('zzt_ledger_pac_b', 'ZZT Paciente B', DATE '2026-01-01', 'zzt_ledger_optica_b');

INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total) VALUES
    ('zzt_ledger_d1', 'zzt_ledger_pac_a', DATE '2026-01-10', 'zzt_ledger_optica_a', 500),
    ('zzt_ledger_d2', 'zzt_ledger_pac_a', DATE '2026-01-10', 'zzt_ledger_optica_a', 500),
    ('zzt_ledger_db', 'zzt_ledger_pac_b', DATE '2026-01-10', 'zzt_ledger_optica_b', 500);

INSERT INTO public.servicios_extra (id, fecha, optica_id, monto_total) VALUES
    ('zzt_ledger_s1', DATE '2026-01-10', 'zzt_ledger_optica_a', 500);

-- ----------------------------------------------------------------------------
-- W1: INSERT applies the signed effect to the correct parent.
--     Abono/Pago completo credit, Reembolso debits, Anulación is cash-neutral.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d1 NUMERIC; v_s1 NUMERIC;
BEGIN
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_p1', 'zzt_ledger_d1', DATE '2026-01-11', 'Abono', 100, 'Efectivo', 'zzt_ledger_optica_a');
    INSERT INTO public.pagos (id, servicio_extra_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_p2', 'zzt_ledger_s1', DATE '2026-01-11', 'Pago completo', 30, 'Tarjeta', 'zzt_ledger_optica_a');
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_p3', 'zzt_ledger_d1', DATE '2026-01-12', 'Reembolso', 10, 'Efectivo', 'zzt_ledger_optica_a');
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_p4', 'zzt_ledger_d1', DATE '2026-01-13', 'Anulación', 999, 'Efectivo', 'zzt_ledger_optica_a');

    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    SELECT a_cuenta INTO v_s1 FROM public.servicios_extra WHERE id = 'zzt_ledger_s1';
    ASSERT abs(v_d1 - 90) < 0.005, 'W1a FAIL: d1 expected 90 (100 - 10 + 0), got ' || v_d1;
    ASSERT abs(v_s1 - 30) < 0.005, 'W1b FAIL: s1 expected 30, got ' || v_s1;
    RAISE NOTICE 'W1 PASS: INSERT effects d1=% s1=%', v_d1, v_s1;
END;
$$;

-- ----------------------------------------------------------------------------
-- W2: UPDATE with an UNCHANGED origin takes the net-delta fast path.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d1 NUMERIC;
BEGIN
    UPDATE public.pagos SET monto = 150 WHERE id = 'zzt_ledger_p1';
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1 - 140) < 0.005, 'W2a FAIL: d1 expected 140 (90 + 50), got ' || v_d1;

    -- tipo flip Abono -> Anulación drops the whole credit (effect 0).
    UPDATE public.pagos SET tipo = 'Anulación' WHERE id = 'zzt_ledger_p1';
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1 + 10) < 0.005, 'W2b FAIL: d1 expected -10 (140 - 150), got ' || v_d1;

    UPDATE public.pagos SET tipo = 'Abono' WHERE id = 'zzt_ledger_p1';
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1 - 140) < 0.005, 'W2c FAIL: d1 expected 140 after restore, got ' || v_d1;
    RAISE NOTICE 'W2 PASS: same-origin UPDATE net delta d1=%', v_d1;
END;
$$;

-- ----------------------------------------------------------------------------
-- W3: DELETE withdraws the effect it had contributed.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d1 NUMERIC;
BEGIN
    DELETE FROM public.pagos WHERE id = 'zzt_ledger_p3';   -- Reembolso -10
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1 - 150) < 0.005, 'W3a FAIL: d1 expected 150 (140 + 10), got ' || v_d1;

    DELETE FROM public.pagos WHERE id = 'zzt_ledger_p4';   -- Anulación, effect 0
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1 - 150) < 0.005, 'W3b FAIL: deleting Anulación must not move cash, got ' || v_d1;
    RAISE NOTICE 'W3 PASS: DELETE effects d1=%', v_d1;
END;
$$;

-- ----------------------------------------------------------------------------
-- W4: ORIGIN MOVE d1 -> d2. The former parent must be debited in full, not
--     left holding cash it no longer has. This is the case a COALESCE-only
--     parent resolution silently strands.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d1 NUMERIC; v_d2 NUMERIC;
BEGIN
    UPDATE public.pagos SET dispensacion_id = 'zzt_ledger_d2' WHERE id = 'zzt_ledger_p1';
    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    SELECT monto_pagado INTO v_d2 FROM public.dispensaciones WHERE id = 'zzt_ledger_d2';
    ASSERT abs(v_d1) < 0.005, 'W4a FAIL: d1 must drop to 0 after the move, got ' || v_d1;
    ASSERT abs(v_d2 - 150) < 0.005, 'W4b FAIL: d2 must receive 150, got ' || v_d2;
    RAISE NOTICE 'W4 PASS: d1->d2 move d1=% d2=%', v_d1, v_d2;
END;
$$;

-- ----------------------------------------------------------------------------
-- W5: ORIGIN MOVE across surfaces, dispensación -> servicio extra.
--     A move that also changes monto must land the NEW effect, not a delta.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d2 NUMERIC; v_s1 NUMERIC;
BEGIN
    UPDATE public.pagos
    SET dispensacion_id = NULL, servicio_extra_id = 'zzt_ledger_s1', monto = 200
    WHERE id = 'zzt_ledger_p1';

    SELECT monto_pagado INTO v_d2 FROM public.dispensaciones WHERE id = 'zzt_ledger_d2';
    SELECT a_cuenta INTO v_s1 FROM public.servicios_extra WHERE id = 'zzt_ledger_s1';
    ASSERT abs(v_d2) < 0.005, 'W5a FAIL: d2 must drop to 0 after moving out, got ' || v_d2;
    ASSERT abs(v_s1 - 230) < 0.005, 'W5b FAIL: s1 expected 230 (30 + 200), got ' || v_s1;

    -- Move it back so later blocks work against a dispensación parent.
    UPDATE public.pagos
    SET servicio_extra_id = NULL, dispensacion_id = 'zzt_ledger_d1', monto = 150
    WHERE id = 'zzt_ledger_p1';
    SELECT a_cuenta INTO v_s1 FROM public.servicios_extra WHERE id = 'zzt_ledger_s1';
    ASSERT abs(v_s1 - 30) < 0.005, 'W5c FAIL: s1 must return to 30, got ' || v_s1;
    RAISE NOTICE 'W5 PASS: cross-surface move d2=% s1=%', v_d2, v_s1;
END;
$$;

-- ----------------------------------------------------------------------------
-- W6: monto stays a non-negative magnitude. Sign never lives in the amount.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT;
BEGIN
    BEGIN
        INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
        VALUES ('zzt_ledger_neg', 'zzt_ledger_d1', DATE '2026-01-14', 'Anulación', -50, 'Efectivo', 'zzt_ledger_optica_a');
        RAISE EXCEPTION 'W6 FAIL: a negative monto was accepted';
    EXCEPTION WHEN check_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'pagos_monto_chk',
            'W6 FAIL: expected pagos_monto_chk, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'W6 PASS: negative monto rejected by %', v_constraint;
END;
$$;

-- ----------------------------------------------------------------------------
-- W7: XOR link — Reverso requires a target, everything else forbids one.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT;
BEGIN
    BEGIN
        INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, reversa_pago_id)
        VALUES ('zzt_ledger_rv_null', 'zzt_ledger_d1', DATE '2026-01-15', 'Reverso', 10, 'Efectivo', 'zzt_ledger_optica_a', NULL);
        RAISE EXCEPTION 'W7a FAIL: Reverso without reversa_pago_id was accepted';
    EXCEPTION WHEN check_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'chk_pagos_reversa_link',
            'W7a FAIL: expected chk_pagos_reversa_link, got ' || COALESCE(v_constraint, 'null');
    END;

    BEGIN
        INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, reversa_pago_id)
        VALUES ('zzt_ledger_ab_link', 'zzt_ledger_d1', DATE '2026-01-15', 'Abono', 10, 'Efectivo', 'zzt_ledger_optica_a', 'zzt_ledger_p1');
        RAISE EXCEPTION 'W7b FAIL: non-Reverso with reversa_pago_id was accepted';
    EXCEPTION WHEN check_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'chk_pagos_reversa_link',
            'W7b FAIL: expected chk_pagos_reversa_link, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'W7 PASS: reversa XOR link enforced';
END;
$$;

-- ----------------------------------------------------------------------------
-- W8: a valid Reverso debits its parent, and only ONE may exist per original.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d1 NUMERIC; v_constraint TEXT;
BEGIN
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, reversa_pago_id)
    VALUES ('zzt_ledger_rv1', 'zzt_ledger_d1', DATE '2026-01-16', 'Reverso', 150, 'Efectivo', 'zzt_ledger_optica_a', 'zzt_ledger_p1');

    SELECT monto_pagado INTO v_d1 FROM public.dispensaciones WHERE id = 'zzt_ledger_d1';
    ASSERT abs(v_d1) < 0.005, 'W8a FAIL: a full Reverso must net d1 to 0, got ' || v_d1;

    BEGIN
        INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, reversa_pago_id)
        VALUES ('zzt_ledger_rv2', 'zzt_ledger_d1', DATE '2026-01-17', 'Reverso', 149, 'Efectivo', 'zzt_ledger_optica_a', 'zzt_ledger_p1');
        RAISE EXCEPTION 'W8b FAIL: a second Reverso for the same original was accepted';
    EXCEPTION WHEN unique_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'pagos_reversa_pago_id_uidx',
            'W8b FAIL: expected pagos_reversa_pago_id_uidx, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'W8 PASS: Reverso nets parent and stays idempotent';
END;
$$;

-- ----------------------------------------------------------------------------
-- W9: CROSS-OPTICA rejection. A Reverso in optica B may not reverse an
--     original in optica A — enforced by the composite FK, not by the client.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT; v_state TEXT;
BEGIN
    BEGIN
        INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id, reversa_pago_id)
        VALUES ('zzt_ledger_rv_x', 'zzt_ledger_db', DATE '2026-01-18', 'Reverso', 150, 'Efectivo', 'zzt_ledger_optica_b', 'zzt_ledger_p1');
        RAISE EXCEPTION 'W9 FAIL: a cross-optica Reverso link was accepted';
    EXCEPTION WHEN foreign_key_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME, v_state = RETURNED_SQLSTATE;
        ASSERT v_constraint = 'pagos_reversa_pago_id_fkey',
            'W9 FAIL: expected pagos_reversa_pago_id_fkey, got ' || COALESCE(v_constraint, 'null');
        ASSERT v_state = '23503', 'W9 FAIL: expected SQLSTATE 23503, got ' || v_state;
    END;
    RAISE NOTICE 'W9 PASS: cross-optica reversa link rejected by the schema';
END;
$$;

-- ----------------------------------------------------------------------------
-- W10: RESTRICT — an original cannot be deleted while a Reverso points at it.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT;
BEGIN
    BEGIN
        DELETE FROM public.pagos WHERE id = 'zzt_ledger_p1';
        RAISE EXCEPTION 'W10 FAIL: the reversed original was deletable';
    EXCEPTION WHEN foreign_key_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'pagos_reversa_pago_id_fkey',
            'W10 FAIL: expected pagos_reversa_pago_id_fkey, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'W10 PASS: reversed original protected by ON DELETE RESTRICT';
END;
$$;

-- ----------------------------------------------------------------------------
-- W11: the expanded estado domains accept the cancel/reclaim states that used
--      to fail remotely with CHECK 23514.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_estado TEXT;
BEGIN
    UPDATE public.servicios_extra SET estado = 'Anulado' WHERE id = 'zzt_ledger_s1';
    SELECT estado INTO v_estado FROM public.servicios_extra WHERE id = 'zzt_ledger_s1';
    ASSERT v_estado = 'Anulado', 'W11a FAIL: servicios_extra Anulado rejected';

    UPDATE public.dispensaciones SET estado_entrega = 'Anulado' WHERE id = 'zzt_ledger_d1';
    UPDATE public.dispensaciones SET estado_entrega = 'Reclamada' WHERE id = 'zzt_ledger_d2';
    SELECT estado_entrega INTO v_estado FROM public.dispensaciones WHERE id = 'zzt_ledger_d2';
    ASSERT v_estado = 'Reclamada', 'W11b FAIL: dispensaciones Reclamada rejected';

    -- Cancelling must NOT delete the pagos: the ledger keeps its history.
    ASSERT (SELECT count(*) FROM public.pagos WHERE dispensacion_id = 'zzt_ledger_d1') = 2,
        'W11c FAIL: cancelling a dispensación must keep its original + Reverso';
    RAISE NOTICE 'W11 PASS: Anulado/Reclamada accepted, ledger history kept';
END;
$$;

-- ----------------------------------------------------------------------------
-- W12: a Reembolso (reclaim refund) debits without any reversa link.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_d2 NUMERIC;
BEGIN
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_reemb', 'zzt_ledger_d2', DATE '2026-01-19', 'Abono', 80, 'Efectivo', 'zzt_ledger_optica_a');
    INSERT INTO public.pagos (id, dispensacion_id, fecha, tipo, monto, metodo_pago, optica_id)
    VALUES ('zzt_ledger_reemb2', 'zzt_ledger_d2', DATE '2026-01-20', 'Reembolso', 80, 'Efectivo', 'zzt_ledger_optica_a');

    SELECT monto_pagado INTO v_d2 FROM public.dispensaciones WHERE id = 'zzt_ledger_d2';
    ASSERT abs(v_d2) < 0.005, 'W12 FAIL: Abono 80 + Reembolso 80 must net 0, got ' || v_d2;
    RAISE NOTICE 'W12 PASS: Reembolso debits with no reversa link';
END;
$$;

-- Nothing is kept. Every fixture and every effect above disappears here.
ROLLBACK;
