-- F2-T1: Nueva tabla dispensacion_items para múltiples lentes por OT.
--
-- Cada dispensación (header) puede tener 1..N items (lentes).
-- Los datos de lente existentes en dispensaciones se mantienen como
-- el item "principal" para backward compatibility.
-- Los items adicionales se almacenan en esta nueva tabla.

CREATE TABLE IF NOT EXISTS public.dispensacion_items (
    id TEXT PRIMARY KEY,
    dispensacion_id TEXT NOT NULL REFERENCES public.dispensaciones(id) ON DELETE CASCADE,
    tipo_lente TEXT DEFAULT '',
    material_lente TEXT DEFAULT '',
    tratamientos TEXT DEFAULT '',
    color_lente TEXT DEFAULT '',
    distancia_lente TEXT DEFAULT '',
    altura TEXT DEFAULT '',
    sub_tipo_bifocal TEXT DEFAULT '',
    notas_diseno TEXT DEFAULT '',
    montura_id TEXT DEFAULT '',
    origen_montura TEXT DEFAULT '',
    tipo_aro TEXT DEFAULT '',
    material_montura TEXT DEFAULT '',
    descripcion_montura TEXT DEFAULT '',
    tipo_montura TEXT DEFAULT '',
    optica_id TEXT NOT NULL DEFAULT 'mi_optica_base'
);

COMMENT ON TABLE public.dispensacion_items IS 'Items de lente + montura dentro de una dispensación (F2-T2). Cada item puede tener su propia montura.';

CREATE INDEX IF NOT EXISTS idx_dispensacion_items_dispensacion_id
    ON public.dispensacion_items (dispensacion_id);

CREATE INDEX IF NOT EXISTS idx_dispensacion_items_optica_id
    ON public.dispensacion_items (optica_id);
