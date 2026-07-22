-- Fix C-2 (GGA): Foundational DO block destroys ALL policies on 5 core tables on rerun
-- Sentinel guard checks for has_optica_role-based policies before allowing
-- the destructive drop+recreate. If restrictive policies already exist, skip.

DO $$
BEGIN
    -- Sentinel: if has_optica_role-based policies already exist on ALL 5 core tables,
    -- skip the foundational policy recreation to avoid downgrading role matrix
    IF EXISTS (
        SELECT 1 FROM pg_policies p
        JOIN pg_class c ON c.oid = p.polrelid
        WHERE p.schemaname = 'public'
          AND c.relname IN ('pacientes','evaluaciones','dispensaciones','servicios_extra','pagos')
          AND pg_get_expr(p.polqual, p.polrelid)::text LIKE '%has_optica_role%'
    ) THEN
        RAISE NOTICE 'Restrictive policies exist on core tables — skipping foundational policy recreation';
    ELSE
        -- Re-create basic member policies for all 5 core tables
        -- (idempotent: DROP IF EXISTS + CREATE POLICY per policy)

        -- PACIENTES
        DROP POLICY IF EXISTS "pacientes_select" ON public.pacientes;
        CREATE POLICY "pacientes_select" ON public.pacientes FOR SELECT
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pacientes_insert" ON public.pacientes;
        CREATE POLICY "pacientes_insert" ON public.pacientes FOR INSERT
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pacientes_update" ON public.pacientes;
        CREATE POLICY "pacientes_update" ON public.pacientes FOR UPDATE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pacientes_delete" ON public.pacientes;
        CREATE POLICY "pacientes_delete" ON public.pacientes FOR DELETE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

        -- EVALUACIONES
        DROP POLICY IF EXISTS "evaluaciones_select" ON public.evaluaciones;
        CREATE POLICY "evaluaciones_select" ON public.evaluaciones FOR SELECT
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "evaluaciones_insert" ON public.evaluaciones;
        CREATE POLICY "evaluaciones_insert" ON public.evaluaciones FOR INSERT
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "evaluaciones_update" ON public.evaluaciones;
        CREATE POLICY "evaluaciones_update" ON public.evaluaciones FOR UPDATE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "evaluaciones_delete" ON public.evaluaciones;
        CREATE POLICY "evaluaciones_delete" ON public.evaluaciones FOR DELETE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

        -- DISPENSACIONES
        DROP POLICY IF EXISTS "dispensaciones_select" ON public.dispensaciones;
        CREATE POLICY "dispensaciones_select" ON public.dispensaciones FOR SELECT
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "dispensaciones_insert" ON public.dispensaciones;
        CREATE POLICY "dispensaciones_insert" ON public.dispensaciones FOR INSERT
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "dispensaciones_update" ON public.dispensaciones;
        CREATE POLICY "dispensaciones_update" ON public.dispensaciones FOR UPDATE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "dispensaciones_delete" ON public.dispensaciones;
        CREATE POLICY "dispensaciones_delete" ON public.dispensaciones FOR DELETE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

        -- SERVICIOS_EXTRA
        DROP POLICY IF EXISTS "servicios_extra_select" ON public.servicios_extra;
        CREATE POLICY "servicios_extra_select" ON public.servicios_extra FOR SELECT
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "servicios_extra_insert" ON public.servicios_extra;
        CREATE POLICY "servicios_extra_insert" ON public.servicios_extra FOR INSERT
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "servicios_extra_update" ON public.servicios_extra;
        CREATE POLICY "servicios_extra_update" ON public.servicios_extra FOR UPDATE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "servicios_extra_delete" ON public.servicios_extra;
        CREATE POLICY "servicios_extra_delete" ON public.servicios_extra FOR DELETE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));

        -- PAGOS
        DROP POLICY IF EXISTS "pagos_select" ON public.pagos;
        CREATE POLICY "pagos_select" ON public.pagos FOR SELECT
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pagos_insert" ON public.pagos;
        CREATE POLICY "pagos_insert" ON public.pagos FOR INSERT
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pagos_update" ON public.pagos;
        CREATE POLICY "pagos_update" ON public.pagos FOR UPDATE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()))
            WITH CHECK (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
        DROP POLICY IF EXISTS "pagos_delete" ON public.pagos;
        CREATE POLICY "pagos_delete" ON public.pagos FOR DELETE
            USING (optica_id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid()));
    END IF;
END;
$$;
