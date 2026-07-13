CREATE TABLE public.regalos_dispensacion (
    id TEXT PRIMARY KEY,
    dispensacion_id TEXT NOT NULL REFERENCES public.dispensaciones(id) ON DELETE CASCADE,
    producto_id TEXT NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 1,
    costo_unitario REAL NOT NULL,
    descripcion TEXT NOT NULL,
    motivo TEXT NOT NULL DEFAULT '',
    optica_id TEXT NOT NULL REFERENCES public.opticas(id)
);
CREATE INDEX idx_regalos_disp ON public.regalos_dispensacion(dispensacion_id);
ALTER TABLE public.regalos_dispensacion ENABLE ROW LEVEL SECURITY;
CREATE POLICY "optica regalos" ON public.regalos_dispensacion FOR ALL USING (optica_id = current_setting('app.current_optica_id')::text);;
