-- Parent cache (dispensaciones.monto_pagado, servicios_extra.a_cuenta) must equal
-- SUM(pago_effect) after every pago write. Incremental += doubled the cache when
-- the client already uploaded the parent with that sum (OT 4582 dual-writer).

CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        IF OLD.dispensacion_id IS NOT NULL THEN
            UPDATE public.dispensaciones
            SET monto_pagado = COALESCE((
                SELECT SUM(public.pago_effect(p.tipo, p.monto))
                FROM public.pagos p
                WHERE p.dispensacion_id = OLD.dispensacion_id
            ), 0)
            WHERE id = OLD.dispensacion_id;
        END IF;
        IF OLD.servicio_extra_id IS NOT NULL THEN
            UPDATE public.servicios_extra
            SET a_cuenta = COALESCE((
                SELECT SUM(public.pago_effect(p.tipo, p.monto))
                FROM public.pagos p
                WHERE p.servicio_extra_id = OLD.servicio_extra_id
            ), 0)
            WHERE id = OLD.servicio_extra_id;
        END IF;
    END IF;

    IF TG_OP IN ('UPDATE', 'INSERT') THEN
        IF NEW.dispensacion_id IS NOT NULL THEN
            UPDATE public.dispensaciones
            SET monto_pagado = COALESCE((
                SELECT SUM(public.pago_effect(p.tipo, p.monto))
                FROM public.pagos p
                WHERE p.dispensacion_id = NEW.dispensacion_id
            ), 0)
            WHERE id = NEW.dispensacion_id;
        END IF;
        IF NEW.servicio_extra_id IS NOT NULL THEN
            UPDATE public.servicios_extra
            SET a_cuenta = COALESCE((
                SELECT SUM(public.pago_effect(p.tipo, p.monto))
                FROM public.pagos p
                WHERE p.servicio_extra_id = NEW.servicio_extra_id
            ), 0)
            WHERE id = NEW.servicio_extra_id;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

REVOKE EXECUTE ON FUNCTION public.trg_pagos_update_monto_pagado()
    FROM PUBLIC, anon, authenticated;

-- Repair caches already doubled by += before this function existed.
UPDATE public.dispensaciones d
SET monto_pagado = COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.dispensacion_id = d.id
), 0)
WHERE abs(d.monto_pagado - COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.dispensacion_id = d.id
), 0)) > 0.005;

UPDATE public.servicios_extra se
SET a_cuenta = COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.servicio_extra_id = se.id
), 0)
WHERE abs(se.a_cuenta - COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.servicio_extra_id = se.id
), 0)) > 0.005;
