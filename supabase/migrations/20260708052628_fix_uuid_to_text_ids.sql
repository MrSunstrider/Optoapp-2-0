-- Drop ALL policies on affected tables (safe drop before ALTER COLUMN TYPE)
DO $$
DECLARE
    pol record;
BEGIN
    FOR pol IN
        SELECT policyname, tablename FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('proveedores','ordenes_compra','orden_compra_items',
                            'inventario_fisico','inventario_fisico_detalle',
                            'montura_proveedor','categorias_montura')
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', pol.policyname, pol.tablename);
    END LOOP;
END;
$$;

-- Disable RLS before altering column types
ALTER TABLE proveedores DISABLE ROW LEVEL SECURITY;
ALTER TABLE ordenes_compra DISABLE ROW LEVEL SECURITY;
ALTER TABLE orden_compra_items DISABLE ROW LEVEL SECURITY;
ALTER TABLE inventario_fisico DISABLE ROW LEVEL SECURITY;
ALTER TABLE inventario_fisico_detalle DISABLE ROW LEVEL SECURITY;
ALTER TABLE montura_proveedor DISABLE ROW LEVEL SECURITY;
ALTER TABLE categorias_montura DISABLE ROW LEVEL SECURITY;

-- Alter column types and drop defaults
ALTER TABLE proveedores ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE proveedores ALTER COLUMN id DROP DEFAULT;
ALTER TABLE ordenes_compra ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE ordenes_compra ALTER COLUMN id DROP DEFAULT;
ALTER TABLE orden_compra_items ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE orden_compra_items ALTER COLUMN id DROP DEFAULT;
ALTER TABLE inventario_fisico ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE inventario_fisico ALTER COLUMN id DROP DEFAULT;
ALTER TABLE inventario_fisico_detalle ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE inventario_fisico_detalle ALTER COLUMN id DROP DEFAULT;
ALTER TABLE montura_proveedor ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE montura_proveedor ALTER COLUMN id DROP DEFAULT;
ALTER TABLE categorias_montura ALTER COLUMN id SET DATA TYPE TEXT;
ALTER TABLE categorias_montura ALTER COLUMN id DROP DEFAULT;

-- Recreate RLS policies
CREATE POLICY proveedores_select ON proveedores FOR SELECT USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY proveedores_insert ON proveedores FOR INSERT WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY proveedores_update ON proveedores FOR UPDATE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY proveedores_delete ON proveedores FOR DELETE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));

CREATE POLICY ordenes_compra_select ON ordenes_compra FOR SELECT USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY ordenes_compra_insert ON ordenes_compra FOR INSERT WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY ordenes_compra_update ON ordenes_compra FOR UPDATE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY ordenes_compra_delete ON ordenes_compra FOR DELETE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));

CREATE POLICY oci_select ON orden_compra_items FOR SELECT USING (orden_id IN (SELECT ordenes_compra.id FROM ordenes_compra WHERE ordenes_compra.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY oci_insert ON orden_compra_items FOR INSERT WITH CHECK (orden_id IN (SELECT ordenes_compra.id FROM ordenes_compra WHERE ordenes_compra.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY oci_update ON orden_compra_items FOR UPDATE USING (orden_id IN (SELECT ordenes_compra.id FROM ordenes_compra WHERE ordenes_compra.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY oci_delete ON orden_compra_items FOR DELETE USING (orden_id IN (SELECT ordenes_compra.id FROM ordenes_compra WHERE ordenes_compra.optica_id = (auth.jwt() ->> 'optica_id'::text)));

CREATE POLICY if_select ON inventario_fisico FOR SELECT USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY if_insert ON inventario_fisico FOR INSERT WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY if_update ON inventario_fisico FOR UPDATE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY if_delete ON inventario_fisico FOR DELETE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));

CREATE POLICY ifd_select ON inventario_fisico_detalle FOR SELECT USING (inventario_id IN (SELECT inventario_fisico.id FROM inventario_fisico WHERE inventario_fisico.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY ifd_insert ON inventario_fisico_detalle FOR INSERT WITH CHECK (inventario_id IN (SELECT inventario_fisico.id FROM inventario_fisico WHERE inventario_fisico.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY ifd_update ON inventario_fisico_detalle FOR UPDATE USING (inventario_id IN (SELECT inventario_fisico.id FROM inventario_fisico WHERE inventario_fisico.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY ifd_delete ON inventario_fisico_detalle FOR DELETE USING (inventario_id IN (SELECT inventario_fisico.id FROM inventario_fisico WHERE inventario_fisico.optica_id = (auth.jwt() ->> 'optica_id'::text)));

CREATE POLICY montura_proveedor_select ON montura_proveedor FOR SELECT USING (montura_id IN (SELECT monturas.id FROM monturas WHERE monturas.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY montura_proveedor_insert ON montura_proveedor FOR INSERT WITH CHECK (montura_id IN (SELECT monturas.id FROM monturas WHERE monturas.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY montura_proveedor_update ON montura_proveedor FOR UPDATE USING (montura_id IN (SELECT monturas.id FROM monturas WHERE monturas.optica_id = (auth.jwt() ->> 'optica_id'::text)));
CREATE POLICY montura_proveedor_delete ON montura_proveedor FOR DELETE USING (montura_id IN (SELECT monturas.id FROM monturas WHERE monturas.optica_id = (auth.jwt() ->> 'optica_id'::text)));

CREATE POLICY categorias_montura_select ON categorias_montura FOR SELECT USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY categorias_montura_insert ON categorias_montura FOR INSERT WITH CHECK (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY categorias_montura_update ON categorias_montura FOR UPDATE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));
CREATE POLICY categorias_montura_delete ON categorias_montura FOR DELETE USING (optica_id = (auth.jwt() ->> 'optica_id'::text));;
