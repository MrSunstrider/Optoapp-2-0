-- Additive catalog keys for product-family cost matrix (serie already exists).
ALTER TABLE public.costos_productos
    ADD COLUMN IF NOT EXISTS subtipo_bifocal TEXT,
    ADD COLUMN IF NOT EXISTS indice_refraccion TEXT,
    ADD COLUMN IF NOT EXISTS curva_base TEXT;

ALTER TABLE public.dispensacion_items
    ADD COLUMN IF NOT EXISTS forzar_stock_astigmatismo BOOLEAN NOT NULL DEFAULT false;
