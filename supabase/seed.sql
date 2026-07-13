-- =============================================================================
-- Seed Data — Development Only (NOT for production)
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
    'Óptica Demo S.A.S.',
    '2026-01-01 00:00:00+00',
    'Laboratorio Demo',
    'lab@test.com',
    'internal_owner',
    'internal_owner',
    'system',
    'active',
    'RUC',
    '20123456789',
    'OPTICA DEMO S.A.S.',
    'Av. Demostración 123, Lima'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 2. Patients (3 with varied profiles)
-- #############################################################################
INSERT INTO public.pacientes (id, nombre_completo, edad, telefono, fecha_creacion, ultimas_etiquetas, optica_id, updated_at, dni, email, sexo, direccion)
VALUES
    (
        'dev-paciente-001',
        'Juan Pérez García',
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
        'María López Medina',
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
        'Carlos Ramírez Torres',
        28,
        '999000003',
        '2026-07-01',
        'habitual,lentes-monte',
        'dev-optica-001',
        '2026-07-01 12:00:00+00',
        '34567890',
        'carlos.ramirez@test.com',
        'M',
        'Calle Sintética 789'
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
        'Modelo Clásico',
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
-- 4. Dispensación (linked to paciente-001, montura-001)
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
    'pendiente',
    'larga',
    'ninguno',
    '2026-07-01 12:00:00+00',
    '0',
    'DEMO-OT-001',
    'ninguno'
)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 5. Dispensación item (linked to disp-001, montura-001)
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
    'Reparación de montura — cambio de varilla',
    50.00,
    0.00,
    'pendiente',
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
-- 8. Categorías producto reference data (if not already present in migrations)
-- #############################################################################
INSERT INTO public.categorias_producto (id, nombre, familia, orden)
VALUES
    ('lente_progresivo',    'Lentes Progresivos',   'lentes',  1),
    ('lente_monofocal',     'Lentes Monofocales',   'lentes',  2),
    ('lente_bifocal',       'Lentes Bifocales',     'lentes',  3),
    ('montura_premium',     'Monturas Premium',     'monturas', 4),
    ('montura_estandar',    'Monturas Estandar',    'monturas', 5),
    ('montura_economica',   'Monturas Economicas',  'monturas', 6),
    ('servicio_extra',      'Servicios Extra',      'servicios', 7),
    ('garantia_extendida',  'Garantias Extendidas', 'garantias', 8),
    ('otro_lente',          'Otros Lentes',         'lentes',  9)
ON CONFLICT (id) DO NOTHING;

-- #############################################################################
-- 9. Configuración financiera (required by app)
-- #############################################################################
INSERT INTO public.configuracion_financiera (id, optica_id, moneda, tasa_igv)
VALUES ('dev-config-fin-001', 'dev-optica-001', 'PEN', 0.18)
ON CONFLICT (id) DO NOTHING;

-- Restore normal trigger behavior
SET session_replication_role = 'origin';

-- =============================================================================
-- End of seed data
-- =============================================================================
