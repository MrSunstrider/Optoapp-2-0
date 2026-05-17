-- Fix RLS policies for business tables to use app_private.has_optica_role for performance and consistency.
-- This resolves permission denied errors during batch inserts/updates.

-- PACIENTES
DROP POLICY IF EXISTS "pacientes_select" ON public.pacientes;
CREATE POLICY "pacientes_select" ON public.pacientes FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pacientes_insert" ON public.pacientes;
CREATE POLICY "pacientes_insert" ON public.pacientes FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pacientes_update" ON public.pacientes;
CREATE POLICY "pacientes_update" ON public.pacientes FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pacientes_delete" ON public.pacientes;
CREATE POLICY "pacientes_delete" ON public.pacientes FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- EVALUACIONES
DROP POLICY IF EXISTS "evaluaciones_select" ON public.evaluaciones;
CREATE POLICY "evaluaciones_select" ON public.evaluaciones FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "evaluaciones_insert" ON public.evaluaciones;
CREATE POLICY "evaluaciones_insert" ON public.evaluaciones FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "evaluaciones_update" ON public.evaluaciones;
CREATE POLICY "evaluaciones_update" ON public.evaluaciones FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "evaluaciones_delete" ON public.evaluaciones;
CREATE POLICY "evaluaciones_delete" ON public.evaluaciones FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- DISPENSACIONES
DROP POLICY IF EXISTS "dispensaciones_select" ON public.dispensaciones;
CREATE POLICY "dispensaciones_select" ON public.dispensaciones FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "dispensaciones_insert" ON public.dispensaciones;
CREATE POLICY "dispensaciones_insert" ON public.dispensaciones FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "dispensaciones_update" ON public.dispensaciones;
CREATE POLICY "dispensaciones_update" ON public.dispensaciones FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "dispensaciones_delete" ON public.dispensaciones;
CREATE POLICY "dispensaciones_delete" ON public.dispensaciones FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- SERVICIOS_EXTRA
DROP POLICY IF EXISTS "servicios_extra_select" ON public.servicios_extra;
CREATE POLICY "servicios_extra_select" ON public.servicios_extra FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "servicios_extra_insert" ON public.servicios_extra;
CREATE POLICY "servicios_extra_insert" ON public.servicios_extra FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "servicios_extra_update" ON public.servicios_extra;
CREATE POLICY "servicios_extra_update" ON public.servicios_extra FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "servicios_extra_delete" ON public.servicios_extra;
CREATE POLICY "servicios_extra_delete" ON public.servicios_extra FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- PAGOS
DROP POLICY IF EXISTS "pagos_select" ON public.pagos;
CREATE POLICY "pagos_select" ON public.pagos FOR SELECT
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pagos_insert" ON public.pagos;
CREATE POLICY "pagos_insert" ON public.pagos FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pagos_update" ON public.pagos;
CREATE POLICY "pagos_update" ON public.pagos FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

DROP POLICY IF EXISTS "pagos_delete" ON public.pagos;
CREATE POLICY "pagos_delete" ON public.pagos FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));