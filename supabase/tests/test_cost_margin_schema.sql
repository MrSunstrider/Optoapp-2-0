-- =============================================================================
-- Test: cost-margin schema foundation — servicio_tipos catalog
-- Change: costos-margenes-reales / S1
--
-- SAFETY CONTRACT
--   * Everything runs inside ONE transaction that ends in ROLLBACK. Nothing is
--     ever committed, so it is safe against any database including production.
--   * Every fixture id is prefixed `zzt_cms_` — a namespace no client emits.
--     Fixture users use fixed UUIDs in the a1000000-... range.
--   * Expected failures are caught in plpgsql EXCEPTION blocks (implicit
--     savepoints), so a rejection does not abort the surrounding transaction.
--   * Fixture inserts run as postgres. RLS assertions switch to the
--     `authenticated` role with SET LOCAL ROLE and a JWT subject, then
--     RESET ROLE before the next privileged step.
--
-- Requires migration *_cost_margin_schema_foundation.sql.
--
-- Run (after `supabase db reset`, local stack only):
--   docker exec -i supabase_db_optoapp psql -U postgres -d postgres \
--     -v ON_ERROR_STOP=1 < supabase/tests/test_cost_margin_schema.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ----------------------------------------------------------------------------
-- Fixtures: two opticas; admin/gerente/asesor of O1 and admin of O2.
-- usuario_optica guards (admin bootstrap, optica-per-user cap) are bypassed
-- only while the fixture memberships are inserted.
-- ----------------------------------------------------------------------------
INSERT INTO public.opticas (id, nombre) VALUES
    ('zzt_cms_o1', 'ZZT CMS 1'),
    ('zzt_cms_o2', 'ZZT CMS 2');

INSERT INTO auth.users (id, email) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'zzt_cms_admin_a@example.invalid'),
    ('a1000000-0000-4000-8000-000000000002', 'zzt_cms_asesor_a@example.invalid'),
    ('a1000000-0000-4000-8000-000000000003', 'zzt_cms_gerente_a@example.invalid'),
    ('a1000000-0000-4000-8000-000000000004', 'zzt_cms_admin_b@example.invalid');

ALTER TABLE public.usuario_optica DISABLE TRIGGER USER;
INSERT INTO public.usuario_optica (user_id, optica_id, rol) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'zzt_cms_o1', 'admin'),
    ('a1000000-0000-4000-8000-000000000002', 'zzt_cms_o1', 'asesor'),
    ('a1000000-0000-4000-8000-000000000003', 'zzt_cms_o1', 'gerente'),
    ('a1000000-0000-4000-8000-000000000004', 'zzt_cms_o2', 'admin');
ALTER TABLE public.usuario_optica ENABLE TRIGGER USER;

INSERT INTO public.servicio_tipos (id, optica_id, nombre, costo_propio, costo_tercero, activo) VALUES
    ('zzt_cms_st_o1_biselado', 'zzt_cms_o1', 'Biselado', 5, 12, true),
    ('zzt_cms_st_o1_pulido',   'zzt_cms_o1', 'Pulido',   3,  9, false),
    ('zzt_cms_st_o2_biselado', 'zzt_cms_o2', 'Biselado', 4, 10, true);

-- ----------------------------------------------------------------------------
-- ST1: columns, types and nullability of the catalog.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_bad TEXT;
BEGIN
    SELECT string_agg(e.col, ', ') INTO v_bad
    FROM (VALUES
        ('id', 'text', 'NO'),
        ('optica_id', 'text', 'NO'),
        ('nombre', 'text', 'NO'),
        ('costo_propio', 'numeric', 'NO'),
        ('costo_tercero', 'numeric', 'NO'),
        ('activo', 'boolean', 'NO'),
        ('created_at', 'timestamp with time zone', 'NO'),
        ('updated_at', 'timestamp with time zone', 'NO'),
        ('updated_by', 'text', 'YES')
    ) AS e(col, typ, nullable)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public' AND c.table_name = 'servicio_tipos' AND c.column_name = e.col
    WHERE c.column_name IS NULL OR c.data_type <> e.typ OR c.is_nullable <> e.nullable;

    ASSERT v_bad IS NULL, 'ST1 FAIL: unexpected column shape for: ' || COALESCE(v_bad, 'none');
    RAISE NOTICE 'ST1 PASS: servicio_tipos columns, types and nullability';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST2: defaults — a minimal insert gets cost 0, activo true and timestamps.
-- ----------------------------------------------------------------------------
INSERT INTO public.servicio_tipos (id, optica_id, nombre) VALUES
    ('zzt_cms_st_o1_defaults', 'zzt_cms_o1', 'Defaults');

DO $$
DECLARE v RECORD;
BEGIN
    SELECT * INTO v FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_defaults';
    ASSERT v.costo_propio = 0 AND v.costo_tercero = 0, 'ST2a FAIL: costs must default to 0';
    ASSERT v.activo = true, 'ST2b FAIL: activo must default to true';
    ASSERT v.created_at IS NOT NULL AND v.updated_at IS NOT NULL, 'ST2c FAIL: timestamps must default to now()';
    RAISE NOTICE 'ST2 PASS: defaults';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST3: same name allowed in another optica, case-insensitive uniqueness inside
--      one optica (fixtures already hold 'Biselado' in both opticas).
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_constraint TEXT;
BEGIN
    ASSERT (SELECT count(*) FROM public.servicio_tipos
            WHERE lower(nombre) = 'biselado' AND optica_id IN ('zzt_cms_o1', 'zzt_cms_o2')) = 2,
        'ST3a FAIL: the same name must be allowed in two different opticas';

    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre)
        VALUES ('zzt_cms_st_o1_dup', 'zzt_cms_o1', 'BISELADO');
        RAISE EXCEPTION 'ST3b FAIL: a case-insensitive duplicate name in one optica was accepted';
    EXCEPTION WHEN unique_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
        ASSERT v_constraint = 'uq_servicio_tipos_optica_nombre',
            'ST3b FAIL: expected uq_servicio_tipos_optica_nombre, got ' || COALESCE(v_constraint, 'null');
    END;
    RAISE NOTICE 'ST3 PASS: unique (optica_id, lower(nombre))';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST4: CHECK constraints — negative costs and blank names rejected.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre, costo_propio)
        VALUES ('zzt_cms_st_neg_propio', 'zzt_cms_o1', 'Neg propio', -1);
        RAISE EXCEPTION 'ST4a FAIL: negative costo_propio accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre, costo_tercero)
        VALUES ('zzt_cms_st_neg_tercero', 'zzt_cms_o1', 'Neg tercero', -0.01);
        RAISE EXCEPTION 'ST4b FAIL: negative costo_tercero accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre)
        VALUES ('zzt_cms_st_blank', 'zzt_cms_o1', '   ');
        RAISE EXCEPTION 'ST4c FAIL: blank nombre accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'ST4 PASS: cost and name CHECKs';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST5: privileges and policy shape — anon has nothing, authenticated cannot
--      DELETE, and no DELETE policy exists.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    ASSERT NOT has_table_privilege('anon', 'public.servicio_tipos', 'SELECT'),
        'ST5a FAIL: anon must not read servicio_tipos';
    ASSERT has_table_privilege('authenticated', 'public.servicio_tipos', 'SELECT')
       AND has_table_privilege('authenticated', 'public.servicio_tipos', 'INSERT')
       AND has_table_privilege('authenticated', 'public.servicio_tipos', 'UPDATE'),
        'ST5b FAIL: authenticated needs SELECT, INSERT and UPDATE';
    ASSERT NOT has_table_privilege('authenticated', 'public.servicio_tipos', 'DELETE'),
        'ST5c FAIL: authenticated must not hold DELETE';
    ASSERT has_table_privilege('service_role', 'public.servicio_tipos', 'DELETE'),
        'ST5d FAIL: service_role keeps full access for operator cleanup';
    ASSERT NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'servicio_tipos' AND cmd IN ('DELETE', 'ALL')
    ), 'ST5e FAIL: no DELETE (or ALL) policy may exist';
    ASSERT (SELECT relrowsecurity FROM pg_class WHERE oid = 'public.servicio_tipos'::regclass),
        'ST5f FAIL: RLS must be enabled';
    ASSERT EXISTS (
        SELECT 1 FROM pg_trigger t JOIN pg_proc p ON p.oid = t.tgfoid
        WHERE t.tgrelid = 'public.servicio_tipos'::regclass AND NOT t.tgisinternal
          AND t.tgname = 'trg_servicio_tipos_set_updated_audit' AND p.proname = 'set_updated_audit_fields'
    ), 'ST5g FAIL: audit trigger missing';
    RAISE NOTICE 'ST5 PASS: privileges, policies and audit trigger';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST6: RLS as admin of O1.
-- ----------------------------------------------------------------------------
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a1000000-0000-4000-8000-000000000001', 'role', 'authenticated')::text, true);

DO $$
DECLARE v_rows INTEGER; v_by TEXT; v_at TIMESTAMPTZ;
BEGIN
    INSERT INTO public.servicio_tipos (id, optica_id, nombre, costo_propio, costo_tercero)
    VALUES ('zzt_cms_st_o1_corte', 'zzt_cms_o1', 'Corte', 2, 6);
    ASSERT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_corte'),
        'ST6a FAIL: admin must insert into its own optica';

    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre)
        VALUES ('zzt_cms_st_cross', 'zzt_cms_o2', 'Cross optica');
        RAISE EXCEPTION 'ST6b FAIL: admin of O1 inserted into O2';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    UPDATE public.servicio_tipos SET costo_propio = 7, updated_at = TIMESTAMPTZ '2026-01-01 00:00:00+00'
    WHERE id = 'zzt_cms_st_o1_corte';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    ASSERT v_rows = 1, 'ST6c FAIL: admin must update its own optica rows';
    SELECT updated_at, updated_by INTO v_at, v_by FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_corte';
    ASSERT v_at = TIMESTAMPTZ '2026-01-01 00:00:00+00', 'ST6d FAIL: client updated_at must be preserved';
    ASSERT v_by = 'a1000000-0000-4000-8000-000000000001', 'ST6e FAIL: updated_by must be the acting user';

    UPDATE public.servicio_tipos SET activo = false WHERE id = 'zzt_cms_st_o1_corte';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    ASSERT v_rows = 1, 'ST6f FAIL: deactivation is an UPDATE and must be allowed';

    UPDATE public.servicio_tipos SET nombre = 'Hijacked' WHERE id = 'zzt_cms_st_o2_biselado';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    ASSERT v_rows = 0, 'ST6g FAIL: admin of O1 must not update O2 rows';

    BEGIN
        UPDATE public.servicio_tipos SET optica_id = 'zzt_cms_o2' WHERE id = 'zzt_cms_st_o1_pulido';
        RAISE EXCEPTION 'ST6h FAIL: admin moved a row into another optica';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    BEGIN
        DELETE FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_pulido';
        GET DIAGNOSTICS v_rows = ROW_COUNT;
        ASSERT v_rows = 0, 'ST6i FAIL: authenticated DELETE removed a row';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
    ASSERT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_pulido'),
        'ST6i FAIL: the row must survive a DELETE attempt';
    RAISE NOTICE 'ST6 PASS: admin RLS (insert, cross-optica denied, update, no delete)';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST7: RLS as gerente of O1 (allowed to write) and asesor of O1 (read-only).
-- ----------------------------------------------------------------------------
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a1000000-0000-4000-8000-000000000003', 'role', 'authenticated')::text, true);

DO $$
BEGIN
    INSERT INTO public.servicio_tipos (id, optica_id, nombre)
    VALUES ('zzt_cms_st_o1_gerente', 'zzt_cms_o1', 'Creado por gerente');
    ASSERT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_gerente'),
        'ST7a FAIL: gerente must insert into its own optica';
    RAISE NOTICE 'ST7 PASS: gerente may write';
END;
$$;

SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a1000000-0000-4000-8000-000000000002', 'role', 'authenticated')::text, true);

DO $$
DECLARE v_rows INTEGER;
BEGIN
    ASSERT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE optica_id = 'zzt_cms_o1'),
        'ST8a FAIL: asesor must read its own optica catalog';
    ASSERT NOT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE optica_id <> 'zzt_cms_o1'),
        'ST8b FAIL: asesor must never see another optica catalog';

    BEGIN
        INSERT INTO public.servicio_tipos (id, optica_id, nombre)
        VALUES ('zzt_cms_st_asesor', 'zzt_cms_o1', 'Creado por asesor');
        RAISE EXCEPTION 'ST8c FAIL: asesor inserted a catalog row';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    UPDATE public.servicio_tipos SET nombre = 'Cambiado por asesor' WHERE id = 'zzt_cms_st_o1_biselado';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    ASSERT v_rows = 0, 'ST8d FAIL: asesor must not update catalog rows';
    RAISE NOTICE 'ST8 PASS: asesor is read-only and tenant-scoped';
END;
$$;

-- ----------------------------------------------------------------------------
-- ST9: admin of O2 reuses a name that already exists in O1 and sees only O2.
-- ----------------------------------------------------------------------------
SELECT set_config('request.jwt.claims',
    json_build_object('sub', 'a1000000-0000-4000-8000-000000000004', 'role', 'authenticated')::text, true);

DO $$
BEGIN
    INSERT INTO public.servicio_tipos (id, optica_id, nombre)
    VALUES ('zzt_cms_st_o2_corte', 'zzt_cms_o2', 'Corte');
    ASSERT NOT EXISTS (SELECT 1 FROM public.servicio_tipos WHERE optica_id <> 'zzt_cms_o2'),
        'ST9a FAIL: admin of O2 must never see O1 rows';
    RAISE NOTICE 'ST9 PASS: same name in another optica, tenant isolation';
END;
$$;

RESET ROLE;

-- ST10: the asesor attempts above changed nothing.
DO $$
BEGIN
    ASSERT (SELECT nombre FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_biselado') = 'Biselado',
        'ST10 FAIL: a denied asesor UPDATE altered the row';
    RAISE NOTICE 'ST10 PASS: denied writes left no trace';
END;
$$;

-- ----------------------------------------------------------------------------
-- Fixtures for the column additions. Rows are inserted with the OLD column
-- shape (no new columns) so every insert doubles as an old-client probe.
-- ----------------------------------------------------------------------------
INSERT INTO public.pacientes (id, nombre_completo, fecha_creacion, optica_id) VALUES
    ('zzt_cms_pac', 'ZZT Paciente', DATE '2026-01-01', 'zzt_cms_o1');

INSERT INTO public.dispensaciones (id, paciente_id, fecha, optica_id, monto_total) VALUES
    ('zzt_cms_disp', 'zzt_cms_pac', DATE '2026-01-10', 'zzt_cms_o1', 500);

INSERT INTO public.servicios_extra (id, optica_id, descripcion, fecha, metodo_pago) VALUES
    ('zzt_cms_se_legacy', 'zzt_cms_o1', 'ZZT servicio legacy', DATE '2026-01-10', 'Efectivo');

INSERT INTO public.servicio_extra_items (id, servicio_extra_id, optica_id) VALUES
    ('zzt_cms_sei_legacy', 'zzt_cms_se_legacy', 'zzt_cms_o1');

INSERT INTO public.dispensacion_items (id, dispensacion_id, optica_id) VALUES
    ('zzt_cms_di_legacy', 'zzt_cms_disp', 'zzt_cms_o1');

INSERT INTO public.gastos_operativos (id, optica_id, categoria, monto) VALUES
    ('a1000000-0000-4000-8000-0000000000a1', 'zzt_cms_o1', 'alquiler', 100);

INSERT INTO public.costos_lc (optica_id, tipo_lc, material_lc, modalidad, costo_unitario)
VALUES ('zzt_cms_o1', 'cosmetico', 'zzt_material', 'diario', 2);

INSERT INTO public.costos_productos (optica_id, material, tipo_lente, stock_o_fabricacion, costo_unitario)
VALUES ('zzt_cms_o1', 'zzt_resina', 'zzt_monofocal', 'stock', 10);

INSERT INTO public.costos_biselado (optica_id, material, tipo_aro, stock_o_fabricacion, costo_por_par)
VALUES ('zzt_cms_o1', 'zzt_resina', 'aro_completo', 'stock', 4);

-- ----------------------------------------------------------------------------
-- SV1: servicios_extra executor and cost columns. Legacy rows keep their
--      description and get NULL type/executor and cost 0.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v RECORD; v_bad TEXT;
BEGIN
    SELECT string_agg(e.col, ', ') INTO v_bad
    FROM (VALUES
        ('servicio_tipo_id', 'text', 'YES'),
        ('realizado_por', 'text', 'YES'),
        ('realizado_por_nombre', 'text', 'YES'),
        ('costo_real', 'numeric', 'NO')
    ) AS e(col, typ, nullable)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public' AND c.table_name = 'servicios_extra' AND c.column_name = e.col
    WHERE c.column_name IS NULL OR c.data_type <> e.typ OR c.is_nullable <> e.nullable;
    ASSERT v_bad IS NULL, 'SV1a FAIL: unexpected servicios_extra column shape for: ' || COALESCE(v_bad, 'none');

    SELECT * INTO v FROM public.servicios_extra WHERE id = 'zzt_cms_se_legacy';
    ASSERT v.servicio_tipo_id IS NULL AND v.realizado_por IS NULL AND v.realizado_por_nombre IS NULL,
        'SV1b FAIL: legacy servicio must have NULL type and executor';
    ASSERT v.costo_real = 0, 'SV1c FAIL: legacy servicio costo_real must be 0';
    ASSERT v.descripcion = 'ZZT servicio legacy', 'SV1d FAIL: descripcion must stay untouched';
    RAISE NOTICE 'SV1 PASS: servicios_extra columns and legacy defaults';
END;
$$;

DO $$
BEGIN
    UPDATE public.servicios_extra
    SET servicio_tipo_id = 'zzt_cms_st_o1_biselado', realizado_por = 'tercero',
        realizado_por_nombre = 'Taller externo', costo_real = 12
    WHERE id = 'zzt_cms_se_legacy';
    ASSERT (SELECT realizado_por FROM public.servicios_extra WHERE id = 'zzt_cms_se_legacy') = 'tercero',
        'SV2a FAIL: realizado_por tercero must be accepted';

    BEGIN
        UPDATE public.servicios_extra SET realizado_por = 'otro' WHERE id = 'zzt_cms_se_legacy';
        RAISE EXCEPTION 'SV2b FAIL: realizado_por = otro accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE public.servicios_extra SET costo_real = -1 WHERE id = 'zzt_cms_se_legacy';
        RAISE EXCEPTION 'SV2c FAIL: negative servicio costo_real accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    DELETE FROM public.servicio_tipos WHERE id = 'zzt_cms_st_o1_biselado';
    ASSERT EXISTS (SELECT 1 FROM public.servicios_extra WHERE id = 'zzt_cms_se_legacy'),
        'SV2d FAIL: deleting a type must not delete the servicio';
    ASSERT (SELECT servicio_tipo_id FROM public.servicios_extra WHERE id = 'zzt_cms_se_legacy') IS NULL,
        'SV2e FAIL: deleting a type must null the servicio reference (ON DELETE SET NULL)';
    ASSERT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public' AND tablename = 'servicios_extra' AND indexname = 'idx_servicios_extra_servicio_tipo'
    ), 'SV2f FAIL: servicio_tipo_id index missing';
    RAISE NOTICE 'SV2 PASS: executor CHECKs, cost CHECK, FK set null, index';
END;
$$;

-- ----------------------------------------------------------------------------
-- SI1: servicio_extra_items.costo_real — frame cost snapshot, NOT NULL, >= 0.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'servicio_extra_items' AND column_name = 'costo_real'
          AND data_type = 'numeric' AND is_nullable = 'NO'
    ), 'SI1a FAIL: servicio_extra_items.costo_real must be numeric NOT NULL';
    ASSERT (SELECT costo_real FROM public.servicio_extra_items WHERE id = 'zzt_cms_sei_legacy') = 0,
        'SI1b FAIL: an insert omitting costo_real must get 0';
    BEGIN
        INSERT INTO public.servicio_extra_items (id, servicio_extra_id, optica_id, costo_real)
        VALUES ('zzt_cms_sei_neg', 'zzt_cms_se_legacy', 'zzt_cms_o1', -1);
        RAISE EXCEPTION 'SI1c FAIL: negative item costo_real accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'SI1 PASS: servicio_extra_items.costo_real';
END;
$$;

-- ----------------------------------------------------------------------------
-- DI1: dispensacion_items.costo_origen — nullable, four allowed values.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_origen TEXT;
BEGIN
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'dispensacion_items' AND column_name = 'costo_origen'
          AND data_type = 'text' AND is_nullable = 'YES' AND column_default IS NULL
    ), 'DI1a FAIL: costo_origen must be nullable text without default';
    ASSERT (SELECT costo_origen FROM public.dispensacion_items WHERE id = 'zzt_cms_di_legacy') IS NULL,
        'DI1b FAIL: legacy items must keep costo_origen NULL';

    FOREACH v_origen IN ARRAY ARRAY['matriz', 'manual', 'mixto', 'faltante'] LOOP
        UPDATE public.dispensacion_items SET costo_origen = v_origen WHERE id = 'zzt_cms_di_legacy';
        ASSERT (SELECT costo_origen FROM public.dispensacion_items WHERE id = 'zzt_cms_di_legacy') = v_origen,
            'DI1c FAIL: costo_origen ' || v_origen || ' must be accepted';
    END LOOP;

    BEGIN
        UPDATE public.dispensacion_items SET costo_origen = 'otro' WHERE id = 'zzt_cms_di_legacy';
        RAISE EXCEPTION 'DI1d FAIL: costo_origen = otro accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'DI1 PASS: costo_origen domain';
END;
$$;

-- ----------------------------------------------------------------------------
-- GA1: gastos_operativos.tipo_gasto — nullable, no default, no backfill.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    ASSERT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'gastos_operativos' AND column_name = 'tipo_gasto'
          AND data_type = 'text' AND is_nullable = 'YES' AND column_default IS NULL
    ), 'GA1a FAIL: tipo_gasto must be nullable text without default';
    ASSERT (SELECT tipo_gasto FROM public.gastos_operativos
            WHERE id = 'a1000000-0000-4000-8000-0000000000a1') IS NULL,
        'GA1b FAIL: a gasto written without tipo_gasto must stay NULL';

    UPDATE public.gastos_operativos SET tipo_gasto = 'fijo' WHERE id = 'a1000000-0000-4000-8000-0000000000a1';
    UPDATE public.gastos_operativos SET tipo_gasto = 'variable' WHERE id = 'a1000000-0000-4000-8000-0000000000a1';
    UPDATE public.gastos_operativos SET tipo_gasto = NULL WHERE id = 'a1000000-0000-4000-8000-0000000000a1';
    BEGIN
        UPDATE public.gastos_operativos SET tipo_gasto = 'otro' WHERE id = 'a1000000-0000-4000-8000-0000000000a1';
        RAISE EXCEPTION 'GA1c FAIL: tipo_gasto = otro accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'GA1 PASS: tipo_gasto domain';
END;
$$;

-- ----------------------------------------------------------------------------
-- MA1: matrix audit columns on the three cost matrices (last-write-wins sync).
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_tbl TEXT; v_id UUID; v_at TIMESTAMPTZ;
BEGIN
    FOREACH v_tbl IN ARRAY ARRAY['costos_lc', 'costos_productos', 'costos_biselado'] LOOP
        ASSERT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = v_tbl AND column_name = 'updated_at'
              AND data_type = 'timestamp with time zone' AND is_nullable = 'NO' AND column_default LIKE 'now()%'
        ), 'MA1a FAIL: ' || v_tbl || '.updated_at must be timestamptz NOT NULL DEFAULT now()';
        ASSERT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = v_tbl AND column_name = 'updated_by'
              AND data_type = 'text' AND is_nullable = 'YES'
        ), 'MA1b FAIL: ' || v_tbl || '.updated_by must be nullable text';
        ASSERT EXISTS (
            SELECT 1 FROM pg_trigger t JOIN pg_proc p ON p.oid = t.tgfoid
            WHERE t.tgrelid = ('public.' || v_tbl)::regclass AND NOT t.tgisinternal
              AND t.tgname = 'trg_' || v_tbl || '_set_updated_audit'
              AND p.proname = 'set_updated_audit_fields'
        ), 'MA1c FAIL: ' || v_tbl || ' audit trigger missing';

        EXECUTE format('SELECT id FROM public.%I WHERE optica_id = %L', v_tbl, 'zzt_cms_o1') INTO v_id;
        EXECUTE format('SELECT updated_at FROM public.%I WHERE id = %L', v_tbl, v_id) INTO v_at;
        ASSERT v_at IS NOT NULL, 'MA1d FAIL: ' || v_tbl || ' old-shape insert must get updated_at';

        EXECUTE format('UPDATE public.%I SET updated_at = %L WHERE id = %L',
                       v_tbl, TIMESTAMPTZ '2026-02-01 00:00:00+00', v_id);
        EXECUTE format('SELECT updated_at FROM public.%I WHERE id = %L', v_tbl, v_id) INTO v_at;
        ASSERT v_at = TIMESTAMPTZ '2026-02-01 00:00:00+00',
            'MA1e FAIL: ' || v_tbl || ' must preserve a client-supplied updated_at';

        EXECUTE format('UPDATE public.%I SET updated_at = NULL WHERE id = %L', v_tbl, v_id);
        EXECUTE format('SELECT updated_at FROM public.%I WHERE id = %L', v_tbl, v_id) INTO v_at;
        ASSERT v_at IS NOT NULL, 'MA1f FAIL: ' || v_tbl || ' trigger must fill a NULL updated_at';
    END LOOP;
    RAISE NOTICE 'MA1 PASS: matrix audit columns and triggers';
END;
$$;

ROLLBACK;
