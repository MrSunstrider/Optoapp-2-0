-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- rpc_analisis_mensual authorizes through auth.uid(), so a fixture admin is impersonated
-- under the authenticated role; fixtures are inserted first because the opticas insert
-- trigger enforces plan limits for auth.uid().
-- The fixture amounts reproduce the July 2026 production sample the original totals came from,
-- so the expected totals stay the same without depending on committed production rows.

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tmc_o1', 'ZZT TMC');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000004', 'zzt_tmc_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000004', 'zzt_tmc_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tmc_pac', 'ZZT TMC', DATE '2026-01-01', 'zzt_tmc_o1');

INSERT INTO public.dispensaciones
    (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega)
SELECT 'zzt_tmc_' || v.tipo || '_' || v.n, 'zzt_tmc_pac', DATE '2026-07-01' + v.n, 'zzt_tmc_o1',
       v.tipo, 'Resina', v.monto, 'Entregado'
FROM (VALUES
    ('Monofocal', 1, 310), ('Monofocal', 2, 400), ('Monofocal', 3, 260), ('Monofocal', 4, 40),
    ('Monofocal', 5, 90), ('Monofocal', 6, 120), ('Monofocal', 7, 90),
    ('Bifocal', 1, 180), ('Bifocal', 2, 60), ('Bifocal', 3, 200), ('Bifocal', 4, 140),
    ('Progresivo', 1, 600)
) AS v(tipo, n, monto);

-- Since S2 the category cost is the real item cost, not the retired margen_por_categoria table.
INSERT INTO public.dispensacion_items
    (id, dispensacion_id, optica_id, costo_real_od, costo_real_oi, costo_real_montura, costo_origen) VALUES
    ('zzt_tmc_i1', 'zzt_tmc_Monofocal_1', 'zzt_tmc_o1', 30, 30, 70, 'matriz'),
    ('zzt_tmc_i2', 'zzt_tmc_Progresivo_1', 'zzt_tmc_o1', 120, 120, NULL, 'manual');

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado)
SELECT 'zzt_tmc_se_' || n, 'zzt_tmc_o1', 'ZZT servicio ' || n, DATE '2026-07-01' + n, 'Efectivo',
       CASE WHEN n = 1 THEN 50 ELSE 42 END, 'Entregado'
FROM generate_series(1, 10) AS n;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000004', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_result jsonb;
    v_cat jsonb;
    v_case RECORD;
BEGIN
    v_result := public.rpc_analisis_mensual('zzt_tmc_o1', DATE '2026-07-01') -> 'margen_por_categoria';

    FOR v_case IN
        SELECT * FROM (VALUES
            ('Lentes Monofocales', 1310.00::NUMERIC, 130.00::NUMERIC),
            ('Lentes Bifocales', 580.00, 0.00),
            ('Lentes Progresivos', 600.00, 240.00),
            ('Servicios Extra', 428.00, 0.00)
        ) AS c(categoria, ventas, costos)
    LOOP
        SELECT value INTO v_cat FROM jsonb_array_elements(v_result) WHERE value ->> 'categoria' = v_case.categoria;
        ASSERT v_cat IS NOT NULL, 'T2 FAIL: ' || v_case.categoria || ' not found';
        ASSERT (v_cat ->> 'ventas')::NUMERIC = v_case.ventas,
            'T2 FAIL: expected ' || v_case.categoria || ' ventas = ' || v_case.ventas || ', got ' || (v_cat ->> 'ventas');
        ASSERT (v_cat ->> 'costos')::NUMERIC = v_case.costos,
            'T2 FAIL: expected ' || v_case.categoria || ' costos = ' || v_case.costos || ', got ' || (v_cat ->> 'costos');
    END LOOP;

    RAISE NOTICE 'T2 PASS: margen_por_categoria returns correct revenue and real-cost totals';
END;
$$;

ROLLBACK;
