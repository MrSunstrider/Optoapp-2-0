-- Fix C-3 (GGA): rpc_cierre_caja_resumen includes Anulación payments
-- Adds AND tipo IS DISTINCT FROM 'Anulación' to exclude voided payments
-- from cierre_caja totals, consistent with rpc_deudores and rpc_analisis_mensual

CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(
    p_optica_id TEXT,
    p_from DATE,
    p_to DATE
) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_efectivo numeric;
    v_movil_trans numeric;
    v_tarjeta numeric;
    v_total numeric;
BEGIN
    -- Fix 2.13: Verify caller belongs to the optica
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    -- Fix 2.29: Verify caller has BI-level role
    IF NOT app_private.has_optica_role(auth.uid(), p_optica_id, ARRAY['admin', 'gerente']) THEN
        RAISE EXCEPTION 'BI access requires admin or gerente role';
    END IF;

    SELECT
        COALESCE(SUM(CASE WHEN metodo_pago = 'Efectivo' THEN monto ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN metodo_pago IN ('Transferencia', 'Yape', 'Plin', 'Móvil') THEN monto ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN metodo_pago = 'Tarjeta' THEN monto ELSE 0 END), 0),
        COALESCE(SUM(monto), 0)
    INTO
        v_efectivo,
        v_movil_trans,
        v_tarjeta,
        v_total
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to
      AND tipo IS DISTINCT FROM 'Anulación';

    RETURN jsonb_build_object(
        'efectivo', v_efectivo,
        'movil_trans', v_movil_trans,
        'tarjeta', v_tarjeta,
        'total', v_total
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) TO authenticated, service_role;
