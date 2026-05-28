-- Fix RLS for dispensacion_items: table was created without any policies.
-- All other business tables use app_private.has_optica_role for consistent RLS.

ALTER TABLE public.dispensacion_items ENABLE ROW LEVEL SECURITY;

-- SELECT
DROP POLICY IF EXISTS "dispensacion_items_select" ON public.dispensacion_items;
CREATE POLICY "dispensacion_items_select" ON public.dispensacion_items FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- INSERT (used by upsert batch sync)
DROP POLICY IF EXISTS "dispensacion_items_insert" ON public.dispensacion_items;
CREATE POLICY "dispensacion_items_insert" ON public.dispensacion_items FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- UPDATE (used by upsert batch sync)
DROP POLICY IF EXISTS "dispensacion_items_update" ON public.dispensacion_items;
CREATE POLICY "dispensacion_items_update" ON public.dispensacion_items FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- DELETE
DROP POLICY IF EXISTS "dispensacion_items_delete" ON public.dispensacion_items;
CREATE POLICY "dispensacion_items_delete" ON public.dispensacion_items FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
