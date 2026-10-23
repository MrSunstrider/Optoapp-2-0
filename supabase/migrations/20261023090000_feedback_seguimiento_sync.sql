-- Seguimiento sync: align remote feedback_recomendaciones with Room sync-ready shape + LWW.
-- Local PK is text stableId (optica|periodo|recomendacionId); remote was UUID-only with SELECT+INSERT.

ALTER TABLE public.feedback_recomendaciones
    ALTER COLUMN id TYPE TEXT USING id::text;

ALTER TABLE public.feedback_recomendaciones
    ALTER COLUMN id DROP DEFAULT;

ALTER TABLE public.feedback_recomendaciones
    ADD COLUMN IF NOT EXISTS estado TEXT NOT NULL DEFAULT '';

ALTER TABLE public.feedback_recomendaciones
    ADD COLUMN IF NOT EXISTS periodo TEXT NOT NULL DEFAULT '';

ALTER TABLE public.feedback_recomendaciones
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX IF NOT EXISTS feedback_recomendaciones_optica_periodo_rec_uidx
    ON public.feedback_recomendaciones (optica_id, periodo, recomendacion_id)
    WHERE periodo <> '';

DROP POLICY IF EXISTS feedback_recomendaciones_update ON public.feedback_recomendaciones;
CREATE POLICY feedback_recomendaciones_update ON public.feedback_recomendaciones
    FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));

DROP POLICY IF EXISTS feedback_recomendaciones_delete ON public.feedback_recomendaciones;
CREATE POLICY feedback_recomendaciones_delete ON public.feedback_recomendaciones
    FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));

COMMENT ON TABLE public.feedback_recomendaciones IS
  'Plan-de-acción seguimiento; id is client stableId; LWW via updated_at.';
