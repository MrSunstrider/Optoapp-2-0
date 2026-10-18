-- Which eyes are dispensed on a product line (Ambos | OD | OI).
ALTER TABLE public.dispensacion_items
    ADD COLUMN IF NOT EXISTS ojos_lente TEXT NOT NULL DEFAULT 'Ambos';
