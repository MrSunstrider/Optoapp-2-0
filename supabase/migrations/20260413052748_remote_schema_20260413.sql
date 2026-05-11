-- OptoApp: remote schema initial dump
-- All CREATE TABLE IF NOT EXISTS — safe to run alongside subsequent migration files.

-- ---------------------------------------------------------------------------
-- 1) opticas (tenant)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.opticas (
    id                            TEXT PRIMARY KEY,
    nombre                        TEXT NOT NULL DEFAULT '',
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    laboratorio_nombre            TEXT NOT NULL DEFAULT '',
    laboratorio_contacto          TEXT NOT NULL DEFAULT '',
    plan                          TEXT NOT NULL DEFAULT 'free',
    plan_code                     TEXT NOT NULL DEFAULT 'free',
    max_opticas                   INTEGER,
    max_pacientes_por_optica      INTEGER,
    max_usuarios_por_optica       INTEGER,
    plan_source                   TEXT NOT NULL DEFAULT 'manual',
    plan_status                   TEXT NOT NULL DEFAULT 'active',
    current_period_end            TIMESTAMPTZ,
    fiscal_doc_tipo               TEXT NOT NULL DEFAULT '',
    fiscal_doc_numero             TEXT NOT NULL DEFAULT '',
    razon_social                  TEXT NOT NULL DEFAULT '',
    direccion_fiscal              TEXT NOT NULL DEFAULT '',
    distrito_ciudad_departamento  TEXT NOT NULL DEFAULT '',
    moneda                        TEXT NOT NULL DEFAULT '',
    pais                          TEXT NOT NULL DEFAULT '',
    contacto_whatsapp_telefono    TEXT NOT NULL DEFAULT '',
    CONSTRAINT opticas_plan_code_chk CHECK (lower(trim(plan_code)) IN ('free','pro_individual','pro_multisite_15','enterprise','dev_owner')),
    CONSTRAINT opticas_plan_source_chk CHECK (lower(trim(plan_source)) IN ('playstore','web','manual','internal')),
    CONSTRAINT opticas_plan_status_chk CHECK (lower(trim(plan_status)) IN ('active','grace','canceled'))
);

COMMENT ON TABLE public.opticas IS 'Tenant (óptica); optica_id en tablas de negocio referencia opticas.id';

-- ---------------------------------------------------------------------------
-- 2) usuario_optica (membership N:N)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.usuario_optica (
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    optica_id   TEXT NOT NULL REFERENCES public.opticas(id) ON DELETE CASCADE,
    rol         TEXT NOT NULL DEFAULT 'admin',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, optica_id)
);

COMMENT ON TABLE public.usuario_optica IS 'Membresía usuario ↔ óptica con rol';

-- ---------------------------------------------------------------------------
-- 3) user_profiles (public mirror of auth.users)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_profiles (
    user_id    UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email      TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now())
);

COMMENT ON TABLE public.user_profiles IS 'Perfil público mínimo para asignación de roles por email (sin exponer auth.users)';

-- ---------------------------------------------------------------------------
-- 4) pacientes
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.pacientes (
    id                    TEXT PRIMARY KEY,
    nombre_completo       TEXT NOT NULL DEFAULT '',
    edad                  INTEGER,
    telefono              TEXT,
    fecha_creacion        DATE,
    dni                   TEXT,
    fecha_nacimiento      DATE,
    sexo                  TEXT,
    email                 TEXT,
    direccion             TEXT,
    distrito              TEXT,
    ocupacion             TEXT,
    acompanante           TEXT,
    hobbies               TEXT,
    ultimas_etiquetas     TEXT,
    optica_id             TEXT NOT NULL DEFAULT 'mi_optica_base',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    historia_optometrica  TEXT,
    updated_by            UUID
);

COMMENT ON TABLE public.pacientes IS 'Pacientes por óptica';
COMMENT ON COLUMN public.pacientes.historia_optometrica IS 'Número de historia optométrica del paciente (opcional)';

-- ---------------------------------------------------------------------------
-- 5) evaluaciones (optometric evaluations)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.evaluaciones (
    -- Core
    id                     TEXT PRIMARY KEY,
    paciente_id            TEXT NOT NULL DEFAULT '',
    fecha                  DATE,
    optica_id              TEXT NOT NULL DEFAULT 'mi_optica_base',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Visual Acuity — SC (without correction)
    av_sc_od_lejos         TEXT,
    av_sc_oi_lejos         TEXT,
    av_sc_od_cerca         TEXT,
    av_sc_oi_cerca         TEXT,
    av_sc_ao               TEXT,
    av_sc_ao_cerca         TEXT,

    -- Visual Acuity — CC (with correction)
    av_cc_od_lejos         TEXT,
    av_cc_oi_lejos         TEXT,
    av_cc_od_cerca         TEXT,
    av_cc_oi_cerca         TEXT,
    av_cc_ao               TEXT,
    av_cc_ao_cerca         TEXT,

    -- Objective refraction
    obj_od_esf             TEXT,
    obj_od_cil             TEXT,
    obj_od_eje             TEXT,
    obj_oi_esf             TEXT,
    obj_oi_cil             TEXT,
    obj_oi_eje             TEXT,

    -- Subjective refraction
    subj_od_esf            TEXT,
    subj_od_cil            TEXT,
    subj_od_eje            TEXT,
    subj_oi_esf            TEXT,
    subj_oi_cil            TEXT,
    subj_oi_eje            TEXT,

    -- Prescription (receta)
    receta_od_esf          TEXT,
    receta_od_cil          TEXT,
    receta_od_eje          TEXT,
    receta_oi_esf          TEXT,
    receta_oi_cil          TEXT,
    receta_oi_eje          TEXT,

    -- Near / Intermediate add
    add_cerca_od           TEXT,
    add_cerca_oi           TEXT,
    add_intermedia_od      TEXT,
    add_intermedia_oi      TEXT,
    add_av                 TEXT,

    -- DIP / Distance interpupillary
    dip_lejos              TEXT,
    dip_cerca              TEXT,
    dip_intermedio         TEXT,
    dip_lejos_mm           TEXT,
    dip_total_mm           TEXT,
    dnp_od_mm              TEXT,
    dnp_oi_mm              TEXT,

    -- Diagnosis
    diagnostico            TEXT,
    diagnostico_od         TEXT,
    diagnostico_oi         TEXT,
    diagnostico_otros      TEXT,
    plan_tratamiento       TEXT,
    observaciones          TEXT,

    -- Boolean flags
    balance_od             TEXT,
    balance_oi             TEXT,
    otros_presbicia        TEXT,
    otros_anisometropia    TEXT,
    otros_ambliopia        TEXT,
    auto_presbicia         TEXT,
    auto_anisometropia     TEXT,
    auto_ambliopia         TEXT,

    -- Contact lens
    lc_od_esf              TEXT,
    lc_od_cil              TEXT,
    lc_od_eje              TEXT,
    lc_oi_esf              TEXT,
    lc_oi_cil              TEXT,
    lc_oi_eje              TEXT,
    lc_radio_base_od       TEXT,
    lc_diametro_od         TEXT,
    lc_radio_base_oi       TEXT,
    lc_diametro_oi         TEXT,
    lc_laboratorio         TEXT,
    lc_tipo_lente          TEXT,
    lc_material            TEXT,
    lc_observaciones       TEXT,

    -- Tests
    ph_od                  TEXT,
    ph_oi                  TEXT,
    kappa_od               TEXT,
    kappa_oi               TEXT,
    hirshberg              TEXT,
    ducciones_od           TEXT,
    ducciones_oi           TEXT,
    versiones_ao           TEXT,
    estereopsis_valor      TEXT,
    estereopsis_segundos   TEXT,
    lang                   TEXT,
    worth                  TEXT,
    ishihara               TEXT,
    farnsworth             TEXT,
    schirmer_od            TEXT,
    schirmer_oi            TEXT,
    osdi_puntuacion        TEXT,
    osdi_clasificacion     TEXT,
    sensibilidad_contraste TEXT,
    sensibilidad_frecuencia TEXT,
    amsler                 TEXT,
    campo_visual           TEXT,
    campo_visual_descripcion TEXT,
    cover_test_6m          TEXT,
    cover_test_40cm        TEXT,
    cover_test_10cm        TEXT,
    ppc_or                 TEXT,
    ppc_luz                TEXT,
    ppc_frl                TEXT,
    reflejo_fotomotor      TEXT,
    reflejo_consensual     TEXT,
    reflejo_acomodativo    TEXT,

    -- Keratometry
    k1_od                  TEXT,
    k2_od                  TEXT,
    k1_oi                  TEXT,
    k2_oi                  TEXT,

    -- Prism
    prisma_od_valor        TEXT,
    prisma_od_base         TEXT,
    prisma_oi_valor        TEXT,
    prisma_oi_base         TEXT,

    -- Appointment
    proxima_fecha_control  TEXT,
    proxima_cita           DATE,
    lc_fecha_adaptacion    DATE,
    cita_estado            TEXT NOT NULL DEFAULT 'programada',

    -- Audit
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_by             UUID
);

COMMENT ON TABLE public.evaluaciones IS 'Evaluaciones optométricas';

-- ---------------------------------------------------------------------------
-- 6) dispensaciones
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.dispensaciones (
    id                        TEXT PRIMARY KEY,
    paciente_id               TEXT NOT NULL DEFAULT '',
    fecha                     DATE,
    optica_id                 TEXT NOT NULL DEFAULT 'mi_optica_base',
    tipo_montura              TEXT,
    material_montura          TEXT,
    tipo_lente                TEXT,
    material_lente            TEXT,
    tratamientos              TEXT,
    color_lente               TEXT,
    notas_diseno              TEXT,
    origen_montura            TEXT,
    tipo_aro                  TEXT,
    descripcion_montura       TEXT,
    monto_total               NUMERIC,
    metodo_pago               TEXT,
    monto_pagado              NUMERIC,
    estado_entrega            TEXT NOT NULL DEFAULT 'Pendiente',
    fecha_vencimiento_garantia DATE,
    distancia_lente           TEXT,
    sub_tipo_bifocal          TEXT,
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    altura                    TEXT,
    ot                        TEXT,
    montura_id                TEXT,
    updated_by                UUID,
    CONSTRAINT dispensaciones_estado_entrega_domain_chk CHECK (estado_entrega IN ('Pendiente', 'Entregado'))
);

COMMENT ON TABLE public.dispensaciones IS 'Dispensaciones / Órdenes de trabajo';

-- ---------------------------------------------------------------------------
-- 7) pagos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.pagos (
    id                TEXT PRIMARY KEY,
    dispensacion_id   TEXT,
    servicio_extra_id TEXT,
    fecha             DATE,
    tipo              TEXT NOT NULL DEFAULT 'Abono',
    monto             NUMERIC,
    metodo_pago       TEXT NOT NULL DEFAULT 'Efectivo',
    nota              TEXT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    optica_id         TEXT NOT NULL DEFAULT 'mi_optica_base',
    updated_by        UUID,
    CONSTRAINT pagos_metodo_pago_not_blank_chk CHECK (btrim(metodo_pago) <> ''),
    CONSTRAINT pagos_tipo_not_blank_chk CHECK (btrim(tipo) <> ''),
    CONSTRAINT pagos_optica_id_not_blank_chk CHECK (btrim(optica_id) <> '')
);

COMMENT ON TABLE public.pagos IS 'Pagos asociados a dispensaciones o servicios extra';

-- ---------------------------------------------------------------------------
-- 8) servicios_extra
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.servicios_extra (
    id              TEXT PRIMARY KEY,
    ot              TEXT NOT NULL DEFAULT '',
    descripcion     TEXT NOT NULL DEFAULT 'Sin descripción',
    monto_total     NUMERIC,
    a_cuenta        NUMERIC,
    estado          TEXT NOT NULL DEFAULT 'Pendiente',
    fecha           DATE,
    paciente_id     TEXT,
    metodo_pago     TEXT NOT NULL DEFAULT 'Sin especificar',
    optica_id       TEXT NOT NULL DEFAULT 'mi_optica_base',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_by      UUID,
    CONSTRAINT servicios_extra_estado_domain_chk CHECK (estado IN ('Pendiente', 'Entregado')),
    CONSTRAINT servicios_extra_descripcion_not_blank_chk CHECK (btrim(descripcion) <> ''),
    CONSTRAINT servicios_extra_estado_not_blank_chk CHECK (btrim(estado) <> ''),
    CONSTRAINT servicios_extra_metodo_pago_not_blank_chk CHECK (btrim(metodo_pago) <> ''),
    CONSTRAINT servicios_extra_optica_id_not_blank_chk CHECK (btrim(optica_id) <> ''),
    CONSTRAINT servicios_extra_ot_not_dash_placeholder_chk CHECK (btrim(coalesce(ot, '')) <> '-')
);

COMMENT ON TABLE public.servicios_extra IS 'Servicios adicionales no cubiertos por dispensaciones';
COMMENT ON COLUMN public.servicios_extra.paciente_id IS 'FK opcional: puede ser NULL para servicios no asociados a un paciente';

-- ---------------------------------------------------------------------------
-- 9) monturas (frames inventory)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.monturas (
    id            TEXT PRIMARY KEY,
    sku           TEXT,
    marca         TEXT,
    modelo        TEXT,
    color         TEXT,
    talla         TEXT,
    costo         NUMERIC,
    precio        NUMERIC,
    stock_actual  INTEGER,
    stock_minimo  INTEGER,
    activo        BOOLEAN NOT NULL DEFAULT true,
    optica_id     TEXT NOT NULL DEFAULT 'mi_optica_base',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.monturas IS 'Inventario de monturas';

-- ---------------------------------------------------------------------------
-- 10) montura_movimientos (frame movement ledger)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.montura_movimientos (
    id            TEXT PRIMARY KEY,
    montura_id    TEXT NOT NULL,
    fecha         DATE NOT NULL DEFAULT CURRENT_DATE,
    tipo          TEXT,
    cantidad      INTEGER,
    stock_previo  INTEGER,
    stock_nuevo   INTEGER,
    referencia_id TEXT,
    nota          TEXT,
    optica_id     TEXT
);

COMMENT ON TABLE public.montura_movimientos IS 'Movimientos de stock de monturas';

-- ---------------------------------------------------------------------------
-- 11) cierres_caja
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.cierres_caja (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id             TEXT NOT NULL REFERENCES public.opticas(id) ON DELETE CASCADE,
    fecha_operativa       DATE NOT NULL,
    estado                TEXT NOT NULL CHECK (estado IN ('abierto', 'cerrado')),
    total_efectivo_cent   INTEGER NOT NULL DEFAULT 0 CHECK (total_efectivo_cent >= 0),
    total_tarjeta_cent    INTEGER NOT NULL DEFAULT 0 CHECK (total_tarjeta_cent >= 0),
    total_transferencia_cent INTEGER NOT NULL DEFAULT 0 CHECK (total_transferencia_cent >= 0),
    total_yape_cent       INTEGER NOT NULL DEFAULT 0 CHECK (total_yape_cent >= 0),
    total_plin_cent       INTEGER NOT NULL DEFAULT 0 CHECK (total_plin_cent >= 0),
    total_general_cent    INTEGER NOT NULL DEFAULT 0 CHECK (total_general_cent >= 0),
    observaciones         TEXT,
    closed_by             UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    closed_at             TIMESTAMPTZ,
    reopened_by           UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    reopened_at           TIMESTAMPTZ,
    cerrado_at            TIMESTAMPTZ,
    cerrado_por           UUID,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (optica_id, fecha_operativa)
);

COMMENT ON TABLE public.cierres_caja IS 'Cierre de caja formal por óptica y fecha operativa';

-- ---------------------------------------------------------------------------
-- 12) optica_settings
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.optica_settings (
    optica_id   TEXT PRIMARY KEY REFERENCES public.opticas(id) ON DELETE CASCADE,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.optica_settings IS 'Configuración operativa compartida por óptica (multiusuario)';

-- ---------------------------------------------------------------------------
-- 13) sync_telemetry_optica
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.sync_telemetry_optica (
    optica_id    TEXT PRIMARY KEY REFERENCES public.opticas(id) ON DELETE CASCADE,
    last_sync_at TIMESTAMPTZ,
    last_status  TEXT NOT NULL DEFAULT 'idle' CHECK (last_status IN ('idle', 'ok', 'error')),
    last_stage   TEXT NOT NULL DEFAULT '',
    last_error   TEXT NOT NULL DEFAULT '',
    last_actor   UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now())
);

COMMENT ON TABLE public.sync_telemetry_optica IS 'Telemetría resumida de última sincronización por óptica (uso operativo/soporte)';

-- ---------------------------------------------------------------------------
-- 14) pacientes_delete_audit
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.pacientes_delete_audit (
    id           BIGSERIAL PRIMARY KEY,
    optica_id    TEXT NOT NULL REFERENCES public.opticas(id) ON DELETE CASCADE,
    paciente_id  TEXT NOT NULL,
    deleted_by   UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    deleted_at   TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now())
);

COMMENT ON TABLE public.pacientes_delete_audit IS 'Auditoría de eliminaciones de pacientes (límite diario + trazabilidad)';

-- ---------------------------------------------------------------------------
-- 15) schema_migrations_flags
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.schema_migrations_flags (
    key         TEXT PRIMARY KEY,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.schema_migrations_flags IS 'Registro interno de hitos de migración ya ejecutados';

-- ---------------------------------------------------------------------------
-- UNIQUE INDEXES (per-optica business constraints)
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS pacientes_optica_historia_optometrica_uq
    ON public.pacientes (optica_id, upper(btrim(historia_optometrica)))
    WHERE nullif(btrim(coalesce(historia_optometrica, '')), '') IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS dispensaciones_optica_ot_uq_v2
    ON public.dispensaciones (optica_id, upper(btrim(ot)))
    WHERE nullif(btrim(coalesce(ot, '')), '') IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS servicios_extra_optica_ot_uq
    ON public.servicios_extra (optica_id, upper(btrim(ot)))
    WHERE nullif(btrim(coalesce(ot, '')), '') IS NOT NULL;
