-- recalcular_resumen_diario_rango: guarded multi-day recalculation for one optica.
-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- Fixtures are inserted as the owner first (the opticas insert trigger enforces plan limits for
-- auth.uid()); the RPC is SECURITY INVOKER, so every call impersonates a fixture user under the
-- authenticated role and the guard is exercised with real memberships and roles.

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES
    ('zzt_trg_o1', 'ZZT TRG 1'),
    ('zzt_trg_o2', 'ZZT TRG 2');

INSERT INTO auth.users (id, email) VALUES
    ('a5000000-0000-4000-8000-000000000231', 'zzt_trg_admin@example.invalid'),
    ('a5000000-0000-4000-8000-000000000232', 'zzt_trg_gerente@example.invalid'),
    ('a5000000-0000-4000-8000-000000000233', 'zzt_trg_asesor@example.invalid'),
    ('a5000000-0000-4000-8000-000000000234', 'zzt_trg_outsider@example.invalid');

ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol) VALUES
    ('a5000000-0000-4000-8000-000000000231', 'zzt_trg_o1', 'admin'),
    ('a5000000-0000-4000-8000-000000000232', 'zzt_trg_o1', 'gerente'),
    ('a5000000-0000-4000-8000-000000000233', 'zzt_trg_o1', 'asesor'),
    ('a5000000-0000-4000-8000-000000000234', 'zzt_trg_o2', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_trg_pac', 'ZZT TRG', DATE '2026-01-01', 'zzt_trg_o1');
INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
VALUES ('zzt_trg_d1', 'zzt_trg_pac', 'zzt_trg_o1', DATE '2026-07-10', 300.0, 'Entregado');

CREATE TEMP TABLE zzt_trg_snapshot ON COMMIT DROP AS
SELECT count(*) AS filas, coalesce(md5(string_agg(r::text, '|' ORDER BY r.fecha)), '') AS huella
FROM public.resumen_diario r WHERE r.optica_id = 'zzt_trg_o1';

CREATE FUNCTION pg_temp.zzt_trg_as(p_user UUID) RETURNS void LANGUAGE sql AS $$
    SELECT set_config('request.jwt.claims', json_build_object('sub', p_user, 'role', 'authenticated')::text, true);
$$;

CREATE FUNCTION pg_temp.zzt_trg_expect_error(p_sql TEXT, p_state TEXT, p_label TEXT) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE EXCEPTION 'FAIL %: call succeeded, expected SQLSTATE %', p_label, p_state;
EXCEPTION
    WHEN OTHERS THEN
        IF SQLSTATE <> p_state THEN
            RAISE EXCEPTION 'FAIL %: got SQLSTATE % (%), expected %', p_label, SQLSTATE, SQLERRM, p_state;
        END IF;
        RAISE NOTICE 'PASS %', p_label;
END $$;

CREATE FUNCTION pg_temp.zzt_trg_expect_dias(p_desde DATE, p_hasta DATE, p_expected INTEGER, p_label TEXT) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE v_dias INTEGER;
BEGIN
    v_dias := public.recalcular_resumen_diario_rango('zzt_trg_o1', p_desde, p_hasta);
    IF v_dias IS DISTINCT FROM p_expected THEN
        RAISE EXCEPTION 'FAIL %: returned %, expected %', p_label, v_dias, p_expected;
    END IF;
    RAISE NOTICE 'PASS %', p_label;
END $$;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA pg_temp TO authenticated;

-- G1 metadata: invoker security, integer result, EXECUTE held only by authenticated.
DO $$
DECLARE
    v_secdef BOOLEAN;
    v_ret TEXT;
    v_grantees TEXT[];
BEGIN
    SELECT p.prosecdef, p.prorettype::regtype::text,
           array(SELECT DISTINCT a.grantee::regrole::text
                 FROM aclexplode(p.proacl) a
                 WHERE a.privilege_type = 'EXECUTE' AND a.grantee <> p.proowner
                 ORDER BY 1)
    INTO v_secdef, v_ret, v_grantees
    FROM pg_proc p
    WHERE p.oid = 'public.recalcular_resumen_diario_rango(text, date, date)'::regprocedure;

    IF v_secdef THEN RAISE EXCEPTION 'FAIL G1: function must be SECURITY INVOKER'; END IF;
    IF v_ret <> 'integer' THEN RAISE EXCEPTION 'FAIL G1: returns %, expected integer', v_ret; END IF;
    IF v_grantees IS DISTINCT FROM ARRAY['authenticated'] THEN
        RAISE EXCEPTION 'FAIL G1: EXECUTE grantees %, expected {authenticated}', v_grantees;
    END IF;
    RAISE NOTICE 'PASS G1 metadata';
END $$;

-- G2 anon and service_role cannot execute.
DO $$
BEGIN
    IF has_function_privilege('anon', 'public.recalcular_resumen_diario_rango(text, date, date)', 'EXECUTE') THEN
        RAISE EXCEPTION 'FAIL G2: anon holds EXECUTE';
    END IF;
    IF has_function_privilege('service_role', 'public.recalcular_resumen_diario_rango(text, date, date)', 'EXECUTE') THEN
        RAISE EXCEPTION 'FAIL G2: service_role holds EXECUTE';
    END IF;
    RAISE NOTICE 'PASS G2 anon and service_role have no EXECUTE';
END $$;

SET LOCAL ROLE authenticated;

-- G3 a member of another optica is denied and nothing of the target optica changes.
SELECT pg_temp.zzt_trg_as('a5000000-0000-4000-8000-000000000234');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', DATE '2026-07-10', DATE '2026-07-12')$q$,
    '42501', 'G3 non-member denied');

RESET ROLE;
DO $$
DECLARE v_antes RECORD; v_despues RECORD;
BEGIN
    SELECT * INTO v_antes FROM zzt_trg_snapshot;
    SELECT count(*) AS filas, coalesce(md5(string_agg(r::text, '|' ORDER BY r.fecha)), '') AS huella
    INTO v_despues
    FROM public.resumen_diario r WHERE r.optica_id = 'zzt_trg_o1';
    IF v_antes.filas <> v_despues.filas OR v_antes.huella <> v_despues.huella THEN
        RAISE EXCEPTION 'FAIL G3: denied call changed resumen_diario of the target optica';
    END IF;
    RAISE NOTICE 'PASS G3 target optica unchanged';
END $$;
SET LOCAL ROLE authenticated;

-- G4 range validation, as admin.
SELECT pg_temp.zzt_trg_as('a5000000-0000-4000-8000-000000000231');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', DATE '2026-07-12', DATE '2026-07-10')$q$,
    '22023', 'G4a inverted range rejected');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', NULL, DATE '2026-07-10')$q$,
    '22023', 'G4b null bound rejected');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', CURRENT_DATE - 399, CURRENT_DATE)$q$,
    '22023', 'G4c 400-day range rejected');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', CURRENT_DATE + 2, CURRENT_DATE + 2)$q$,
    '22023', 'G4d range ending at CURRENT_DATE + 2 rejected');
SELECT pg_temp.zzt_trg_expect_dias(CURRENT_DATE + 1, CURRENT_DATE + 1, 1, 'G4e range ending at CURRENT_DATE + 1 accepted');

-- G5 asesor is limited to 31 days; a rejected range recalculates nothing.
SELECT pg_temp.zzt_trg_as('a5000000-0000-4000-8000-000000000233');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', CURRENT_DATE - 59, CURRENT_DATE)$q$,
    '42501', 'G5a asesor 60-day range rejected');
SELECT pg_temp.zzt_trg_expect_error(
    $q$SELECT public.recalcular_resumen_diario_rango('zzt_trg_o1', DATE '2026-05-01', DATE '2026-06-01')$q$,
    '42501', 'G5b asesor 32-day range rejected');
SELECT pg_temp.zzt_trg_expect_dias(CURRENT_DATE - 2, CURRENT_DATE, 3, 'G5c asesor 3-day range accepted');
SELECT pg_temp.zzt_trg_expect_dias(DATE '2026-05-01', DATE '2026-05-31', 31, 'G5d asesor 31-day range accepted');

-- G6 admin and gerente may recalculate more than 31 days.
SELECT pg_temp.zzt_trg_as('a5000000-0000-4000-8000-000000000231');
SELECT pg_temp.zzt_trg_expect_dias(DATE '2026-04-01', DATE '2026-05-02', 32, 'G6a admin 32-day range accepted');
SELECT pg_temp.zzt_trg_as('a5000000-0000-4000-8000-000000000232');
SELECT pg_temp.zzt_trg_expect_dias(DATE '2026-03-01', DATE '2026-04-01', 32, 'G6b gerente 32-day range accepted');

-- G7 returned day count matches the days recalculated, including empty days.
SELECT pg_temp.zzt_trg_expect_dias(DATE '2026-07-10', DATE '2026-07-12', 3, 'G7a three-day range returns 3');
SELECT pg_temp.zzt_trg_expect_dias(DATE '2026-07-20', DATE '2026-07-20', 1, 'G7b single day returns 1');

RESET ROLE;

DO $$
DECLARE v_filas INTEGER; v_ventas INTEGER;
BEGIN
    SELECT count(*) INTO v_filas FROM public.resumen_diario
    WHERE optica_id = 'zzt_trg_o1' AND fecha BETWEEN DATE '2026-07-10' AND DATE '2026-07-12';
    IF v_filas <> 3 THEN RAISE EXCEPTION 'FAIL G7c: % rows for 2026-07-10..12, expected 3', v_filas; END IF;

    SELECT ventas_cantidad INTO v_ventas FROM public.resumen_diario
    WHERE optica_id = 'zzt_trg_o1' AND fecha = DATE '2026-07-11';
    IF v_ventas IS DISTINCT FROM 0 THEN RAISE EXCEPTION 'FAIL G7c: empty day row has ventas_cantidad %', v_ventas; END IF;

    SELECT ventas_cantidad INTO v_ventas FROM public.resumen_diario
    WHERE optica_id = 'zzt_trg_o1' AND fecha = DATE '2026-07-10';
    IF v_ventas IS DISTINCT FROM 1 THEN RAISE EXCEPTION 'FAIL G7c: sale day ventas_cantidad %, expected 1', v_ventas; END IF;

    SELECT count(*) INTO v_filas FROM public.resumen_diario
    WHERE optica_id = 'zzt_trg_o1' AND fecha = DATE '2026-06-01';
    IF v_filas <> 0 THEN RAISE EXCEPTION 'FAIL G5b: rejected range recalculated 2026-06-01'; END IF;

    SELECT count(*) INTO v_filas FROM public.resumen_diario
    WHERE optica_id = 'zzt_trg_o2';
    IF v_filas <> 0 THEN RAISE EXCEPTION 'FAIL G3: % rows written for the outsider optica', v_filas; END IF;

    RAISE NOTICE 'PASS G7c rows created for every day, empty days included';
END $$;

ROLLBACK;
