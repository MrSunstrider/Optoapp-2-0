-- T6: Edge case — optica with no sales data for the month
-- Verifies that margen_por_categoria still returns 9 categories
-- with ventas=0 when there are no dispensaciones or servicios_extra
-- for the given month. The LEFT JOIN + COALESCE guarantees this,
-- so this test documents and locks the behavior.
--
-- Uses a future month (2027-01-01) where no data exists.

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_optica_id TEXT := '25af5a92-4a2d-4e7a-957f-61bec87a07d8';
    v_mes DATE := '2027-01-01';
    v_idx INTEGER;
    v_any_nonzero BOOLEAN := false;
BEGIN
    SELECT public.rpc_analisis_mensual(v_optica_id, v_mes) INTO v_result;
    v_result := v_result -> 'margen_por_categoria';

    ASSERT jsonb_array_length(v_result) = 9,
        'Expected 9 categories for empty month, got ' || jsonb_array_length(v_result);

    FOR v_idx IN 0..jsonb_array_length(v_result)-1
    LOOP
        v_item := v_result->v_idx;

        ASSERT (v_item->>'ventas')::NUMERIC = 0,
            'Category ' || (v_item->>'categoria') ||
            ' has ventas=' || (v_item->>'ventas') ||
            ' but month ' || v_mes || ' should have no sales';

        IF (v_item->>'ventas')::NUMERIC > 0 THEN
            v_any_nonzero := true;
        END IF;
    END LOOP;

    ASSERT NOT v_any_nonzero,
        'At least one category has non-zero ventas in empty month';
END;
$$;
