-- Fix: regalos_dispensacion RLS policies are too permissive.
-- Currently any member (including 'invitado') can INSERT/UPDATE/DELETE.
-- Now matches the pagos pattern: staff roles for INSERT/UPDATE, only admin/gerente for DELETE.

-- === DROP existing permissive policies ===
DROP POLICY IF EXISTS "regalos_dispensacion_select" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_insert" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_update" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_delete" ON public.regalos_dispensacion;

-- === SELECT: any member can view (keeps existing behavior) ===
CREATE POLICY "regalos_dispensacion_select" ON public.regalos_dispensacion
FOR SELECT USING (app_private.is_optica_member(auth.uid(), optica_id));

-- === INSERT: staff roles can add gifts ===
CREATE POLICY "regalos_dispensacion_insert" ON public.regalos_dispensacion
FOR INSERT WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
);

-- === UPDATE: staff roles can edit gifts ===
CREATE POLICY "regalos_dispensacion_update" ON public.regalos_dispensacion
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
);

-- === DELETE: only admin/gerente (matching pagos_delete pattern) ===
CREATE POLICY "regalos_dispensacion_delete" ON public.regalos_dispensacion
FOR DELETE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])
);
