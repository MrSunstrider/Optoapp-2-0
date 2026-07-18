-- Fix 1.8: opticas_update_member allows ANY member to UPDATE opticas (including 'invitado').
-- Restrict to admin/gerente only. Also add defense-in-depth for SELECT.

DROP POLICY IF EXISTS "opticas_update_member" ON public.opticas;
DROP POLICY IF EXISTS "opticas_select_member" ON public.opticas;

CREATE POLICY "opticas_select_member" ON public.opticas
FOR SELECT USING (
    app_private.is_optica_member(auth.uid(), id)
);

CREATE POLICY "opticas_update_member" ON public.opticas
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), id, ARRAY['admin', 'gerente'])
);
