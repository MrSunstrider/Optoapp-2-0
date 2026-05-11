-- M1: Replace 9 client-side count queries with a single RPC.
-- Returns all entity counts for the sync dashboard in one round-trip.

CREATE OR REPLACE FUNCTION public.sync_snapshot(p_optica_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_pacientes_total int;
    v_disp_total int;
    v_disp_pendientes int;
    v_serv_total int;
    v_serv_pendientes int;
    v_eval_total int;
    v_eval_pendientes int;
    v_inv_total int;
    v_inv_critico int;
BEGIN
    -- Pacientes: total count
    SELECT COUNT(*) INTO v_pacientes_total
    FROM pacientes WHERE optica_id = p_optica_id;

    -- Dispensaciones: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado_entrega = 'Pendiente')
    INTO v_disp_total, v_disp_pendientes
    FROM dispensaciones WHERE optica_id = p_optica_id;

    -- Servicios extra: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado = 'Pendiente')
    INTO v_serv_total, v_serv_pendientes
    FROM servicios_extra WHERE optica_id = p_optica_id;

    -- Evaluaciones: total (con próxima cita) + pendientes (no atendidas ni canceladas)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (
            WHERE cita_estado IS NULL OR (cita_estado <> 'atendida' AND cita_estado <> 'cancelada')
        )
    INTO v_eval_total, v_eval_pendientes
    FROM evaluaciones
    WHERE optica_id = p_optica_id
      AND proxima_cita IS NOT NULL;

    -- Inventario (monturas activas): total + críticas (stock <= 2)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (WHERE stock_actual <= 2)
    INTO v_inv_total, v_inv_critico
    FROM monturas
    WHERE optica_id = p_optica_id
      AND activo = true;

    RETURN jsonb_build_object(
        'pacientes', jsonb_build_object('total', v_pacientes_total, 'pending', 0),
        'dispensaciones', jsonb_build_object('total', v_disp_total, 'pending', v_disp_pendientes),
        'servicios_extra', jsonb_build_object('total', v_serv_total, 'pending', v_serv_pendientes),
        'evaluaciones', jsonb_build_object('total', v_eval_total, 'pending', v_eval_pendientes),
        'inventario', jsonb_build_object('total', v_inv_total, 'pending', v_inv_critico)
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.sync_snapshot(uuid) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.sync_snapshot(uuid) TO authenticated, service_role;
