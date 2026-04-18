-- P3-T3: Contacto de laboratorio en tabla opticas (fuente de verdad en Supabase).

ALTER TABLE public.opticas
    ADD COLUMN IF NOT EXISTS laboratorio_nombre TEXT NOT NULL DEFAULT '';

ALTER TABLE public.opticas
    ADD COLUMN IF NOT EXISTS laboratorio_contacto TEXT NOT NULL DEFAULT '';

COMMENT ON COLUMN public.opticas.laboratorio_nombre IS 'Nombre del laboratorio (ticket / WhatsApp)';
COMMENT ON COLUMN public.opticas.laboratorio_contacto IS 'WhatsApp o teléfono del laboratorio';

-- UPDATE requiere política explícita (antes solo existía SELECT en opticas).
DROP POLICY IF EXISTS "opticas_update_member" ON public.opticas;
CREATE POLICY "opticas_update_member" ON public.opticas
    FOR UPDATE USING (
        id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid())
    )
    WITH CHECK (
        id IN (SELECT uo.optica_id FROM public.usuario_optica uo WHERE uo.user_id = auth.uid())
    );
