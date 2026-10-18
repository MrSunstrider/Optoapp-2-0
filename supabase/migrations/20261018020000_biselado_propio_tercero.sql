-- OT bevel origin (Propio | Tercero), optica defaults, and linked gasto for outsourced bevel COGS.
ALTER TABLE public.dispensacion_items
    ADD COLUMN IF NOT EXISTS origen_biselado TEXT NOT NULL DEFAULT 'Tercero';

ALTER TABLE public.configuracion_financiera
    ADD COLUMN IF NOT EXISTS origen_biselado_default TEXT NOT NULL DEFAULT 'Tercero';

ALTER TABLE public.configuracion_financiera
    ADD COLUMN IF NOT EXISTS costo_biselado_propio NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE public.gastos_operativos
    ADD COLUMN IF NOT EXISTS origen_dispensacion_item_id TEXT;

CREATE INDEX IF NOT EXISTS gastos_operativos_origen_dispensacion_item_id_idx
    ON public.gastos_operativos (origen_dispensacion_item_id);
