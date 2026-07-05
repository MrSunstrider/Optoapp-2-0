-- Migration: Fase 6 — Esquema de datos para analisis de negocio
-- Creates 8 tables, alters ventas, adds RLS policies, seeds categorias_producto,
-- and creates the recalcular_resumen_diario() RPC function.
-- All DDL is idempotent: IF NOT EXISTS / DROP POLICY IF EXISTS / ON CONFLICT DO NOTHING.

-- ============================================================================
-- 1. categorias_producto — global seed table (no optica_id)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.categorias_producto (
    id      TEXT PRIMARY KEY,
    nombre  TEXT NOT NULL,
    familia TEXT NOT NULL CHECK (familia IN ('lente', 'montura', 'servicio')),
    orden   INTEGER NOT NULL DEFAULT 0
);

-- Seed data (idempotent)
INSERT INTO public.categorias_producto (id, nombre, familia, orden) VALUES
    ('lente_progresivo',  'Lentes Progresivos',     'lente',    1),
    ('lente_monofocal',   'Lentes Monofocales',      'lente',    2),
    ('lente_bifocal',     'Lentes Bifocales',        'lente',    3),
    ('lente_otro',        'Otros Lentes',            'lente',    9),
    ('montura_premium',   'Monturas Premium',        'montura',  4),
    ('montura_estandar',  'Monturas Estandar',       'montura',  5),
    ('montura_economica', 'Monturas Economicas',      'montura',  6),
    ('servicio_extra',    'Servicios Extra',          'servicio', 7),
    ('servicio_garantia', 'Garantias Extendidas',     'servicio', 8)
ON CONFLICT (id) DO NOTHING;

-- RLS: global table — all authenticated users can read, admin in any optica can modify
ALTER TABLE public.categorias_producto ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS categorias_producto_select ON public.categorias_producto;
CREATE POLICY categorias_producto_select ON public.categorias_producto FOR SELECT
    USING (true);

-- Since categorias_producto has no optica_id, admin check is global (admin in any optica)
DROP POLICY IF EXISTS categorias_producto_insert ON public.categorias_producto;
CREATE POLICY categorias_producto_insert ON public.categorias_producto FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.usuario_optica
        WHERE user_id = auth.uid() AND lower(trim(rol)) = 'admin'
    ));

DROP POLICY IF EXISTS categorias_producto_delete ON public.categorias_producto;
CREATE POLICY categorias_producto_delete ON public.categorias_producto FOR DELETE
    USING (EXISTS (
        SELECT 1 FROM public.usuario_optica
        WHERE user_id = auth.uid() AND lower(trim(rol)) = 'admin'
    ));

-- ============================================================================
-- 2. ALTER ventas — add categoria_producto_id FK
-- ============================================================================

ALTER TABLE public.ventas ADD COLUMN IF NOT EXISTS categoria_producto_id TEXT
    REFERENCES public.categorias_producto(id);

CREATE INDEX IF NOT EXISTS idx_ventas_categoria
    ON public.ventas (categoria_producto_id);

-- ============================================================================
-- 3. costos_productos — product cost history per optica
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.costos_productos (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id             TEXT NOT NULL REFERENCES public.opticas(id),
    categoria_producto_id TEXT NOT NULL REFERENCES public.categorias_producto(id),
    producto_descripcion  TEXT,
    costo_unitario        NUMERIC NOT NULL,
    vigente_desde         DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta         DATE,
    fecha_actualizacion   TIMESTAMPTZ DEFAULT now()
);

-- Partial index: only currently active costs
CREATE INDEX IF NOT EXISTS idx_costos_vigentes
    ON public.costos_productos (optica_id, categoria_producto_id)
    WHERE vigente_hasta IS NULL;

-- RLS
ALTER TABLE public.costos_productos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS costos_productos_select ON public.costos_productos;
CREATE POLICY costos_productos_select ON public.costos_productos FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS costos_productos_insert ON public.costos_productos;
CREATE POLICY costos_productos_insert ON public.costos_productos FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_productos_update ON public.costos_productos;
CREATE POLICY costos_productos_update ON public.costos_productos FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS costos_productos_delete ON public.costos_productos;
CREATE POLICY costos_productos_delete ON public.costos_productos FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin']));

-- ============================================================================
-- 4. configuracion_financiera — per-optica financial settings (1:1 with opticas)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.configuracion_financiera (
    optica_id                  TEXT PRIMARY KEY REFERENCES public.opticas(id),
    margen_neto_objetivo       NUMERIC DEFAULT 15.0,
    ticket_promedio_objetivo   NUMERIC,
    caida_ventas_alerta_pct    NUMERIC DEFAULT 10.0,
    deuda_vieja_alerta_dias    INTEGER DEFAULT 30,
    deuda_total_alerta_monto   NUMERIC DEFAULT 3000.0,
    stock_estancado_alerta_dias INTEGER DEFAULT 180,
    stock_bajo_alerta_unidades INTEGER DEFAULT 2,
    min_ventas_para_recomendar INTEGER DEFAULT 5,
    frecuencia_recalculo_dias  INTEGER DEFAULT 1
);

-- RLS
ALTER TABLE public.configuracion_financiera ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS configuracion_financiera_select ON public.configuracion_financiera;
CREATE POLICY configuracion_financiera_select ON public.configuracion_financiera FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS configuracion_financiera_insert ON public.configuracion_financiera;
CREATE POLICY configuracion_financiera_insert ON public.configuracion_financiera FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS configuracion_financiera_update ON public.configuracion_financiera;
CREATE POLICY configuracion_financiera_update ON public.configuracion_financiera FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS configuracion_financiera_delete ON public.configuracion_financiera;
CREATE POLICY configuracion_financiera_delete ON public.configuracion_financiera FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin']));

-- ============================================================================
-- 5. gastos_operativos — operative expenses per optica
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.gastos_operativos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id         TEXT NOT NULL REFERENCES public.opticas(id),
    categoria         TEXT NOT NULL CHECK (categoria IN (
                          'alquiler', 'servicios', 'personal', 'proveedores',
                          'insumos', 'marketing', 'impuestos', 'otro'
                      )),
    descripcion       TEXT,
    monto             NUMERIC NOT NULL,
    fecha             DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_programada  DATE,
    nota              TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_gastos_opt_fecha
    ON public.gastos_operativos (optica_id, fecha);

-- RLS
ALTER TABLE public.gastos_operativos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS gastos_operativos_select ON public.gastos_operativos;
CREATE POLICY gastos_operativos_select ON public.gastos_operativos FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS gastos_operativos_insert ON public.gastos_operativos;
CREATE POLICY gastos_operativos_insert ON public.gastos_operativos FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS gastos_operativos_update ON public.gastos_operativos;
CREATE POLICY gastos_operativos_update ON public.gastos_operativos FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

DROP POLICY IF EXISTS gastos_operativos_delete ON public.gastos_operativos;
CREATE POLICY gastos_operativos_delete ON public.gastos_operativos FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente']));

-- ============================================================================
-- 6. margen_por_categoria — margin analysis per category/period (server-only)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.margen_por_categoria (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id             TEXT NOT NULL REFERENCES public.opticas(id),
    categoria_producto_id TEXT NOT NULL REFERENCES public.categorias_producto(id),
    periodo               TEXT NOT NULL,
    tipo_periodo          TEXT NOT NULL CHECK (tipo_periodo IN ('mensual', 'trimestral', 'anual')),
    ventas_totales        NUMERIC NOT NULL,
    costo_total           NUMERIC NOT NULL,
    cantidad_ventas       INTEGER NOT NULL,
    margen_bruto          NUMERIC NOT NULL,
    margen_porcentaje     NUMERIC NOT NULL,
    ticket_promedio       NUMERIC NOT NULL,
    calculado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (optica_id, categoria_producto_id, periodo, tipo_periodo)
);

CREATE INDEX IF NOT EXISTS idx_margen_cat_opt_per
    ON public.margen_por_categoria (optica_id, periodo);

-- RLS: server-calculated, read-only from client
ALTER TABLE public.margen_por_categoria ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS margen_por_categoria_select ON public.margen_por_categoria;
CREATE POLICY margen_por_categoria_select ON public.margen_por_categoria FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

-- ============================================================================
-- 7. resumen_diario — daily financial summaries (server-calculated, read-only)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.resumen_diario (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id                TEXT NOT NULL REFERENCES public.opticas(id),
    fecha                    DATE NOT NULL,
    ventas_cantidad          INTEGER NOT NULL DEFAULT 0,
    ventas_monto_total       NUMERIC NOT NULL DEFAULT 0,
    ventas_costo_total       NUMERIC NOT NULL DEFAULT 0,
    cobros_cantidad          INTEGER NOT NULL DEFAULT 0,
    cobros_monto_total       NUMERIC NOT NULL DEFAULT 0,
    saldo_pendiente_total    NUMERIC NOT NULL DEFAULT 0,
    saldo_pendiente_cantidad INTEGER NOT NULL DEFAULT 0,
    inventario_valor         NUMERIC,
    inventario_unidades      INTEGER,
    calculado_en             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (optica_id, fecha)
);

CREATE INDEX IF NOT EXISTS idx_resumen_diario_opt_fecha
    ON public.resumen_diario (optica_id, fecha);

-- RLS: server-calculated, read-only from client
ALTER TABLE public.resumen_diario ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS resumen_diario_select ON public.resumen_diario;
CREATE POLICY resumen_diario_select ON public.resumen_diario FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

-- ============================================================================
-- 8. feedback_recomendaciones — recommendation feedback (append-only)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.feedback_recomendaciones (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id        TEXT NOT NULL REFERENCES public.opticas(id),
    recomendacion_id TEXT NOT NULL,
    fue_util         BOOLEAN NOT NULL,
    fecha            TIMESTAMPTZ DEFAULT now()
);

-- RLS: append-only feedback
ALTER TABLE public.feedback_recomendaciones ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS feedback_recomendaciones_select ON public.feedback_recomendaciones;
CREATE POLICY feedback_recomendaciones_select ON public.feedback_recomendaciones FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

DROP POLICY IF EXISTS feedback_recomendaciones_insert ON public.feedback_recomendaciones;
CREATE POLICY feedback_recomendaciones_insert ON public.feedback_recomendaciones FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));

-- ============================================================================
-- 9. recalcular_resumen_diario — RPC function (idempotent daily aggregation)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY INVOKER
AS $$
DECLARE
    v_ventas_cantidad INTEGER;
    v_ventas_monto NUMERIC;
    v_ventas_costo NUMERIC;
    v_cobros_cantidad INTEGER;
    v_cobros_monto NUMERIC;
    v_saldo_total NUMERIC;
    v_saldo_cantidad INTEGER;
    v_inv_valor NUMERIC;
    v_inv_unidades INTEGER;
BEGIN
    -- Ventas del dia
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto_total), 0), COALESCE(SUM(costo_unitario_snapshot), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM public.ventas
    WHERE optica_id = p_optica_id AND fecha = p_fecha;

    -- Cobros del dia
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha;

    -- Saldo pendiente acumulado
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(v.monto_total - COALESCE(pg.total_pagado, 0)), 0)
    INTO v_saldo_cantidad, v_saldo_total
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos WHERE optica_id = p_optica_id GROUP BY venta_id
    ) pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
      AND v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;

    -- Inventario al cierre
    SELECT COALESCE(SUM(costo * stock_actual), 0), COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas WHERE optica_id = p_optica_id;

    -- Upsert idempotente
    INSERT INTO public.resumen_diario (
        optica_id, fecha,
        ventas_cantidad, ventas_monto_total, ventas_costo_total,
        cobros_cantidad, cobros_monto_total,
        saldo_pendiente_total, saldo_pendiente_cantidad,
        inventario_valor, inventario_unidades
    ) VALUES (
        p_optica_id, p_fecha,
        v_ventas_cantidad, v_ventas_monto, v_ventas_costo,
        v_cobros_cantidad, v_cobros_monto,
        v_saldo_total, v_saldo_cantidad,
        v_inv_valor, v_inv_unidades
    )
    ON CONFLICT (optica_id, fecha) DO UPDATE SET
        ventas_cantidad = EXCLUDED.ventas_cantidad,
        ventas_monto_total = EXCLUDED.ventas_monto_total,
        ventas_costo_total = EXCLUDED.ventas_costo_total,
        cobros_cantidad = EXCLUDED.cobros_cantidad,
        cobros_monto_total = EXCLUDED.cobros_monto_total,
        saldo_pendiente_total = EXCLUDED.saldo_pendiente_total,
        saldo_pendiente_cantidad = EXCLUDED.saldo_pendiente_cantidad,
        inventario_valor = EXCLUDED.inventario_valor,
        inventario_unidades = EXCLUDED.inventario_unidades,
        calculado_en = now();
END;
$$;
