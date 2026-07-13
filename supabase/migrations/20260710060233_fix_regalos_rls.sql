DROP POLICY IF EXISTS "optica regalos" ON public.regalos_dispensacion;
CREATE POLICY "regalos_dispensacion_select" ON public.regalos_dispensacion FOR SELECT USING (app_private.is_optica_member(auth.uid(), optica_id));
CREATE POLICY "regalos_dispensacion_insert" ON public.regalos_dispensacion FOR INSERT WITH CHECK (app_private.is_optica_member(auth.uid(), optica_id));
CREATE POLICY "regalos_dispensacion_update" ON public.regalos_dispensacion FOR UPDATE USING (app_private.is_optica_member(auth.uid(), optica_id));
CREATE POLICY "regalos_dispensacion_delete" ON public.regalos_dispensacion FOR DELETE USING (app_private.is_optica_member(auth.uid(), optica_id));;
