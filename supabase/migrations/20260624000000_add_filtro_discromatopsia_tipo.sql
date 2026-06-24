-- Add filtro_discromatopsia_tipo column to dispensaciones and dispensacion_items
ALTER TABLE dispensaciones ADD COLUMN IF NOT EXISTS filtro_discromatopsia_tipo TEXT NOT NULL DEFAULT '';
ALTER TABLE dispensacion_items ADD COLUMN IF NOT EXISTS filtro_discromatopsia_tipo TEXT NOT NULL DEFAULT '';
