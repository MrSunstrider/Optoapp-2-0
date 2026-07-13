ALTER TABLE inventario_fisico_detalle ADD COLUMN optica_id TEXT;
UPDATE inventario_fisico_detalle d SET optica_id = f.optica_id FROM inventario_fisico f WHERE d.inventario_id = f.id;
ALTER TABLE inventario_fisico_detalle ALTER COLUMN optica_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_inventario_fisico_detalle_optica_id ON inventario_fisico_detalle(optica_id);;
