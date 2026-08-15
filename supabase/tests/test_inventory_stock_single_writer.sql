-- =============================================================================
-- Test: inventory single stock writer — purge predicate, RPC guard, idempotency
-- Change: fix-inventory-double-stock-writer / WU-4
--
-- SAFETY CONTRACT
--   * Everything runs inside ONE transaction that ends in ROLLBACK. Nothing is
--     ever committed, so it is safe against any database including production.
--   * Every fixture id is prefixed `zzt_inv_` — a namespace no client emits.
--   * The purge is exercised against FIXTURES, never against real rows: the
--     DELETE below is scoped by `optica_id LIKE 'zzt_inv_%'` on top of the
--     production predicate, so a run cannot destroy tenant data even by mistake.
--   * Expected failures are caught in plpgsql EXCEPTION blocks (implicit
--     savepoints), so a rejection does not abort the surrounding transaction.
--   * Must run as a role that bypasses RLS (postgres / service_role).
--
-- Requires migration 20260815*_inventory_single_writer_purge_and_guard.sql.
--
-- Run: psql -h localhost -p 54322 -U postgres -d postgres \
--        -v ON_ERROR_STOP=1 -f supabase/tests/test_inventory_stock_single_writer.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ----------------------------------------------------------------------------
-- Fixtures: two opticas so the tenant guard can be exercised. The acting user
-- is an existing auth user (no PII hardcoded) made 'admin' of optica A only.
-- ----------------------------------------------------------------------------
INSERT INTO public.opticas (id, nombre) VALUES
    ('zzt_inv_optica_a', 'ZZT Inv A'),
    ('zzt_inv_optica_b', 'ZZT Inv B');

-- Fail loudly instead of letting every B-block report a misleading 'forbidden':
-- with no auth user there is no subject to put in the JWT, so auth.uid() is NULL
-- and the role guard denies everything.
DO $$
BEGIN
    ASSERT EXISTS (SELECT 1 FROM auth.users),
        'FIXTURE FAIL: auth.users is empty. Seed one user before running this file — '
        'without a subject the JWT has no sub, auth.uid() is NULL and every B-block '
        'would fail as forbidden for the wrong reason.';
END;
$$;

INSERT INTO public.usuario_optica (user_id, optica_id, rol)
SELECT id, 'zzt_inv_optica_a', 'asesor' FROM auth.users ORDER BY created_at LIMIT 1;

INSERT INTO public.monturas (id, sku, stock_actual, optica_id) VALUES
    ('zzt_inv_m1', 'ZZT-1', 5, 'zzt_inv_optica_a'),
    ('zzt_inv_m2', 'ZZT-2', 0, 'zzt_inv_optica_a'),
    ('zzt_inv_mb', 'ZZT-B', 5, 'zzt_inv_optica_b');

-- Act as that user for the whole transaction.
SELECT set_config(
    'request.jwt.claims',
    json_build_object('sub', (SELECT user_id::text FROM public.usuario_optica WHERE optica_id = 'zzt_inv_optica_a'))::text,
    true
);

-- ----------------------------------------------------------------------------
-- D1: the RPC must carry a role guard. SECURITY DEFINER bypasses the
--     montura_movimientos_insert RLS policy, so the check has to be explicit.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_src TEXT;
BEGIN
    SELECT pg_get_functiondef(p.oid) INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_adjust_montura_stock';

    ASSERT v_src IS NOT NULL, 'D1 FAIL: rpc_adjust_montura_stock is missing';
    ASSERT v_src LIKE '%has_optica_role%', 'D1a FAIL: role guard absent';
    ASSERT v_src LIKE '%forbidden%', 'D1b FAIL: guard must return error=forbidden';
    ASSERT v_src LIKE '%FOR UPDATE%', 'D1c FAIL: montura row must be locked before the read-modify-write';
    ASSERT v_src LIKE '%ON CONFLICT%', 'D1d FAIL: movement insert must tolerate a lost race';
    ASSERT v_src LIKE '%SECURITY DEFINER%', 'D1e FAIL: SECURITY DEFINER must be preserved';
    RAISE NOTICE 'D1 PASS: RPC carries guard, lock and conflict tolerance';
END;
$$;

-- ----------------------------------------------------------------------------
-- D2: no mutate-then-compensate. The old body decremented, then restored the
--     previous value when the result went negative — visible to any concurrent
--     reader in between.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_src TEXT;
BEGIN
    SELECT pg_get_functiondef(p.oid) INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_adjust_montura_stock';

    ASSERT v_src NOT LIKE '%stock_actual = v_old_stock%',
        'D2 FAIL: insufficiency must be refused before the update, not compensated after it';
    RAISE NOTICE 'D2 PASS: no mutate-then-compensate path';
END;
$$;

-- ----------------------------------------------------------------------------
-- D3: sale-alias recognition. 'SALIDA_VENTA' (local save) and 'venta' (retired
--     sync writer) are the same business fact under one referencia_id. Without
--     this, purging the 'venta' rows lets any client still on the old build
--     re-apply the decrement as if it were a new sale.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_src TEXT;
BEGIN
    SELECT pg_get_functiondef(p.oid) INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_adjust_montura_stock';

    ASSERT v_src LIKE '%salida_venta%', 'D3a FAIL: sale aliases are not recognised';
    ASSERT v_src LIKE '%optica_id = p_optica_id%', 'D3b FAIL: identity lookup must be tenant-scoped';
    RAISE NOTICE 'D3 PASS: sale aliases collapse to one fact, tenant-scoped';
END;
$$;

-- ----------------------------------------------------------------------------
-- I1: the identity index stays. It is what kept stock from collapsing while the
--     second writer was replaying every dispensación on every sync.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_def TEXT;
BEGIN
    SELECT indexdef INTO v_def FROM pg_indexes
    WHERE schemaname = 'public' AND indexname = 'idx_movimientos_conflict';

    ASSERT v_def IS NOT NULL, 'I1a FAIL: idx_movimientos_conflict was dropped';
    ASSERT v_def LIKE 'CREATE UNIQUE INDEX%', 'I1b FAIL: index must stay UNIQUE';
    ASSERT v_def LIKE '%referencia_id%' AND v_def LIKE '%tipo%' AND v_def LIKE '%montura_id%',
        'I1c FAIL: index must cover (referencia_id, tipo, montura_id)';
    RAISE NOTICE 'I1 PASS: movement identity index retained';
END;
$$;

-- ----------------------------------------------------------------------------
-- B1: a caller with no role in the target óptica is refused, and nothing moves.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER; v_movs BIGINT;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_mb', 'zzt_inv_optica_b', -1, 'zzt_inv_ref_b', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'false', 'B1a FAIL: cross-tenant call was accepted: ' || v_res::text;
    ASSERT v_res->>'error' = 'forbidden', 'B1b FAIL: expected forbidden, got ' || COALESCE(v_res->>'error', 'null');

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_mb';
    SELECT count(*) INTO v_movs FROM public.montura_movimientos WHERE montura_id = 'zzt_inv_mb';
    ASSERT v_stock = 5, 'B1c FAIL: denied call moved stock to ' || v_stock;
    ASSERT v_movs = 0, 'B1d FAIL: denied call wrote ' || v_movs || ' movement(s)';
    RAISE NOTICE 'B1 PASS: cross-tenant call denied with no side effect';
END;
$$;

-- ----------------------------------------------------------------------------
-- B2: the happy path decrements exactly once and records exactly one movement.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER; v_movs BIGINT;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_m1', 'zzt_inv_optica_a', -1, 'zzt_inv_ref1', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'true', 'B2a FAIL: authorized call refused: ' || v_res::text;
    ASSERT (v_res->>'new_stock')::int = 4, 'B2b FAIL: expected new_stock 4, got ' || COALESCE(v_res->>'new_stock', 'null');

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m1';
    SELECT count(*) INTO v_movs FROM public.montura_movimientos
    WHERE referencia_id = 'zzt_inv_ref1' AND tipo = 'venta' AND montura_id = 'zzt_inv_m1';
    ASSERT v_stock = 4, 'B2c FAIL: stock expected 4, got ' || v_stock;
    ASSERT v_movs = 1, 'B2d FAIL: expected 1 movement, got ' || v_movs;
    RAISE NOTICE 'B2 PASS: single application recorded once';
END;
$$;

-- ----------------------------------------------------------------------------
-- B3: replay is a no-op, NOT a 23505. This is the flood the device produced:
--     24 identical calls per sync, each aborting the function.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER; v_movs BIGINT;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_m1', 'zzt_inv_optica_a', -1, 'zzt_inv_ref1', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'true', 'B3a FAIL: replay must succeed as a no-op, got ' || v_res::text;
    ASSERT v_res->>'idempotent' = 'true', 'B3b FAIL: replay must be flagged idempotent, got ' || v_res::text;

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m1';
    SELECT count(*) INTO v_movs FROM public.montura_movimientos
    WHERE referencia_id = 'zzt_inv_ref1' AND tipo = 'venta' AND montura_id = 'zzt_inv_m1';
    ASSERT v_stock = 4, 'B3c FAIL: replay double-decremented to ' || v_stock;
    ASSERT v_movs = 1, 'B3d FAIL: replay duplicated the movement (' || v_movs || ' rows)';
    RAISE NOTICE 'B3 PASS: replay is idempotent, no duplicate key raised';
END;
$$;

-- ----------------------------------------------------------------------------
-- B4: insufficiency is refused BEFORE any write.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER; v_movs BIGINT;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_m2', 'zzt_inv_optica_a', -1, 'zzt_inv_ref2', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'false', 'B4a FAIL: overdraw was accepted: ' || v_res::text;
    ASSERT v_res->>'error' = 'insufficient', 'B4b FAIL: expected insufficient, got ' || COALESCE(v_res->>'error', 'null');

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m2';
    SELECT count(*) INTO v_movs FROM public.montura_movimientos WHERE montura_id = 'zzt_inv_m2';
    ASSERT v_stock = 0, 'B4c FAIL: refused overdraw left stock at ' || v_stock;
    ASSERT v_movs = 0, 'B4d FAIL: refused overdraw wrote ' || v_movs || ' movement(s)';
    RAISE NOTICE 'B4 PASS: insufficiency refused with no mutation';
END;
$$;

-- ----------------------------------------------------------------------------
-- B5: an unknown montura inside a legitimate óptica is not_found, not forbidden.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_missing', 'zzt_inv_optica_a', -1, 'zzt_inv_ref3', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'false', 'B5a FAIL: unknown montura was accepted';
    ASSERT v_res->>'error' = 'not_found', 'B5b FAIL: expected not_found, got ' || COALESCE(v_res->>'error', 'null');
    RAISE NOTICE 'B5 PASS: unknown montura reported as not_found';
END;
$$;

-- ----------------------------------------------------------------------------
-- B6: THE POST-PURGE CASE. A sale already recorded as 'SALIDA_VENTA' by the
--     local save, with its phantom 'venta' twin purged. A client still on the
--     old build calls with tipo='venta'. Before the sale-alias fix this passed
--     the identity check as brand new and decremented stock a second time.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER; v_venta BIGINT;
BEGIN
    INSERT INTO public.montura_movimientos
        (id, montura_id, fecha, tipo, cantidad, stock_previo, stock_nuevo, referencia_id, nota, optica_id)
    VALUES
        ('zzt_inv_localsale', 'zzt_inv_m1', DATE '2026-08-14', 'SALIDA_VENTA', 1, 4, 3, 'zzt_inv_sref', '', 'zzt_inv_optica_a');

    UPDATE public.monturas SET stock_actual = 3 WHERE id = 'zzt_inv_m1';

    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_m1', 'zzt_inv_optica_a', -1, 'zzt_inv_sref', 'venta_dispensacion', 'venta', '2026-08-14');

    ASSERT v_res->>'ok' = 'true', 'B6a FAIL: an already-sold unit must not error, got ' || v_res::text;
    ASSERT v_res->>'idempotent' = 'true',
        'B6b FAIL: a SALIDA_VENTA twin must be recognised as the same fact, got ' || v_res::text;

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m1';
    SELECT count(*) INTO v_venta FROM public.montura_movimientos
    WHERE referencia_id = 'zzt_inv_sref' AND tipo = 'venta';

    ASSERT v_stock = 3, 'B6c FAIL: stock was decremented a second time, now ' || v_stock;
    ASSERT v_venta = 0, 'B6d FAIL: a phantom venta row was re-created (' || v_venta || ' row(s))';
    RAISE NOTICE 'B6 PASS: old-build sale replay is a no-op after the purge';
END;
$$;

-- ----------------------------------------------------------------------------
-- B7: the alias collapse must NOT swallow unrelated movement types. An AJUSTE
--     sharing a referencia_id with a sale is a different fact and must apply.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_res JSONB; v_stock INTEGER;
BEGIN
    v_res := public.rpc_adjust_montura_stock(
        'zzt_inv_m1', 'zzt_inv_optica_a', 2, 'zzt_inv_sref', 'ajuste_manual', 'AJUSTE', '2026-08-14');

    ASSERT v_res->>'ok' = 'true', 'B7a FAIL: AJUSTE refused: ' || v_res::text;
    ASSERT v_res->>'idempotent' IS NULL, 'B7b FAIL: AJUSTE must not be treated as the sale, got ' || v_res::text;

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m1';
    ASSERT v_stock = 5, 'B7c FAIL: AJUSTE +2 expected stock 5, got ' || v_stock;
    RAISE NOTICE 'B7 PASS: non-sale movement types are unaffected by the alias collapse';
END;
$$;

-- ----------------------------------------------------------------------------
-- P1..P3: the purge predicate. Three fixtures:
--   phantom   — tipo 'venta' + note + a SALIDA_VENTA twin  → must be deleted
--   orphan    — tipo 'venta' + note, no twin               → must survive
--   authored  — the SALIDA_VENTA twin itself               → must survive
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_phantom BIGINT; v_orphan BIGINT; v_twin BIGINT; v_stock INTEGER; v_deleted BIGINT;
BEGIN
    INSERT INTO public.montura_movimientos
        (id, montura_id, fecha, tipo, cantidad, stock_previo, stock_nuevo, referencia_id, nota, optica_id)
    VALUES
        ('zzt_inv_twin',    'zzt_inv_m1', DATE '2026-08-14', 'SALIDA_VENTA', 1, 5, 4, 'zzt_inv_pref', '',                  'zzt_inv_optica_a'),
        ('zzt_inv_phantom', 'zzt_inv_m1', DATE '2026-08-14', 'venta',        1, 5, 4, 'zzt_inv_pref', 'venta_dispensacion', 'zzt_inv_optica_a'),
        ('zzt_inv_orphan',  'zzt_inv_m1', DATE '2026-08-14', 'venta',        1, 4, 3, 'zzt_inv_oref', 'venta_dispensacion', 'zzt_inv_optica_a');

    SELECT stock_actual INTO v_stock FROM public.monturas WHERE id = 'zzt_inv_m1';

    -- Production predicate, extra-scoped to the fixture namespace for safety.
    WITH purged AS (
        DELETE FROM public.montura_movimientos m
        WHERE m.optica_id LIKE 'zzt_inv_%'
          AND m.tipo = 'venta'
          AND m.nota = 'venta_dispensacion'
          AND EXISTS (
            SELECT 1 FROM public.montura_movimientos twin
            WHERE twin.tipo = 'SALIDA_VENTA'
              AND twin.referencia_id = m.referencia_id
              AND twin.montura_id = m.montura_id
              AND twin.optica_id = m.optica_id
          )
        RETURNING 1
    )
    SELECT count(*) INTO v_deleted FROM purged;

    SELECT count(*) INTO v_phantom FROM public.montura_movimientos WHERE id = 'zzt_inv_phantom';
    SELECT count(*) INTO v_orphan  FROM public.montura_movimientos WHERE id = 'zzt_inv_orphan';
    SELECT count(*) INTO v_twin    FROM public.montura_movimientos WHERE id = 'zzt_inv_twin';

    ASSERT v_deleted = 1, 'P1a FAIL: expected exactly 1 purged row, got ' || v_deleted;
    ASSERT v_phantom = 0, 'P1b FAIL: the correlated phantom survived';
    ASSERT v_orphan  = 1, 'P2 FAIL: an uncorrelated venta row was destroyed';
    ASSERT v_twin    = 1, 'P3a FAIL: the SALIDA_VENTA twin was destroyed';

    ASSERT (SELECT stock_actual FROM public.monturas WHERE id = 'zzt_inv_m1') = v_stock,
        'P3b FAIL: the purge changed stock_actual';
    RAISE NOTICE 'P PASS: purge removed % phantom, kept orphan + twin, stock untouched', v_deleted;
END;
$$;

-- ----------------------------------------------------------------------------
-- P4: production must be clean after the migration. Asserted only when run
--     against a database that already holds the real tenant rows.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_left BIGINT;
BEGIN
    SELECT count(*) INTO v_left
    FROM public.montura_movimientos m
    WHERE m.optica_id NOT LIKE 'zzt_inv_%'
      AND m.tipo = 'venta'
      AND m.nota = 'venta_dispensacion'
      AND EXISTS (
        SELECT 1 FROM public.montura_movimientos twin
        WHERE twin.tipo = 'SALIDA_VENTA'
          AND twin.referencia_id = m.referencia_id
          AND twin.montura_id = m.montura_id
          AND twin.optica_id = m.optica_id
      );

    ASSERT v_left = 0, 'P4 FAIL: ' || v_left || ' real phantom row(s) still present';
    RAISE NOTICE 'P4 PASS: no phantom rows remain outside the fixture namespace';
END;
$$;

-- Nothing is kept. Every fixture and every effect above disappears here.
ROLLBACK;
