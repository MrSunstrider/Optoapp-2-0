-- ============================================================================
-- Trigger: auto-set venta_id on pagos INSERT from dispensacion_id / servicio_extra_id
-- Ensures every pago has venta_id regardless of origin (Android, web, direct SQL).
-- ============================================================================

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

DROP TRIGGER IF EXISTS trg_pagos_before_insert_venta_id ON public.pagos;
CREATE TRIGGER trg_pagos_before_insert_venta_id
    BEFORE INSERT ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_set_venta_id();
