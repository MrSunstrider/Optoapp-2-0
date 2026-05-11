-- Financial report previously fetched ALL rows from pagos, dispensaciones, servicios_extra
-- and reduced in JavaScript. A yearly report could transfer thousands of rows.
-- This single RPC computes all five metrics server-side in one call.

CREATE OR REPLACE FUNCTION public.rpc_resumen_financiero(
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
    v_ingresos_cobrados numeric;
    v_disp_ventas numeric;
    v_disp_saldo numeric;
    v_disp_count bigint;
    v_serv_ventas numeric;
    v_serv_saldo numeric;
    v_serv_count bigint;
    v_ventas_emitidas numeric;
    v_saldo_pendiente numeric;
    v_total_movimientos bigint;
    v_ticket_promedio numeric;
BEGIN
    -- Sum of all payments received in the period
    SELECT COALESCE(SUM(monto), 0)
    INTO v_ingresos_cobrados
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    -- Dispensaciones: ventas + saldo + count
    SELECT
        COALESCE(SUM(monto_total), 0),
        COALESCE(SUM(GREATEST(monto_total - COALESCE(monto_pagado, 0), 0)), 0),
        COUNT(*)
    INTO
        v_disp_ventas,
        v_disp_saldo,
        v_disp_count
    FROM public.dispensaciones
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    -- Servicios_extra: ventas + saldo + count
    SELECT
        COALESCE(SUM(monto_total), 0),
        COALESCE(SUM(GREATEST(monto_total - COALESCE(a_cuenta, 0), 0)), 0),
        COUNT(*)
    INTO
        v_serv_ventas,
        v_serv_saldo,
        v_serv_count
    FROM public.servicios_extra
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    v_ventas_emitidas := v_disp_ventas + v_serv_ventas;
    v_saldo_pendiente := v_disp_saldo + v_serv_saldo;
    v_total_movimientos := v_disp_count + v_serv_count;
    v_ticket_promedio := CASE
        WHEN v_total_movimientos > 0 THEN v_ventas_emitidas / v_total_movimientos
        ELSE 0
    END;

    RETURN jsonb_build_object(
        'ingresos_cobrados', v_ingresos_cobrados,
        'ventas_emitidas', v_ventas_emitidas,
        'saldo_pendiente', v_saldo_pendiente,
        'total_movimientos', v_total_movimientos,
        'ticket_promedio', v_ticket_promedio,
        'fecha_inicio', p_from::text,
        'fecha_fin_exclusiva', p_to::text
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_resumen_financiero(text, date, date) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_resumen_financiero(text, date, date) TO authenticated, service_role;