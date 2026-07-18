DO $$
DECLARE
    tbl text;
BEGIN
    FOR tbl IN 
        SELECT unnest(ARRAY['proveedores','ordenes_compra','inventario_fisico',
                             'montura_proveedor','categorias_montura',
                             'inventario_fisico_detalle','orden_compra_items'])
    LOOP
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS text_id text', tbl);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS idx_%I_text_id ON public.%I(text_id)', tbl, tbl);
        EXECUTE format('
            CREATE OR REPLACE FUNCTION public.sync_%I_text_id() RETURNS trigger AS $fn$
            BEGIN
                IF NEW.text_id IS NULL OR NEW.text_id = '''' THEN
                    NEW.text_id := NEW.id::text;
                END IF;
                RETURN NEW;
            END;
            $fn$ LANGUAGE plpgsql;
        ', tbl);
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_sync_%I_text_id ON public.%I;
            CREATE TRIGGER trg_sync_%I_text_id BEFORE INSERT OR UPDATE ON public.%I
            FOR EACH ROW EXECUTE FUNCTION public.sync_%I_text_id();
        ', tbl, tbl, tbl, tbl, tbl);
        EXECUTE format('UPDATE public.%I SET text_id = id::text WHERE text_id IS NULL', tbl);
    END LOOP;
END;
$$;
