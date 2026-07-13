-- Trigger function: auto-set venta_id from dispensacion_id or servicio_extra_id
CREATE OR REPLACE FUNCTION public.trg_pagos_set_venta_id()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.venta_id IS NULL THEN
        IF NEW.dispensacion_id IS NOT NULL THEN
            NEW.venta_id := 'v_disp_' || NEW.dispensacion_id;
        ELSIF NEW.servicio_extra_id IS NOT NULL THEN
            NEW.venta_id := 'v_serv_' || NEW.servicio_extra_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- Attach trigger to pagos table
DROP TRIGGER IF EXISTS trg_pagos_before_insert_venta_id ON public.pagos;
CREATE TRIGGER trg_pagos_before_insert_venta_id
    BEFORE INSERT ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_set_venta_id();;
