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
    v_ventas_emitidas  numeric;
    v_saldo_pendiente  numeric;
    v_total_movimientos bigint;
    v_ticket_promedio  numeric;
BEGIN
    SELECT COALESCE(SUM(monto), 0)
    INTO v_ingresos_cobrados
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    SELECT
        COALESCE(SUM(monto_total), 0),
        COUNT(*)
    INTO
        v_ventas_emitidas,
        v_total_movimientos
    FROM public.ventas
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to
      AND estado IS DISTINCT FROM 'Anulado';

    v_saldo_pendiente := v_ventas_emitidas - v_ingresos_cobrados;
    v_ticket_promedio := CASE
        WHEN v_total_movimientos > 0 THEN v_ventas_emitidas / v_total_movimientos
        ELSE 0
    END;

    RETURN jsonb_build_object(
        'ingresos_cobrados',   v_ingresos_cobrados,
        'ventas_emitidas',     v_ventas_emitidas,
        'saldo_pendiente',     v_saldo_pendiente,
        'total_movimientos',   v_total_movimientos,
        'ticket_promedio',     v_ticket_promedio,
        'fecha_inicio',        p_from::text,
        'fecha_fin_exclusiva', p_to::text
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_resumen_financiero(text, date, date) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_resumen_financiero(text, date, date) TO authenticated, service_role;;
