-- ============================================================================
-- Migration: Security Hardening — RLS + Grants
-- Date: 2026-07-14
--
-- Addresses 3 findings from the comprehensive DB audit:
--   #11: 7 tables use jwt->>'optica_id' instead of app_private.is_optica_member()
--   #14: app_releases has full CRUD + TRUNCATE grants to anon (unauthenticated)
--   #17: categorias_producto SELECT is public (deferred — shared reference table)
--
-- Pattern: DROP old JWT-based policies, CREATE new app_private-based policies.
-- All policies use the same naming convention and role-based access as core tables.
-- ============================================================================

-- ============================================================================
-- BLOCK 1: Direct optica_id tables (Pattern A)
-- categorias_montura, inventario_fisico, ordenes_compra, proveedores
-- ============================================================================

-- 1a. categorias_montura
DROP POLICY IF EXISTS categorias_montura_select ON public.categorias_montura;
DROP POLICY IF EXISTS categorias_montura_insert ON public.categorias_montura;
DROP POLICY IF EXISTS categorias_montura_update ON public.categorias_montura;
DROP POLICY IF EXISTS categorias_montura_delete ON public.categorias_montura;

CREATE POLICY categorias_montura_select ON public.categorias_montura
  FOR SELECT TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY categorias_montura_insert ON public.categorias_montura
  FOR INSERT TO public
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY categorias_montura_update ON public.categorias_montura
  FOR UPDATE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id))
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY categorias_montura_delete ON public.categorias_montura
  FOR DELETE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

-- 1b. inventario_fisico
DROP POLICY IF EXISTS if_select ON public.inventario_fisico;
DROP POLICY IF EXISTS if_insert ON public.inventario_fisico;
DROP POLICY IF EXISTS if_update ON public.inventario_fisico;
DROP POLICY IF EXISTS if_delete ON public.inventario_fisico;

CREATE POLICY inventario_fisico_select ON public.inventario_fisico
  FOR SELECT TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY inventario_fisico_insert ON public.inventario_fisico
  FOR INSERT TO public
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY inventario_fisico_update ON public.inventario_fisico
  FOR UPDATE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id))
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY inventario_fisico_delete ON public.inventario_fisico
  FOR DELETE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

-- 1c. ordenes_compra
DROP POLICY IF EXISTS ordenes_compra_select ON public.ordenes_compra;
DROP POLICY IF EXISTS ordenes_compra_insert ON public.ordenes_compra;
DROP POLICY IF EXISTS ordenes_compra_update ON public.ordenes_compra;
DROP POLICY IF EXISTS ordenes_compra_delete ON public.ordenes_compra;

CREATE POLICY ordenes_compra_select ON public.ordenes_compra
  FOR SELECT TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY ordenes_compra_insert ON public.ordenes_compra
  FOR INSERT TO public
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY ordenes_compra_update ON public.ordenes_compra
  FOR UPDATE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id))
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY ordenes_compra_delete ON public.ordenes_compra
  FOR DELETE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

-- 1d. proveedores
DROP POLICY IF EXISTS proveedores_select ON public.proveedores;
DROP POLICY IF EXISTS proveedores_insert ON public.proveedores;
DROP POLICY IF EXISTS proveedores_update ON public.proveedores;
DROP POLICY IF EXISTS proveedores_delete ON public.proveedores;

CREATE POLICY proveedores_select ON public.proveedores
  FOR SELECT TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY proveedores_insert ON public.proveedores
  FOR INSERT TO public
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY proveedores_update ON public.proveedores
  FOR UPDATE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id))
  WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY proveedores_delete ON public.proveedores
  FOR DELETE TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

-- ============================================================================
-- BLOCK 2: Indirect optica_id tables (Pattern B)
-- inventario_fisico_detalle, montura_proveedor, orden_compra_items
-- These use subqueries on parent tables. Update subquery to use
-- app_private.is_optica_member() on parent.optica_id.
-- ============================================================================

-- 2a. inventario_fisico_detalle
DROP POLICY IF EXISTS ifd_select ON public.inventario_fisico_detalle;
DROP POLICY IF EXISTS ifd_insert ON public.inventario_fisico_detalle;
DROP POLICY IF EXISTS ifd_update ON public.inventario_fisico_detalle;
DROP POLICY IF EXISTS ifd_delete ON public.inventario_fisico_detalle;

CREATE POLICY inventario_fisico_detalle_select ON public.inventario_fisico_detalle
  FOR SELECT TO public
  USING (inventario_id IN (
    SELECT id FROM public.inventario_fisico
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY inventario_fisico_detalle_insert ON public.inventario_fisico_detalle
  FOR INSERT TO public
  WITH CHECK (inventario_id IN (
    SELECT id FROM public.inventario_fisico
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY inventario_fisico_detalle_update ON public.inventario_fisico_detalle
  FOR UPDATE TO public
  USING (inventario_id IN (
    SELECT id FROM public.inventario_fisico
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ))
  WITH CHECK (inventario_id IN (
    SELECT id FROM public.inventario_fisico
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY inventario_fisico_detalle_delete ON public.inventario_fisico_detalle
  FOR DELETE TO public
  USING (inventario_id IN (
    SELECT id FROM public.inventario_fisico
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

-- 2b. montura_proveedor
DROP POLICY IF EXISTS montura_proveedor_select ON public.montura_proveedor;
DROP POLICY IF EXISTS montura_proveedor_insert ON public.montura_proveedor;
DROP POLICY IF EXISTS montura_proveedor_update ON public.montura_proveedor;
DROP POLICY IF EXISTS montura_proveedor_delete ON public.montura_proveedor;

CREATE POLICY montura_proveedor_select ON public.montura_proveedor
  FOR SELECT TO public
  USING (montura_id IN (
    SELECT id FROM public.monturas
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY montura_proveedor_insert ON public.montura_proveedor
  FOR INSERT TO public
  WITH CHECK (montura_id IN (
    SELECT id FROM public.monturas
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY montura_proveedor_update ON public.montura_proveedor
  FOR UPDATE TO public
  USING (montura_id IN (
    SELECT id FROM public.monturas
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ))
  WITH CHECK (montura_id IN (
    SELECT id FROM public.monturas
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY montura_proveedor_delete ON public.montura_proveedor
  FOR DELETE TO public
  USING (montura_id IN (
    SELECT id FROM public.monturas
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

-- 2c. orden_compra_items
DROP POLICY IF EXISTS oci_select ON public.orden_compra_items;
DROP POLICY IF EXISTS oci_insert ON public.orden_compra_items;
DROP POLICY IF EXISTS oci_update ON public.orden_compra_items;
DROP POLICY IF EXISTS oci_delete ON public.orden_compra_items;

CREATE POLICY orden_compra_items_select ON public.orden_compra_items
  FOR SELECT TO public
  USING (orden_id IN (
    SELECT id FROM public.ordenes_compra
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY orden_compra_items_insert ON public.orden_compra_items
  FOR INSERT TO public
  WITH CHECK (orden_id IN (
    SELECT id FROM public.ordenes_compra
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY orden_compra_items_update ON public.orden_compra_items
  FOR UPDATE TO public
  USING (orden_id IN (
    SELECT id FROM public.ordenes_compra
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ))
  WITH CHECK (orden_id IN (
    SELECT id FROM public.ordenes_compra
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

CREATE POLICY orden_compra_items_delete ON public.orden_compra_items
  FOR DELETE TO public
  USING (orden_id IN (
    SELECT id FROM public.ordenes_compra
    WHERE app_private.is_optica_member(auth.uid(), optica_id)
  ));

-- ============================================================================
-- BLOCK 3: Revoke dangerous anon grants on app_releases (#14)
-- Edge function track-release uses service_role, not anon.
-- Public only needs SELECT to check for app updates.
-- ============================================================================

REVOKE ALL ON public.app_releases FROM anon;
GRANT SELECT ON public.app_releases TO anon;

-- ============================================================================
-- BLOCK 4: Verify no JWT-based policies remain on these 7 tables
-- ============================================================================

DO $$
DECLARE
    v_remaining TEXT[];
BEGIN
    SELECT array_agg(c.relname || '.' || pol.polname) INTO v_remaining
    FROM pg_policy pol
    JOIN pg_class c ON c.oid = pol.polrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname IN (
        'categorias_montura','inventario_fisico','inventario_fisico_detalle',
        'montura_proveedor','ordenes_compra','orden_compra_items','proveedores'
      )
      AND (
        pg_get_expr(pol.polqual, pol.polrelid)::text LIKE '%jwt%optica_id%'
        OR pg_get_expr(pol.polwithcheck, pol.polrelid)::text LIKE '%jwt%optica_id%'
      );

    IF v_remaining IS NOT NULL AND array_length(v_remaining, 1) > 0 THEN
        RAISE WARNING 'JWT-based policies still remain: %', array_to_string(v_remaining, ', ');
    ELSE
        RAISE NOTICE 'All 7 tables migrated to app_private.is_optica_member(). Zero JWT-based policies remain.';
    END IF;
END;
$$;
