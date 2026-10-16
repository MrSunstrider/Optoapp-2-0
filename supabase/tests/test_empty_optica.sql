-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- rpc_analisis_mensual authorizes through auth.uid(), so a fixture admin is impersonated
-- under the authenticated role; fixtures are inserted first because the opticas insert
-- trigger enforces plan limits for auth.uid().
-- Sales exist in July 2026 so an empty January 2027 proves month scoping, not just an empty table.

\set ON_ERROR_STOP on

BEGIN;

-- Local seed.sql adds two alias categories that production never had; the
-- contract is one row per production category (9).
DELETE FROM public.categorias_producto WHERE id IN ('garantia_extendida', 'otro_lente');

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_teo_o1', 'ZZT TEO');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000002', 'zzt_teo_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000002', 'zzt_teo_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_teo_pac', 'ZZT TEO', DATE '2026-01-01', 'zzt_teo_o1');

INSERT INTO public.dispensaciones
    (id, paciente_id, fecha, optica_id, tipo_lente, material_lente, monto_total, estado_entrega)
VALUES ('zzt_teo_d1', 'zzt_teo_pac', DATE '2026-07-05', 'zzt_teo_o1', 'Monofocal', 'Resina', 300, 'Entregado');

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago, monto_total, estado)
VALUES ('zzt_teo_se1', 'zzt_teo_o1', 'ZZT servicio', DATE '2026-07-08', 'Efectivo', 50, 'Entregado');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000002', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_mes DATE := DATE '2027-01-01';
    v_idx INTEGER;
BEGIN
    ASSERT (SELECT (e ->> 'ventas')::NUMERIC
            FROM jsonb_array_elements(
                public.rpc_analisis_mensual('zzt_teo_o1', DATE '2026-07-01') -> 'margen_por_categoria') e
            WHERE e ->> 'categoria' = 'Lentes Monofocales') = 300,
        'T6 FAIL: the populated month must report its sales, otherwise the empty month proves nothing';

    v_result := public.rpc_analisis_mensual('zzt_teo_o1', v_mes) -> 'margen_por_categoria';

    ASSERT jsonb_array_length(v_result) = 9,
        'T6 FAIL: expected 9 categories for empty month, got ' || jsonb_array_length(v_result);

    FOR v_idx IN 0..jsonb_array_length(v_result) - 1
    LOOP
        v_item := v_result -> v_idx;
        ASSERT (v_item ->> 'ventas')::NUMERIC = 0,
            'T6 FAIL: category ' || (v_item ->> 'categoria') || ' has ventas=' || (v_item ->> 'ventas') ||
            ' but month ' || v_mes || ' should have no sales';
    END LOOP;

    RAISE NOTICE 'T6 PASS: empty month returns 9 categories with ventas = 0';
END;
$$;

ROLLBACK;
