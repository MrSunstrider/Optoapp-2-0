-- ============================================================================
-- Trigger: maintain monto_pagado on dispensaciones and a_cuenta on servicios_extra
-- from the pagos table. Fires on INSERT/UPDATE/DELETE of pagos.
-- Excludes Anulación rows from the sum (they are audit-only, tracked in caja).
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_disp_id TEXT;
    v_serv_id TEXT;
BEGIN
    v_disp_id := COALESCE(NEW.dispensacion_id, OLD.dispensacion_id);
    v_serv_id := COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id);

    IF v_disp_id IS NOT NULL THEN
        UPDATE public.dispensaciones
        SET monto_pagado = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE dispensacion_id = v_disp_id
              AND tipo IS DISTINCT FROM 'Anulación'
        )
        WHERE id = v_disp_id;
    END IF;

    IF v_serv_id IS NOT NULL THEN
        UPDATE public.servicios_extra
        SET a_cuenta = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE servicio_extra_id = v_serv_id
              AND tipo IS DISTINCT FROM 'Anulación'
        )
        WHERE id = v_serv_id;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_pagos_maintain_monto_pagado ON public.pagos;
CREATE TRIGGER trg_pagos_maintain_monto_pagado
    AFTER INSERT OR UPDATE OR DELETE ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_update_monto_pagado();
