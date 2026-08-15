-- =============================================================================
-- Test: financial SQL aggregates converge on public.pago_effect
-- Change: fix-sync-financial-ledger / WU-1B + WU-1C
-- Covers:
--   WU-1B: recalcular_resumen_diario, rpc_cierre_caja_resumen
--   WU-1C: rpc_deudores, rpc_analisis_mensual (proyeccion_caja + debt inputs)
-- RED before the converge migrations; GREEN after them.
-- Run via: supabase db reset && psql -h localhost -p 54322 -U postgres -d postgres \
--            -f supabase/tests/test_ledger_aggregate_convergence.sql
-- =============================================================================

-- A. Effect matrix nets fixture F1: Abono +100, Reverso 40, Reembolso 10,
--    Anulación 999 => net 50 (Efectivo 60, Tarjeta -10).
DO $$
DECLARE
    v_net NUMERIC;
    v_efectivo NUMERIC;
    v_tarjeta NUMERIC;
BEGIN
    SELECT COALESCE(SUM(public.pago_effect(f.tipo, f.monto)), 0),
           COALESCE(SUM(CASE WHEN f.metodo = 'Efectivo'
                             THEN public.pago_effect(f.tipo, f.monto) ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN f.metodo = 'Tarjeta'
                             THEN public.pago_effect(f.tipo, f.monto) ELSE 0 END), 0)
    INTO v_net, v_efectivo, v_tarjeta
    FROM (VALUES
        ('Abono',     100.0::numeric, 'Efectivo'),
        ('Reverso',    40.0::numeric, 'Efectivo'),
        ('Reembolso',  10.0::numeric, 'Tarjeta'),
        ('Anulación', 999.0::numeric, 'Efectivo')
    ) AS f(tipo, monto, metodo);

    ASSERT abs(v_net - 50.0) < 0.005, 'A1 FAIL: net = ' || v_net || ' (expected 50)';
    ASSERT abs(v_efectivo - 60.0) < 0.005, 'A2 FAIL: Efectivo = ' || v_efectivo;
    ASSERT abs(v_tarjeta + 10.0) < 0.005, 'A3 FAIL: Tarjeta = ' || v_tarjeta;
    ASSERT public.pago_effect('Anulación', 999) = 0, 'A4 FAIL: Anulación must be 0';
    RAISE NOTICE 'A PASS: net=% efectivo=% tarjeta=%', v_net, v_efectivo, v_tarjeta;
END;
$$;

-- B. recalcular_resumen_diario aggregates through pago_effect and keeps
--    cancelled/claimed sales out of ventas and debt.
--
--    B2 note: the raw-cash ban is a regex, NOT `LIKE '%SUM(monto)%'`. A literal
--    substring ban is imprecise in both directions: it misses qualified and
--    spaced variants (`SUM(pg.monto)`, `SUM( monto )`), and any naive widening
--    of it flags the legitimate non-cash aggregates that also sum a column
--    whose name merely starts with `monto` — `SUM(monto_total)` (sales) here,
--    and `gastos_operativos.monto` (expenses) in block F. The pattern below
--    requires `monto` to end at the closing paren, so `monto_total` can never
--    match, and it is self-tested before it is trusted.
DO $$
DECLARE
    v_src TEXT;
    c_raw_cash_sum CONSTANT TEXT := 'SUM\s*\(\s*(\w+\.)?monto\s*\)';
BEGIN
    ASSERT 'COALESCE(SUM(monto_total), 0)' !~ c_raw_cash_sum
       AND 'SUM(v.monto_total - x)' !~ c_raw_cash_sum,
        'B2a FAIL: raw-cash ban must never match the SUM(monto_total) sales aggregate';
    ASSERT 'SUM(monto)' ~ c_raw_cash_sum
       AND 'SUM(pd.monto)' ~ c_raw_cash_sum
       AND 'SUM( monto )' ~ c_raw_cash_sum,
        'B2b FAIL: raw-cash ban must match raw, qualified and spaced monto sums';

    SELECT p.prosrc INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'recalcular_resumen_diario';

    ASSERT v_src IS NOT NULL, 'B0 FAIL: recalcular_resumen_diario must exist';
    ASSERT v_src LIKE '%public.pago_effect(%',
        'B1 FAIL: cobros/saldo must aggregate via public.pago_effect';
    ASSERT v_src !~ c_raw_cash_sum,
        'B2 FAIL: raw SUM(monto) must not remain the cash definition';
    ASSERT v_src LIKE '%SUM(monto_total)%',
        'B2c FAIL: the SUM(monto_total) sales aggregate must survive the ban';
    ASSERT v_src LIKE '%estado_entrega IS DISTINCT FROM ''Anulado''%'
       AND v_src LIKE '%estado_entrega IS DISTINCT FROM ''Reclamada''%',
        'B3 FAIL: dispensaciones must exclude Anulado and Reclamada';
    ASSERT v_src LIKE '%estado IS DISTINCT FROM ''Anulado''%',
        'B4 FAIL: servicios_extra must exclude Anulado';
    ASSERT v_src LIKE '%app_private.is_optica_member%',
        'B5 FAIL: tenant membership guard must be preserved';
    ASSERT v_src LIKE '%ON CONFLICT (optica_id, fecha) DO UPDATE%',
        'B6 FAIL: idempotent upsert contract must be preserved';
    RAISE NOTICE 'B PASS: recalcular_resumen_diario converged';
END;
$$;

-- C. rpc_cierre_caja_resumen aggregates through pago_effect, same JSON contract.
DO $$
DECLARE
    v_src TEXT;
    c_raw_cash_sum CONSTANT TEXT := 'SUM\s*\(\s*(\w+\.)?monto\s*\)';
BEGIN
    SELECT p.prosrc INTO v_src
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_cierre_caja_resumen';

    ASSERT v_src IS NOT NULL, 'C0 FAIL: rpc_cierre_caja_resumen must exist';
    -- Cierre reads only pagos, so no non-cash monto aggregate is legitimate here.
    ASSERT v_src NOT LIKE '%THEN monto ELSE 0%' AND v_src !~ c_raw_cash_sum,
        'C1 FAIL: per-method and total sums must not use raw monto';
    ASSERT v_src LIKE '%public.pago_effect(tipo, monto)%',
        'C2 FAIL: cash sums must use public.pago_effect(tipo, monto)';
    ASSERT v_src LIKE '%''efectivo''%' AND v_src LIKE '%''movil_trans''%'
       AND v_src LIKE '%''tarjeta''%' AND v_src LIKE '%''total''%',
        'C3 FAIL: jsonb output keys must be preserved';
    ASSERT v_src LIKE '%has_optica_role%', 'C4 FAIL: BI role guard must be preserved';
    RAISE NOTICE 'C PASS: rpc_cierre_caja_resumen converged';
END;
$$;

-- D. Security context and grants unchanged by the convergence.
DO $$
DECLARE
    v_recalc_definer BOOLEAN;
    v_cierre_definer BOOLEAN;
    v_recalc_cfg TEXT[];
    v_cierre_cfg TEXT[];
BEGIN
    SELECT p.prosecdef, p.proconfig INTO v_recalc_definer, v_recalc_cfg
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'recalcular_resumen_diario';
    SELECT p.prosecdef, p.proconfig INTO v_cierre_definer, v_cierre_cfg
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_cierre_caja_resumen';

    ASSERT v_recalc_definer, 'D1 FAIL: recalcular must stay SECURITY DEFINER';
    ASSERT NOT v_cierre_definer, 'D2 FAIL: cierre must stay SECURITY INVOKER';
    -- Postgres serializes an empty list GUC as `search_path=` or `search_path=""`
    ASSERT array_to_string(v_recalc_cfg, ',') IN ('search_path=', 'search_path=""'),
        'D3 FAIL: recalcular must keep an empty search_path';
    ASSERT 'search_path=public' = ANY(v_cierre_cfg),
        'D4 FAIL: cierre must keep search_path=public';
    ASSERT has_function_privilege('authenticated',
        'public.recalcular_resumen_diario(text,date)', 'EXECUTE'),
        'D5 FAIL: authenticated must keep EXECUTE on recalcular';
    ASSERT has_function_privilege('authenticated',
        'public.rpc_cierre_caja_resumen(text,date,date)', 'EXECUTE'),
        'D6 FAIL: authenticated must keep EXECUTE on cierre';
    ASSERT NOT has_function_privilege('anon',
        'public.rpc_cierre_caja_resumen(text,date,date)', 'EXECUTE'),
        'D7 FAIL: anon must not execute cierre';
    ASSERT has_function_privilege('authenticated',
        'public.pago_effect(text,numeric)', 'EXECUTE'),
        'D8 FAIL: SECURITY INVOKER callers need EXECUTE on pago_effect';
    RAISE NOTICE 'D PASS: security context and grants preserved';
END;
$$;

-- E. rpc_deudores: pago_effect paid totals + Anulado/Reclamada exclusion (WU-1C).
DO $$
DECLARE
    v_src TEXT;
    c_raw_cash_sum CONSTANT TEXT := 'SUM\s*\(\s*(\w+\.)?monto\s*\)';
BEGIN
    SELECT p.prosrc INTO v_src FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_deudores';
    ASSERT v_src IS NOT NULL, 'E0 FAIL: rpc_deudores must exist';
    ASSERT v_src LIKE '%public.pago_effect(%' AND v_src LIKE '%AS efecto%'
       AND v_src LIKE '%SUM(pd.efecto)%', 'E1 FAIL: paid totals must use pago_effect/efecto';
    -- Deudores aggregates no non-cash monto column, so the ban applies in full.
    ASSERT v_src !~ c_raw_cash_sum, 'E2 FAIL: raw SUM(pd.monto) must not remain';
    ASSERT v_src LIKE '%estado_entrega IS DISTINCT FROM ''Anulado''%'
       AND v_src LIKE '%estado_entrega IS DISTINCT FROM ''Reclamada''%'
       AND v_src LIKE '%estado IS DISTINCT FROM ''Anulado''%',
       'E3 FAIL: debt ventas must exclude Anulado/Reclamada';
    ASSERT v_src LIKE '%app_private.is_optica_member%' AND v_src LIKE '%has_optica_role%',
       'E4 FAIL: membership/BI guards must be preserved';
    RAISE NOTICE 'E PASS: rpc_deudores converged';
END;
$$;

-- F. rpc_analisis_mensual proyeccion_caja: pago_effect + exclusions + JSON keys.
--    Unlike B/C/E this body legitimately sums a bare `monto`: gastos_operativos
--    tracks operating EXPENSES, which are not pagos and carry no ledger sign.
--    So the ban is scoped by count instead of being absolute — exactly the
--    non-cash aggregate a blanket raw-SUM ban would false-positive on.
DO $$
DECLARE
    v_src TEXT;
    c_raw_cash_sum CONSTANT TEXT := 'SUM\s*\(\s*(\w+\.)?monto\s*\)';
    v_raw_sums INTEGER;
    v_gastos_sums INTEGER;
BEGIN
    SELECT p.prosrc INTO v_src FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_analisis_mensual';
    ASSERT v_src IS NOT NULL, 'F0 FAIL: rpc_analisis_mensual must exist';
    ASSERT v_src LIKE '%public.pago_effect(%' AND v_src LIKE '%AS efecto%'
       AND v_src LIKE '%SUM(efecto) AS total_pagado%',
       'F1 FAIL: proyeccion paid totals must use pago_effect/efecto';
    ASSERT v_src NOT LIKE '%SUM(monto) AS total_pagado%',
       'F2 FAIL: raw SUM(monto) AS total_pagado must not remain';

    SELECT count(*) INTO v_raw_sums
    FROM regexp_matches(v_src, c_raw_cash_sum, 'g');
    SELECT count(*) INTO v_gastos_sums
    FROM regexp_matches(v_src, 'gastos_operativos', 'g');
    ASSERT v_raw_sums = 2 AND v_gastos_sums = 2,
       'F2b FAIL: the only bare SUM(monto) uses must be the two gastos_operativos '
       || 'expense sums (raw=' || v_raw_sums || ', gastos=' || v_gastos_sums || ')';
    ASSERT v_src LIKE '%estado_entrega IS DISTINCT FROM ''Anulado''%'
       AND v_src LIKE '%estado_entrega IS DISTINCT FROM ''Reclamada''%'
       AND v_src LIKE '%estado IS DISTINCT FROM ''Anulado''%',
       'F3 FAIL: proyeccion ventas must exclude Anulado/Reclamada';
    ASSERT v_src LIKE '%''meses_historicos''%' AND v_src LIKE '%''proyeccion_caja''%'
       AND v_src LIKE '%''deudores''%' AND v_src LIKE '%''margen_por_categoria''%',
       'F4 FAIL: 16-field JSON contract keys must be preserved';
    ASSERT v_src LIKE '%app_private.is_optica_member%' AND v_src LIKE '%has_optica_role%',
       'F5 FAIL: membership/BI guards must be preserved';
    RAISE NOTICE 'F PASS: rpc_analisis_mensual converged';
END;
$$;

-- G. INVOKER + search_path + grants for the two remaining RPCs.
DO $$
DECLARE
    v_deud_definer BOOLEAN; v_anal_definer BOOLEAN;
    v_deud_cfg TEXT[]; v_anal_cfg TEXT[];
BEGIN
    SELECT p.prosecdef, p.proconfig INTO v_deud_definer, v_deud_cfg
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_deudores';
    SELECT p.prosecdef, p.proconfig INTO v_anal_definer, v_anal_cfg
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'rpc_analisis_mensual';
    ASSERT NOT v_deud_definer AND NOT v_anal_definer, 'G1 FAIL: both must stay SECURITY INVOKER';
    ASSERT 'search_path=public' = ANY(v_deud_cfg) AND 'search_path=public' = ANY(v_anal_cfg),
       'G2 FAIL: both must keep search_path=public';
    ASSERT has_function_privilege('authenticated', 'public.rpc_deudores(text)', 'EXECUTE')
       AND has_function_privilege('authenticated', 'public.rpc_analisis_mensual(text,date)', 'EXECUTE'),
       'G3 FAIL: authenticated must keep EXECUTE';
    ASSERT NOT has_function_privilege('anon', 'public.rpc_deudores(text)', 'EXECUTE')
       AND NOT has_function_privilege('anon', 'public.rpc_analisis_mensual(text,date)', 'EXECUTE'),
       'G4 FAIL: anon must not execute either RPC';
    RAISE NOTICE 'G PASS: deudores/analisis security and grants preserved';
END;
$$;

-- H. Reversa link integrity is enforced by the schema, not by client trust:
--    the FK is COMPOSITE on (reversa_pago_id, optica_id) -> pagos(id, optica_id),
--    so a Reverso can only ever point at an original of the SAME optica. The
--    partial UNIQUE stays GLOBAL on (reversa_pago_id) — ids are global, and a
--    per-optica unique would let a cross-tenant id collision insert a second
--    Reverso. Requires the referenced UNIQUE (id, optica_id) to exist.
DO $$
DECLARE
    v_fk_def TEXT;
    v_target_unique INTEGER;
    v_link_validated BOOLEAN;
    v_uidx_def TEXT;
    v_monto_def TEXT;
BEGIN
    SELECT count(*) INTO v_target_unique
    FROM pg_constraint
    WHERE conrelid = 'public.pagos'::regclass
      AND contype = 'u'
      AND pg_get_constraintdef(oid) = 'UNIQUE (id, optica_id)';
    ASSERT v_target_unique = 1,
        'H1 FAIL: pagos needs UNIQUE (id, optica_id) as the composite FK target';

    SELECT pg_get_constraintdef(oid) INTO v_fk_def
    FROM pg_constraint
    WHERE conrelid = 'public.pagos'::regclass
      AND conname = 'pagos_reversa_pago_id_fkey';
    ASSERT v_fk_def IS NOT NULL, 'H2 FAIL: pagos_reversa_pago_id_fkey must exist';
    ASSERT v_fk_def LIKE 'FOREIGN KEY (reversa_pago_id, optica_id) REFERENCES pagos(id, optica_id)%',
        'H3 FAIL: reversa FK must be composite on optica_id, got: ' || v_fk_def;
    ASSERT v_fk_def LIKE '%ON DELETE RESTRICT%',
        'H4 FAIL: reversa FK must keep ON DELETE RESTRICT, got: ' || v_fk_def;

    SELECT convalidated INTO v_link_validated
    FROM pg_constraint
    WHERE conrelid = 'public.pagos'::regclass AND conname = 'chk_pagos_reversa_link';
    ASSERT v_link_validated, 'H5 FAIL: chk_pagos_reversa_link must exist and be VALIDATEd';

    SELECT indexdef INTO v_uidx_def
    FROM pg_indexes
    WHERE schemaname = 'public' AND indexname = 'pagos_reversa_pago_id_uidx';
    ASSERT v_uidx_def IS NOT NULL, 'H6 FAIL: pagos_reversa_pago_id_uidx must exist';
    ASSERT v_uidx_def LIKE '%UNIQUE INDEX%(reversa_pago_id)%'
       AND v_uidx_def NOT LIKE '%(reversa_pago_id, optica_id)%',
        'H7 FAIL: Reverso idempotency must stay globally unique, got: ' || v_uidx_def;
    ASSERT v_uidx_def LIKE '%WHERE%Reverso%',
        'H8 FAIL: the unique index must stay partial on tipo = Reverso';

    SELECT pg_get_constraintdef(oid) INTO v_monto_def
    FROM pg_constraint
    WHERE conrelid = 'public.pagos'::regclass AND conname = 'pagos_monto_chk';
    ASSERT v_monto_def LIKE '%monto >= %' AND v_monto_def NOT LIKE '%NOT VALID%',
        'H9 FAIL: pagos_monto_chk must stay a validated monto >= 0 guard';
    RAISE NOTICE 'H PASS: composite reversa integrity enforced at schema level';
END;
$$;

-- I. Trigger contract: an UPDATE that MOVES a pago between parents must debit
--    the OLD parent and credit the NEW one. Resolving the parent with
--    COALESCE(NEW.x, OLD.x) alone silently strands the old balance, so the
--    origin-change branch must be present. Behavior is proven in
--    supabase/tests/test_ledger_write_path_rollback.sql.
DO $$
DECLARE v_src TEXT;
BEGIN
    SELECT p.prosrc INTO v_src FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'trg_pagos_update_monto_pagado';
    ASSERT v_src IS NOT NULL, 'I0 FAIL: trg_pagos_update_monto_pagado must exist';
    ASSERT v_src LIKE '%public.pago_effect(NEW.tipo, NEW.monto)%'
       AND v_src LIKE '%public.pago_effect(OLD.tipo, OLD.monto)%',
       'I1 FAIL: both sides of the delta must come from public.pago_effect';
    ASSERT v_src ~ 'NEW\.dispensacion_id\s+IS DISTINCT FROM\s+OLD\.dispensacion_id'
       AND v_src ~ 'NEW\.servicio_extra_id\s+IS DISTINCT FROM\s+OLD\.servicio_extra_id',
       'I2 FAIL: the trigger must detect an origin change on both parent columns';
    ASSERT v_src ~ 'WHERE\s+id\s*=\s*OLD\.dispensacion_id'
       AND v_src ~ 'WHERE\s+id\s*=\s*NEW\.dispensacion_id'
       AND v_src ~ 'WHERE\s+id\s*=\s*OLD\.servicio_extra_id'
       AND v_src ~ 'WHERE\s+id\s*=\s*NEW\.servicio_extra_id',
       'I3 FAIL: origin moves must update the OLD and the NEW parent explicitly';
    RAISE NOTICE 'I PASS: trigger handles origin moves, not just net deltas';
END;
$$;
