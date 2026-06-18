-- Sprint A: Data Model Extensions for inventario-v2
-- ALTER existing tables + 7 CREATE TABLE with RLS

-- ============================================================
-- 1. ALTER monturas — 5 new catalog/commercial fields
-- ============================================================
ALTER TABLE monturas ADD COLUMN IF NOT EXISTS categoria TEXT NOT NULL DEFAULT '';
ALTER TABLE monturas ADD COLUMN IF NOT EXISTS coleccion TEXT NOT NULL DEFAULT '';
ALTER TABLE monturas ADD COLUMN IF NOT EXISTS temporada TEXT NOT NULL DEFAULT '';
ALTER TABLE monturas ADD COLUMN IF NOT EXISTS estado_comercial TEXT NOT NULL DEFAULT '';
ALTER TABLE monturas ADD COLUMN IF NOT EXISTS genero TEXT NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_monturas_estado_comercial ON monturas(estado_comercial);
CREATE INDEX IF NOT EXISTS idx_monturas_categoria ON monturas(categoria);

-- ============================================================
-- 2. ALTER montura_movimientos — 3 new audit fields
-- ============================================================
ALTER TABLE montura_movimientos ADD COLUMN IF NOT EXISTS user_id TEXT NOT NULL DEFAULT '';
ALTER TABLE montura_movimientos ADD COLUMN IF NOT EXISTS costo_unitario DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE montura_movimientos ADD COLUMN IF NOT EXISTS tipo_documento TEXT NOT NULL DEFAULT '';

CREATE UNIQUE INDEX IF NOT EXISTS idx_movimientos_conflict
  ON montura_movimientos(referencia_id, tipo, montura_id);

-- ============================================================
-- 3. CREATE TABLE proveedores
-- ============================================================
CREATE TABLE IF NOT EXISTS proveedores (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre TEXT NOT NULL,
  ruc TEXT NOT NULL,
  telefono TEXT NOT NULL DEFAULT '',
  email TEXT NOT NULL DEFAULT '',
  direccion TEXT NOT NULL DEFAULT '',
  contacto TEXT NOT NULL DEFAULT '',
  activo BOOLEAN NOT NULL DEFAULT true,
  optica_id TEXT NOT NULL REFERENCES opticas(id),
  updated_at TIMESTAMPTZ DEFAULT now(),
  updated_by TEXT,
  UNIQUE(ruc, optica_id)
);

ALTER TABLE proveedores ENABLE ROW LEVEL SECURITY;

CREATE POLICY "proveedores_select"
  ON proveedores FOR SELECT
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "proveedores_insert"
  ON proveedores FOR INSERT
  WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "proveedores_update"
  ON proveedores FOR UPDATE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "proveedores_delete"
  ON proveedores FOR DELETE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

-- ============================================================
-- 4. CREATE TABLE montura_proveedor
-- ============================================================
CREATE TABLE IF NOT EXISTS montura_proveedor (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  montura_id TEXT NOT NULL REFERENCES monturas(id) ON DELETE CASCADE,
  proveedor_id TEXT NOT NULL REFERENCES proveedores(id) ON DELETE CASCADE,
  costo_proveedor DOUBLE PRECISION NOT NULL DEFAULT 0,
  precio_sugerido DOUBLE PRECISION NOT NULL DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT true,
  UNIQUE(montura_id, proveedor_id)
);

ALTER TABLE montura_proveedor ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view montura_proveedor for their optica"
  ON montura_proveedor FOR SELECT
  USING (
    montura_id IN (
      SELECT id FROM monturas WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can insert montura_proveedor for their optica"
  ON montura_proveedor FOR INSERT
  WITH CHECK (
    montura_id IN (
      SELECT id FROM monturas WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can update montura_proveedor for their optica"
  ON montura_proveedor FOR UPDATE
  USING (
    montura_id IN (
      SELECT id FROM monturas WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can delete montura_proveedor for their optica"
  ON montura_proveedor FOR DELETE
  USING (
    montura_id IN (
      SELECT id FROM monturas WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

-- ============================================================
-- 5. CREATE TABLE categorias_montura
-- ============================================================
CREATE TABLE IF NOT EXISTS categorias_montura (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre TEXT NOT NULL,
  descripcion TEXT NOT NULL DEFAULT '',
  optica_id TEXT NOT NULL REFERENCES opticas(id),
  UNIQUE(nombre, optica_id)
);

ALTER TABLE categorias_montura ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view categorias_montura for their optica"
  ON categorias_montura FOR SELECT
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can insert categorias_montura for their optica"
  ON categorias_montura FOR INSERT
  WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can update categorias_montura for their optica"
  ON categorias_montura FOR UPDATE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can delete categorias_montura for their optica"
  ON categorias_montura FOR DELETE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

-- ============================================================
-- 6. CREATE TABLE ordenes_compra
-- ============================================================
CREATE TABLE IF NOT EXISTS ordenes_compra (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  numero TEXT NOT NULL,
  proveedor_id TEXT NOT NULL REFERENCES proveedores(id),
  fecha DATE NOT NULL,
  estado TEXT NOT NULL DEFAULT 'PENDIENTE',
  total DOUBLE PRECISION NOT NULL DEFAULT 0,
  optica_id TEXT NOT NULL REFERENCES opticas(id),
  updated_at TIMESTAMPTZ DEFAULT now(),
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_oc_estado ON ordenes_compra(estado);
CREATE INDEX IF NOT EXISTS idx_oc_numero ON ordenes_compra(numero);

ALTER TABLE ordenes_compra ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view ordenes_compra for their optica"
  ON ordenes_compra FOR SELECT
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can insert ordenes_compra for their optica"
  ON ordenes_compra FOR INSERT
  WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can update ordenes_compra for their optica"
  ON ordenes_compra FOR UPDATE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can delete ordenes_compra for their optica"
  ON ordenes_compra FOR DELETE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

-- ============================================================
-- 7. CREATE TABLE orden_compra_items
-- ============================================================
CREATE TABLE IF NOT EXISTS orden_compra_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  orden_id TEXT NOT NULL REFERENCES ordenes_compra(id) ON DELETE CASCADE,
  montura_id TEXT NOT NULL REFERENCES monturas(id),
  cantidad INTEGER NOT NULL,
  costo_unitario DOUBLE PRECISION NOT NULL DEFAULT 0,
  recibido INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE orden_compra_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view orden_compra_items for their optica"
  ON orden_compra_items FOR SELECT
  USING (
    orden_id IN (
      SELECT id FROM ordenes_compra WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can insert orden_compra_items for their optica"
  ON orden_compra_items FOR INSERT
  WITH CHECK (
    orden_id IN (
      SELECT id FROM ordenes_compra WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can update orden_compra_items for their optica"
  ON orden_compra_items FOR UPDATE
  USING (
    orden_id IN (
      SELECT id FROM ordenes_compra WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can delete orden_compra_items for their optica"
  ON orden_compra_items FOR DELETE
  USING (
    orden_id IN (
      SELECT id FROM ordenes_compra WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

-- ============================================================
-- 8. CREATE TABLE inventario_fisico
-- ============================================================
CREATE TABLE IF NOT EXISTS inventario_fisico (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  fecha DATE NOT NULL DEFAULT CURRENT_DATE,
  estado TEXT NOT NULL DEFAULT 'EN_PROGRESO',
  optica_id TEXT NOT NULL REFERENCES opticas(id),
  user_id TEXT NOT NULL,
  notas TEXT NOT NULL DEFAULT ''
);

ALTER TABLE inventario_fisico ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view inventario_fisico for their optica"
  ON inventario_fisico FOR SELECT
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can insert inventario_fisico for their optica"
  ON inventario_fisico FOR INSERT
  WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can update inventario_fisico for their optica"
  ON inventario_fisico FOR UPDATE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

CREATE POLICY "Users can delete inventario_fisico for their optica"
  ON inventario_fisico FOR DELETE
  USING (optica_id = (auth.jwt() ->> 'optica_id')::text);

-- ============================================================
-- 9. CREATE TABLE inventario_fisico_detalle
-- ============================================================
CREATE TABLE IF NOT EXISTS inventario_fisico_detalle (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  inventario_id TEXT NOT NULL REFERENCES inventario_fisico(id) ON DELETE CASCADE,
  montura_id TEXT NOT NULL REFERENCES monturas(id),
  stock_sistema INTEGER NOT NULL,
  stock_contado INTEGER,
  diferencia INTEGER,
  UNIQUE(inventario_id, montura_id)
);

ALTER TABLE inventario_fisico_detalle ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view inventario_fisico_detalle for their optica"
  ON inventario_fisico_detalle FOR SELECT
  USING (
    inventario_id IN (
      SELECT id FROM inventario_fisico WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can insert inventario_fisico_detalle for their optica"
  ON inventario_fisico_detalle FOR INSERT
  WITH CHECK (
    inventario_id IN (
      SELECT id FROM inventario_fisico WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can update inventario_fisico_detalle for their optica"
  ON inventario_fisico_detalle FOR UPDATE
  USING (
    inventario_id IN (
      SELECT id FROM inventario_fisico WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );

CREATE POLICY "Users can delete inventario_fisico_detalle for their optica"
  ON inventario_fisico_detalle FOR DELETE
  USING (
    inventario_id IN (
      SELECT id FROM inventario_fisico WHERE optica_id = (auth.jwt() ->> 'optica_id')::text
    )
  );
