-- Add optica_id to orden_compra_items for sync filtering
-- Same pattern as inventario_fisico_detalle — child table missing the column

ALTER TABLE orden_compra_items ADD COLUMN optica_id TEXT;
UPDATE orden_compra_items i SET optica_id = o.optica_id FROM ordenes_compra o WHERE i.orden_id = o.id;
ALTER TABLE orden_compra_items ALTER COLUMN optica_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orden_compra_items_optica_id ON orden_compra_items(optica_id);
