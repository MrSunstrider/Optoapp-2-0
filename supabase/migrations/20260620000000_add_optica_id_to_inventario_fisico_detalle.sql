-- Add optica_id to inventario_fisico_detalle for sync filtering
-- The column was missing, causing sync errors in Config → Sync diagnostics
-- Sync code queries by optica_id but the column didn't exist

ALTER TABLE inventario_fisico_detalle ADD COLUMN optica_id TEXT;

-- Populate from parent inventario_fisico
UPDATE inventario_fisico_detalle d
SET optica_id = f.optica_id
FROM inventario_fisico f
WHERE d.inventario_id = f.id;

-- Make it NOT NULL after population
ALTER TABLE inventario_fisico_detalle ALTER COLUMN optica_id SET NOT NULL;

-- Add index for sync queries
CREATE INDEX idx_inventario_fisico_detalle_optica_id ON inventario_fisico_detalle(optica_id);
