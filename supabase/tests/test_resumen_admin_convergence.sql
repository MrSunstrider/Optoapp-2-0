-- recalcular_resumen_diario_admin (operator backfill) must write exactly what the member RPC
-- recalcular_resumen_diario writes, including servicio labor cost, servicio frame items, gifts and
-- the Reclamada exclusion. Runs in one transaction that ends in ROLLBACK, so it is safe on any
-- database. The admin RPC runs as service_role (its only grantee); the member RPC runs as a
-- fixture admin under the authenticated role.

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tac_o1', 'ZZT TAC');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000241', 'zzt_tac_admin@example.invalid');

ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000241', 'zzt_tac_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tac_pac', 'ZZT TAC', DATE '2026-01-01', 'zzt_tac_o1');

INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega) VALUES
    ('zzt_tac_d1', 'zzt_tac_pac', 'zzt_tac_o1', DATE '2026-07-03', 200.0, 'Entregado'),
    ('zzt_tac_d_rc', 'zzt_tac_pac', 'zzt_tac_o1', DATE '2026-07-03', 300.0, 'Reclamada');
INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, tipo_lente, costo_real_od) VALUES
    ('zzt_tac_d1_i1', 'zzt_tac_d1', 'zzt_tac_o1', 'monofocal', 50.0),
    ('zzt_tac_d_rc_i1', 'zzt_tac_d_rc', 'zzt_tac_o1', 'monofocal', 99.0);
INSERT INTO public.regalos_dispensacion (id, dispensacion_id, producto_id, cantidad, costo_unitario, descripcion, optica_id)
VALUES ('zzt_tac_d1_r1', 'zzt_tac_d1', 'estuche', 2, 5.0, 'Estuche', 'zzt_tac_o1');
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago, costo_real)
VALUES ('zzt_tac_s1', 'zzt_tac_o1', DATE '2026-07-03', 180.0, 'Entregado', 'Montaje', 'Efectivo', 10.0);
INSERT INTO public.servicio_extra_items (id, servicio_extra_id, descripcion, monto, optica_id, costo_real)
VALUES ('zzt_tac_s1_i1', 'zzt_tac_s1', 'Montura', 90.0, 'zzt_tac_o1', 30.0);
INSERT INTO public.regalos_servicio_extra (id, servicio_extra_id, producto_id, cantidad, costo_unitario, optica_id)
VALUES ('zzt_tac_s1_r1', 'zzt_tac_s1', 'pano', 1, 8.0, 'zzt_tac_o1');

CREATE TEMP TABLE zzt_tac_rows (origen TEXT, fila JSONB) ON COMMIT DROP;

SET LOCAL ROLE service_role;
SELECT public.recalcular_resumen_diario_admin('zzt_tac_o1', DATE '2026-07-03');
RESET ROLE;
INSERT INTO zzt_tac_rows
SELECT 'admin', to_jsonb(r) - 'id' - 'calculado_en'
FROM public.resumen_diario r WHERE r.optica_id = 'zzt_tac_o1' AND r.fecha = DATE '2026-07-03';
DELETE FROM public.resumen_diario WHERE optica_id = 'zzt_tac_o1';

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000241', 'role', 'authenticated')::text, true);
SELECT public.recalcular_resumen_diario('zzt_tac_o1', DATE '2026-07-03');
RESET ROLE;
INSERT INTO zzt_tac_rows
SELECT 'member', to_jsonb(r) - 'id' - 'calculado_en'
FROM public.resumen_diario r WHERE r.optica_id = 'zzt_tac_o1' AND r.fecha = DATE '2026-07-03';

DO $$
DECLARE v_admin JSONB; v_member JSONB;
BEGIN
    SELECT fila INTO v_admin FROM zzt_tac_rows WHERE origen = 'admin';
    SELECT fila INTO v_member FROM zzt_tac_rows WHERE origen = 'member';

    IF (v_member->>'ventas_costo_total')::numeric <> 108.0 THEN
        RAISE EXCEPTION 'FAIL C1: member ventas_costo_total %, expected 108 (50 + 2 x 5 + 10 + 30 + 8)',
            v_member->>'ventas_costo_total';
    END IF;
    RAISE NOTICE 'PASS C1 member RPC real cost 108';

    IF v_admin IS DISTINCT FROM v_member THEN
        RAISE EXCEPTION 'FAIL C2: admin row % differs from member row %', v_admin, v_member;
    END IF;
    RAISE NOTICE 'PASS C2 admin backfill writes the same row as the member RPC';
END $$;

-- C3 the shared core is never callable by API roles; only the two wrappers reach it.
DO $$
DECLARE v_role TEXT;
BEGIN
    FOREACH v_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
        IF has_function_privilege(v_role, 'app_private.recalcular_resumen_diario_core(text, date)', 'EXECUTE') THEN
            RAISE EXCEPTION 'FAIL C3: % holds EXECUTE on the core', v_role;
        END IF;
    END LOOP;
    IF has_function_privilege('authenticated', 'public.recalcular_resumen_diario_admin(text, date)', 'EXECUTE') THEN
        RAISE EXCEPTION 'FAIL C3: authenticated holds EXECUTE on the admin RPC';
    END IF;
    RAISE NOTICE 'PASS C3 core private, admin RPC still service_role only';
END $$;

-- C4 the member RPC keeps its membership guard.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000999', 'role', 'authenticated')::text, true);
DO $$
BEGIN
    PERFORM public.recalcular_resumen_diario('zzt_tac_o1', DATE '2026-07-03');
    RAISE EXCEPTION 'FAIL C4: non-member recalculated';
EXCEPTION
    WHEN raise_exception THEN
        IF SQLERRM <> 'Access denied' THEN RAISE; END IF;
        RAISE NOTICE 'PASS C4 non-member denied';
END $$;
RESET ROLE;

ROLLBACK;
