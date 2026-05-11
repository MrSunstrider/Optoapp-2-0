-- Single round-trip: avoids the read-check-update race window that existed in client code.
-- Two concurrent requests can no longer both read stock=2 and both write stock=1.
ALTER TABLE public.monturas
    ADD CONSTRAINT monturas_stock_actual_non_negative CHECK (stock_actual >= 0);

CREATE OR REPLACE FUNCTION public.rpc_adjust_montura_stock(
    p_montura_id text,
    p_optica_id text,
    p_delta integer,
    p_reference_id text,
    p_note text,
    p_tipo text,
    p_fecha text
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_new_stock integer;
    v_old_stock integer;
BEGIN
    UPDATE public.monturas
    SET stock_actual = stock_actual + p_delta
    WHERE id = p_montura_id
      AND optica_id = p_optica_id
    RETURNING stock_actual, stock_actual - p_delta
    INTO v_new_stock, v_old_stock;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_found');
    END IF;

    -- If the CHECK constraint didn't catch it (unlikely but safe),
    -- revert and signal insufficient.
    IF v_new_stock < 0 THEN
        UPDATE public.monturas
        SET stock_actual = v_old_stock
        WHERE id = p_montura_id
          AND optica_id = p_optica_id;
        RETURN jsonb_build_object('ok', false, 'error', 'insufficient');
    END IF;

    INSERT INTO public.montura_movimientos (
        id, montura_id, fecha, tipo, cantidad,
        stock_previo, stock_nuevo, referencia_id, nota, optica_id
    ) VALUES (
        gen_random_uuid()::text,
        p_montura_id,
        CAST(p_fecha AS date),
        p_tipo,
        ABS(p_delta),
        v_old_stock,
        v_new_stock,
        p_reference_id,
        p_note,
        p_optica_id
    );

    RETURN jsonb_build_object('ok', true, 'new_stock', v_new_stock);
END;
$$;

REVOKE EXECUTE ON FUNCTION public.rpc_adjust_montura_stock(text, text, integer, text, text, text, text) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.rpc_adjust_montura_stock(text, text, integer, text, text, text, text) TO authenticated, service_role;
