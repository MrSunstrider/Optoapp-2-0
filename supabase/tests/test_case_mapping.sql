-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- rpc_analisis_mensual authorizes through auth.uid(), so a fixture admin is impersonated
-- under the authenticated role; fixtures are inserted first because the opticas insert
-- trigger enforces plan limits for auth.uid().
-- Each fixture order targets one branch of the inline (tipo_lente, material_lente) mapping.

\set ON_ERROR_STOP on

BEGIN;

-- Local seed.sql adds two alias categories that production never had; the
-- contract is one row per production category (9).
DELETE FROM public.categorias_producto WHERE id IN ('garantia_extendida', 'otro_lente');

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tcm_o1', 'ZZT TCM');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000003', 'zzt_tcm_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000003', 'zzt_tcm_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tcm_pac', 'ZZT TCM', DATE '2026-01-01', 'zzt_tcm_o1');

INSERT INTO public.dispensaciones
    (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega) VALUES
    ('zzt_tcm_d1', 'zzt_tcm_pac', DATE '2026-07-05', 'zzt_tcm_o1', 'Monofocal',  'Resina',  310, 'Entregado'),
    ('zzt_tcm_d2', 'zzt_tcm_pac', DATE '2026-07-06', 'zzt_tcm_o1', 'Progresivo', 'Resina',  600, 'Entregado'),
    ('zzt_tcm_d3', 'zzt_tcm_pac', DATE '2026-07-07', 'zzt_tcm_o1', 'Bifocal',    'Resina',  180, 'Entregado'),
    ('zzt_tcm_d4', 'zzt_tcm_pac', DATE '2026-07-08', 'zzt_tcm_o1', 'Monofocal',  'Cristal',  70, 'Entregado');

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado)
VALUES ('zzt_tcm_se1', 'zzt_tcm_o1', 'ZZT servicio', DATE '2026-07-09', 'Efectivo', 45, 'Entregado');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000003', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_result jsonb;
    v_cat jsonb;
    v_case RECORD;
BEGIN
    v_result := public.rpc_analisis_mensual('zzt_tcm_o1', DATE '2026-07-01') -> 'margen_por_categoria';

    ASSERT jsonb_array_length(v_result) = 9,
        'T1 FAIL: expected 9 categories in margen_por_categoria, got ' || jsonb_array_length(v_result);

    FOR v_case IN
        SELECT * FROM (VALUES
            ('Lentes Monofocales', 310::NUMERIC),
            ('Lentes Progresivos', 600),
            ('Lentes Bifocales', 180),
            ('Servicios Extra', 45),
            ('Otros Lentes', 70)
        ) AS c(categoria, ventas)
    LOOP
        SELECT value INTO v_cat FROM jsonb_array_elements(v_result) WHERE value ->> 'categoria' = v_case.categoria;
        ASSERT v_cat IS NOT NULL, 'T1 FAIL: ' || v_case.categoria || ' not found in margen_por_categoria';
        ASSERT (v_cat ->> 'ventas')::NUMERIC > 0,
            'T1 FAIL: expected ' || v_case.categoria || ' ventas > 0, got ' || (v_cat ->> 'ventas');
        ASSERT (v_cat ->> 'ventas')::NUMERIC = v_case.ventas,
            'T1 FAIL: ' || v_case.categoria || ' ventas = ' || (v_cat ->> 'ventas') || ' (expected ' || v_case.ventas || ')';
    END LOOP;

    RAISE NOTICE 'T1 PASS: inline CASE mapping routes each lens type to its category';
END;
$$;

ROLLBACK;
