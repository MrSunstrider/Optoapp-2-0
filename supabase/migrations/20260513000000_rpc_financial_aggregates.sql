-- JS-side code was fetching entire tables to count/sum in application memory.
-- Each RPC does the aggregation in a single SQL pass so the DB can use indexes
-- instead of shipping thousands of rows to the client.
--
-- SECURITY INVOKER: runs with the caller's privileges so table-level RLS
-- (which scopes access by optica membership) is still enforced.  Without this,
-- an authenticated user could pass any p_optica_id and read financial data
-- from opticas they don't belong to.

CREATE OR REPLACE FUNCTION public.rpc_saldo_pendiente(p_optica_id text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_disp numeric;
    v_serv numeric;
BEGIN
    SELECT COALESCE(SUM(GREATEST(monto_total - COALESCE(monto_pagado, 0), 0)), 0)
    INTO v_disp
    FROM public.dispensaciones
    WHERE optica_id = p_optica_id;

    SELECT COALESCE(SUM(GREATEST(monto_total - COALESCE(a_cuenta, 0), 0)), 0)
    INTO v_serv
    FROM public.servicios_extra
    WHERE optica_id = p_optica_id;

    RETURN jsonb_build_object(
        'saldo_dispensaciones', v_disp,
        'saldo_servicios', v_serv,
        'saldo_total', v_disp + v_serv
    );
END;
$$;
REVOKE EXECUTE ON FUNCTION public.rpc_saldo_pendiente(text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_saldo_pendiente(text) TO authenticated, service_role;
CREATE OR REPLACE FUNCTION public.rpc_count_pendientes(p_optica_id text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_entregas integer;
    v_servicios integer;
BEGIN
    SELECT COUNT(*)
    INTO v_entregas
    FROM public.dispensaciones
    WHERE optica_id = p_optica_id
      AND estado_entrega = 'Pendiente'
      AND fecha < CURRENT_DATE;

    SELECT COUNT(*)
    INTO v_servicios
    FROM public.servicios_extra
    WHERE optica_id = p_optica_id
      AND (
          LOWER(COALESCE(estado, '')) = 'pendiente'
          OR (COALESCE(monto_total, 0) - COALESCE(a_cuenta, 0)) > 0.005
      );

    RETURN jsonb_build_object(
        'entregas_pendientes', v_entregas,
        'servicios_pendientes', v_servicios
    );
END;
$$;
REVOKE EXECUTE ON FUNCTION public.rpc_count_pendientes(text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_count_pendientes(text) TO authenticated, service_role;
CREATE OR REPLACE FUNCTION public.rpc_pacientes_con_saldo(p_optica_id text)
RETURNS text[]
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_ids text[];
BEGIN
    SELECT ARRAY_AGG(DISTINCT paciente_id)
    INTO v_ids
    FROM (
        SELECT paciente_id
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND (monto_total - COALESCE(monto_pagado, 0)) > 0.005

        UNION

        SELECT paciente_id
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND (monto_total - COALESCE(a_cuenta, 0)) > 0.005
    ) sub;

    RETURN COALESCE(v_ids, ARRAY[]::text[]);
END;
$$;
REVOKE EXECUTE ON FUNCTION public.rpc_pacientes_con_saldo(text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_pacientes_con_saldo(text) TO authenticated, service_role;
CREATE OR REPLACE FUNCTION public.rpc_pacientes_con_entrega_pendiente(p_optica_id text)
RETURNS text[]
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_ids text[];
BEGIN
    SELECT ARRAY_AGG(DISTINCT paciente_id)
    INTO v_ids
    FROM (
        SELECT paciente_id
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND estado_entrega = 'Pendiente'

        UNION

        SELECT paciente_id
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND estado = 'Pendiente'
    ) sub;

    RETURN COALESCE(v_ids, ARRAY[]::text[]);
END;
$$;
REVOKE EXECUTE ON FUNCTION public.rpc_pacientes_con_entrega_pendiente(text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_pacientes_con_entrega_pendiente(text) TO authenticated, service_role;
