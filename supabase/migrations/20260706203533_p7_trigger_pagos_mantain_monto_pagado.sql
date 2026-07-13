-- Trigger function: update monto_pagado on dispensaciones from pagos
CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_disp_id TEXT;
    v_serv_id TEXT;
BEGIN
    -- Determine affected dispensacion or servicio_extra
    v_disp_id := COALESCE(NEW.dispensacion_id, OLD.dispensacion_id);
    v_serv_id := COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id);

    -- Update dispensacion monto_pagado
    IF v_disp_id IS NOT NULL THEN
        UPDATE public.dispensaciones
        SET monto_pagado = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE dispensacion_id = v_disp_id
        )
        WHERE id = v_disp_id;
    END IF;

    -- Update servicio_extra a_cuenta
    IF v_serv_id IS NOT NULL THEN
        UPDATE public.servicios_extra
        SET a_cuenta = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE servicio_extra_id = v_serv_id
        )
        WHERE id = v_serv_id;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

-- Attach trigger to pagos for INSERT, UPDATE, DELETE
DROP TRIGGER IF EXISTS trg_pagos_maintain_monto_pagado ON public.pagos;
CREATE TRIGGER trg_pagos_maintain_monto_pagado
    AFTER INSERT OR UPDATE OR DELETE ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_update_monto_pagado();;
