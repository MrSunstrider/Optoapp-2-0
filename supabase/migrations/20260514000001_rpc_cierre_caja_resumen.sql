-- Cierre de caja previously fetched ALL pagos rows for a day and iterated in JS
-- to sum by payment method. This RPC computes the payment method totals server-side.
-- The individual transaction rows are still fetched client-side for display,
-- but the heavy aggregation (sum by medio_pago) happens in the DB.

CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(
    p_optica_id text,
    p_from date,
    p_to date
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_efectivo numeric;
    v_movil_trans numeric;
    v_tarjeta numeric;
    v_total numeric;
BEGIN
    SELECT
        COALESCE(SUM(CASE
            WHEN LOWER(metodo_pago) LIKE '%efectivo%' OR metodo_pago IS NULL OR metodo_pago = ''
            THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN LOWER(metodo_pago) LIKE '%yape%'
              OR LOWER(metodo_pago) LIKE '%plin%'
              OR LOWER(metodo_pago) LIKE '%transfer%'
              OR LOWER(metodo_pago) LIKE '%movil%'
              OR LOWER(metodo_pago) LIKE '%móvil%'
            THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN LOWER(metodo_pago) LIKE '%tarjeta%'
            THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(monto), 0)
    INTO
        v_efectivo,
        v_movil_trans,
        v_tarjeta,
        v_total
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    RETURN jsonb_build_object(
        'efectivo', v_efectivo,
        'movil_trans', v_movil_trans,
        'tarjeta', v_tarjeta,
        'total', v_total
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(text, date, date) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_cierre_caja_resumen(text, date, date) TO authenticated, service_role;