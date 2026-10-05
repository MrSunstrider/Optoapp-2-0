-- Runs in one transaction that ends in ROLLBACK, so it is safe on any database.
-- recalcular_resumen_diario is SECURITY DEFINER but rejects non-members of the optica, so a
-- fixture admin is impersonated under the authenticated role; fixtures are inserted first
-- because the opticas insert trigger enforces plan limits for auth.uid().
-- Sources are dispensaciones + servicios_extra (the legacy ventas table no longer exists).

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO public.opticas (id, nombre) VALUES ('zzt_tcr_o1', 'ZZT TCR');

INSERT INTO auth.users (id, email)
VALUES ('a5000000-0000-4000-8000-000000000005', 'zzt_tcr_admin@example.invalid');

-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture membership is inserted.
ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES ('a5000000-0000-4000-8000-000000000005', 'zzt_tcr_o1', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id)
VALUES ('zzt_tcr_pac', 'ZZT TCR', DATE '2026-01-01', 'zzt_tcr_o1');

INSERT INTO public.dispensaciones (id, paciente_id, optica_id, fecha, monto_total, estado_entrega)
VALUES ('zzt_tcr_disp', 'zzt_tcr_pac', 'zzt_tcr_o1', DATE '2026-07-15', 500.0, 'Entregado');

INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id, tipo_lente, costo_real_od, costo_real_montura)
VALUES ('zzt_tcr_item', 'zzt_tcr_disp', 'zzt_tcr_o1', 'progresivo', 25.0, 80.0);

-- No items: the servicio contributes revenue but falls back to zero cost.
INSERT INTO public.servicios_extra (id, optica_id, fecha, monto_total, estado, descripcion, metodo_pago)
VALUES ('zzt_tcr_serv', 'zzt_tcr_o1', DATE '2026-07-15', 200.0, 'Entregado', 'ZZT servicio', 'Efectivo');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a5000000-0000-4000-8000-000000000005', 'role', 'authenticated')::text, true);

DO $$
DECLARE
    v_costo_total NUMERIC;
    v_ventas_total NUMERIC;
BEGIN
    PERFORM public.recalcular_resumen_diario('zzt_tcr_o1', DATE '2026-07-15');

    SELECT ventas_costo_total, ventas_monto_total INTO v_costo_total, v_ventas_total
    FROM public.resumen_diario
    WHERE optica_id = 'zzt_tcr_o1' AND fecha = DATE '2026-07-15';

    ASSERT v_costo_total IS NOT NULL, 'T8 FAIL: recalcular_resumen_diario wrote no resumen_diario row';
    ASSERT abs(v_costo_total - 105.0) < 0.01,
        'T8.1 FAIL: ventas_costo_total = ' || v_costo_total || ' (expected 105.0)';
    ASSERT abs(v_ventas_total - 700.0) < 0.01,
        'T8.2 FAIL: ventas_monto_total = ' || v_ventas_total || ' (expected 700.0)';

    RAISE NOTICE 'T8 PASS: costo_real_* from items, zero-cost servicio fallback, mixed aggregate';
END;
$$;

ROLLBACK;
