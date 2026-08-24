-- Oleada 2: typed proveedores (monturas | laboratorio | tecnico).
ALTER TABLE public.proveedores
    ADD COLUMN IF NOT EXISTS tipo text NOT NULL DEFAULT 'monturas';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'proveedores_tipo_check'
    ) THEN
        ALTER TABLE public.proveedores
            ADD CONSTRAINT proveedores_tipo_check
            CHECK (tipo = ANY (ARRAY['monturas'::text, 'laboratorio'::text, 'tecnico'::text]));
    END IF;
END $$;
