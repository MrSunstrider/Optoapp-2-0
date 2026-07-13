-- =============================================================================
-- Seed Data â€” Development Only (NOT for production)
--
-- Populates the database with representative synthetic data after
-- supabase db reset, making local development environments functional.
--
-- ALL inserts use ON CONFLICT DO NOTHING for idempotency.
-- ALL identifiers use synthetic test domains (@test.com).
--
-- Supabase config.toml already references this file via:
--   [db.seed]
--   sql_paths = ["./seed.sql"]
-- =============================================================================

-- Temporarily disable triggers that might interfere with seed inserts
SET session_replication_role = 'replica';

-- #############################################################################
-- 1. Optica (tenant)
-- #############################################################################
INSERT INTO public.opticas (id, nombre, created_at, laboratorio_nombre, laboratorio_contacto, plan, plan_code, plan_source, plan_status, fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal)
VALUES (
    'dev-optica-001',
    'Ã“ptica Demo S.A.S.',
    '2026-01-01 00:00:00+00',
    'Laboratorio Demo',
    'lab@test.com',
    'dev_owner',
    'dev_owner',
    'internal',
    'active',
    'RUC',
    '20123456789',
    'OPTICA DEMO S.A.S.',
    'Av. DemostraciÃ³n 123, Lima'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 2. Patients (3 with varied profiles)
-- #############################################################################
INSERT INTO public.pacientes (id, nombre_completo, edad, telefono, fecha_creacion, ultimas_etiquetas, optica_id, updated_at, dni, email, sexo, direccion)
VALUES
    (
        'dev-paciente-001',
        'Juan PÃ©rez GarcÃ­a',
        35,
        '999000001',
        '2026-06-01',
        'habitual,lentes-progresivos',
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        '12345678',
        'juan.perez@test.com',
        'M',
        'Jr. Prueba 123'
    ),
    (
        'dev-paciente-002',
        'MarÃ­a LÃ³pez Medina',
        42,
        '999000002',
        '2026-06-15',
        'nuevo',
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        '23456789',
        'maria.lopez@test.com',
        'F',
        'Av. Test 456'
    ),
    (
        'dev-paciente-003',
        'Carlos RamÃ­rez Torres',
        28,
        '999000003',
        '2026-07-01',
        'habitual,lentes-monte',
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        '34567890',
        'carlos.ramirez@test.com',
        'M',
        'Calle SintÃ©tica 789'
    )
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 3. Monturas (2 products with prices)
-- #############################################################################
INSERT INTO public.monturas (id, sku, marca, modelo, color, talla, costo, precio, stock_actual, stock_minimo, activo, optica_id, updated_at, tipo_aro, material_montura, categoria, coleccion, temporada, estado_comercial, genero)
VALUES
    (
        'dev-montura-001',
        'DEMO-MT-001',
        'Marca Demo',
        'Modelo ClÃ¡sico',
        'Negro',
        'M',
        45.00,
        150.00,
        5,
        2,
        true,
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        'completo',
        'metal',
        'estandar',
        'Demo 2026',
        'todo-el-ano',
        'activo',
        'unisex'
    ),
    (
        'dev-montura-002',
        'DEMO-MT-002',
        'Marca Demo',
        'Modelo Premium',
        'Tortuga',
        'L',
        120.00,
        350.00,
        3,
        1,
        true,
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        'semialado',
        'acetato',
        'premium',
        'Demo 2026',
        'todo-el-ano',
        'activo',
        'unisex'
    )
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 4. DispensaciÃ³n (linked to paciente-001, montura-001)
-- #############################################################################
INSERT INTO public.dispensaciones (
    id, paciente_id, fecha, optica_id, tipo_montura, material_montura,
    tipo_lente, material_lente, tratamientos, color_lente, notas_diseno,
    origen_montura, tipo_aro, descripcion_montura, monto_total, metodo_pago,
    monto_pagado, estado_entrega, distancia_lente, sub_tipo_bifocal,
    updated_at, altura, ot, filtro_discromatopsia_tipo
)
VALUES (
    'dev-disp-001',
    'dev-paciente-001',
    '2026-07-01',
    'dev-optica-001',
    'completo',
    'metal',
    'progresivo',
    'resina',
    'antireflejo,resistente-raya',
    'blanco',
    'Lentes progresivos con antireflejo',
    'propia',
    'completo',
    'Montura completa metal',
    600.00,
    'efectivo',
     600.00,
     'Pendiente',
    'larga',
    'ninguno',
    '2026-07-01 12:00:00+00',
    '0',
    'DEMO-OT-001',
    'ninguno'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 5. DispensaciÃ³n item (linked to disp-001, montura-001)
-- #############################################################################
INSERT INTO public.dispensacion_items (
    id, dispensacion_id, tipo_lente, material_lente, tratamientos,
    color_lente, distancia_lente, montura_id, origen_montura,
    tipo_aro, material_montura, descripcion_montura, tipo_montura,
    optica_id, filtro_discromatopsia_tipo
)
VALUES (
    'dev-dispitem-001',
    'dev-disp-001',
    'progresivo',
    'resina',
    'antireflejo,resistente-raya',
    'blanco',
    'larga',
    'dev-montura-001',
    'propia',
    'completo',
    'metal',
    'Montura completa metal',
    'completo',
    'dev-optica-001',
    'ninguno'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 6. Service entry (linked to paciente-002)
-- #############################################################################
INSERT INTO public.servicios_extra (
    id, ot, descripcion, monto_total, a_cuenta, estado,
    fecha, paciente_id, metodo_pago, optica_id, updated_at
)
VALUES (
    'dev-serv-001',
    'DEMO-OT-S001',
    'ReparaciÃ³n de montura â€” cambio de varilla',
    50.00,
    0.00,
    'Pendiente',
    '2026-07-05',
    'dev-paciente-002',
    'efectivo',
    'dev-optica-001',
    '2026-07-05 12:00:00+00'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 7. Pago (linked to disp-001)
-- #############################################################################
INSERT INTO public.pagos (
    id, dispensacion_id, fecha, tipo, monto, metodo_pago,
    nota, updated_at, optica_id
)
VALUES (
    'dev-pago-001',
    'dev-disp-001',
    '2026-07-01',
    'contado',
    600.00,
    'efectivo',
    'Pago completo al contado',
    '2026-07-01 12:00:00+00',
    'dev-optica-001'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 8. CategorÃ­as producto reference data (if not already present in migrations)
-- #############################################################################
INSERT INTO public.categorias_producto (id, nombre, familia, orden)
VALUES
    ('lente_progresivo',    'Lentes Progresivos',   'lente',     1),
    ('lente_monofocal',     'Lentes Monofocales',   'lente',     2),
    ('lente_bifocal',       'Lentes Bifocales',     'lente',     3),
    ('montura_premium',     'Monturas Premium',     'montura',   4),
    ('montura_estandar',    'Monturas Estandar',    'montura',   5),
    ('montura_economica',   'Monturas Economicas',  'montura',   6),
    ('servicio_extra',      'Servicios Extra',      'servicio',  7),
    ('garantia_extendida',  'Garantias Extendidas', 'servicio',  8),
    ('otro_lente',          'Otros Lentes',         'lente',     9)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 9. ConfiguraciÃ³n financiera (required by app)
-- #############################################################################
INSERT INTO public.configuracion_financiera (optica_id, margen_neto_objetivo)
VALUES ('dev-optica-001', 35.0)
ON CONFLICT (optica_id) DO NOTHING;

-- Restore normal trigger behavior
SET session_replication_role = 'origin';

-- =============================================================================
-- End of seed data
-- =============================================================================
