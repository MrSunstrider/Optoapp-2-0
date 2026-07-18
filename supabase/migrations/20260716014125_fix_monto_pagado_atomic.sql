-- ============================================================================
-- Fix 1.7: replace SELECT SUM with atomic increment/decrement in
-- trg_pagos_update_monto_pagado to eliminate lost-update race conditions
-- under concurrent INSERT/UPDATE/DELETE on pagos.
--
-- Problem: the previous version used SELECT SUM(monto) subqueries without
-- FOR UPDATE, so two simultaneous pagos against the same dispensacion could
-- both read the same stale monto_pagado and produce a wrong total.
--
-- Fix: use column += delta for each row operation. PostgreSQL guarantees
-- that SET col = col + N is an atomic read-modify-write under MVCC.
-- ============================================================================

CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    v_disp_id TEXT;
    v_serv_id TEXT;
    v_delta NUMERIC;
BEGIN
    v_disp_id := COALESCE(NEW.dispensacion_id, OLD.dispensacion_id);
    v_serv_id := COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id);

    IF TG_OP = 'INSERT' THEN
        -- Only count payments that are not Anulación (audit-only rows)
        IF NEW.tipo IS DISTINCT FROM 'Anulación' THEN
            IF v_disp_id IS NOT NULL THEN
                UPDATE public.dispensaciones
                SET monto_pagado = monto_pagado + NEW.monto
                WHERE id = v_disp_id;
            END IF;
            IF v_serv_id IS NOT NULL THEN
                UPDATE public.servicios_extra
                SET a_cuenta = a_cuenta + NEW.monto
                WHERE id = v_serv_id;
            END IF;
        END IF;

    ELSIF TG_OP = 'DELETE' THEN
        -- Reverse the contribution if the deleted row was counted
        IF OLD.tipo IS DISTINCT FROM 'Anulación' THEN
            IF v_disp_id IS NOT NULL THEN
                UPDATE public.dispensaciones
                SET monto_pagado = monto_pagado - OLD.monto
                WHERE id = v_disp_id;
            END IF;
            IF v_serv_id IS NOT NULL THEN
                UPDATE public.servicios_extra
                SET a_cuenta = a_cuenta - OLD.monto
                WHERE id = v_serv_id;
            END IF;
        END IF;

    ELSIF TG_OP = 'UPDATE' THEN
        -- Net delta accounts for all four tipo transitions:
        --   non-Anulación → non-Anulación: +NEW.monto - OLD.monto
        --   non-Anulación → Anulación:     -OLD.monto        (NEW is excluded)
        --   Anulación     → non-Anulación: +NEW.monto        (OLD was excluded)
        --   Anulación     → Anulación:      0                (both excluded)
        v_delta := CASE
            WHEN NEW.tipo IS DISTINCT FROM 'Anulación' THEN NEW.monto ELSE 0
        END - CASE
            WHEN OLD.tipo IS DISTINCT FROM 'Anulación' THEN OLD.monto ELSE 0
        END;

        IF v_delta <> 0 THEN
            IF v_disp_id IS NOT NULL THEN
                UPDATE public.dispensaciones
                SET monto_pagado = monto_pagado + v_delta
                WHERE id = v_disp_id;
            END IF;
            IF v_serv_id IS NOT NULL THEN
                UPDATE public.servicios_extra
                SET a_cuenta = a_cuenta + v_delta
                WHERE id = v_serv_id;
            END IF;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

-- Recreate the trigger (idempotent — always safe to re-run)
DROP TRIGGER IF EXISTS trg_pagos_maintain_monto_pagado ON public.pagos;
CREATE TRIGGER trg_pagos_maintain_monto_pagado
    AFTER INSERT OR UPDATE OR DELETE ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_update_monto_pagado();

-- Revoke EXECUTE from public/anon (trigger-only function, never callable via REST)
REVOKE EXECUTE ON FUNCTION public.trg_pagos_update_monto_pagado() FROM PUBLIC, anon, authenticated;
