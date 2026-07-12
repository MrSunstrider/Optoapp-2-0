-- T4: Integration test — JSON structure validation
-- Validates that margen_por_categoria JSON array:
--   1. Has exactly 9 elements (one per categorias_producto)
--   2. All elements have the required fields: categoria, ventas, costos, margen_pct
--   3. All categories appear in correct order (by cat.orden)
--   4. Montura categories (montura_premium/estandar/economica) exist with ventas=0
--   5. At least some categories have ventas > 0 (verifying the fix works)
--
-- Before fix: orden is undefined (no ORDER BY), all ventas = 0
-- After fix:  ORDER BY cat.orden, real ventas for lens/service categories

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_optica_id TEXT := '25af5a92-4a2d-4e7a-957f-61bec87a07d8';
    v_mes DATE := '2026-07-01';
    v_expected_order TEXT[] := ARRAY[
        'Lentes Progresivos',
        'Lentes Monofocales',
        'Lentes Bifocales',
        'Monturas Premium',
        'Monturas Estandar',
        'Monturas Economicas',
        'Servicios Extra',
        'Garantias Extendidas',
        'Otros Lentes'
    ];
    v_idx INTEGER;
    v_cat_name TEXT;
    v_has_nonzero_ventas BOOLEAN := false;
    v_total_ventas NUMERIC := 0;
BEGIN
    -- Call the function
    SELECT public.rpc_analisis_mensual(v_optica_id, v_mes) INTO v_result;
    v_result := v_result -> 'margen_por_categoria';

    -- 1. Assert exactly 9 categories
    ASSERT jsonb_array_length(v_result) = 9,
        'Expected 9 categories, got ' || jsonb_array_length(v_result);

    -- 2 & 3. Validate each element structure and order
    FOR v_idx IN 0..jsonb_array_length(v_result)-1
    LOOP
        v_item := v_result->v_idx;

        -- Check required fields exist
        ASSERT v_item ? 'categoria',
            'Item ' || (v_idx+1) || ' missing categoria field';
        ASSERT v_item ? 'ventas',
            'Item ' || (v_idx+1) || ' missing ventas field';
        ASSERT v_item ? 'costos',
            'Item ' || (v_idx+1) || ' missing costos field';
        ASSERT v_item ? 'margen_pct',
            'Item ' || (v_idx+1) || ' missing margen_pct field';

        -- Check ventas type and non-negative
        ASSERT jsonb_typeof(v_item->'ventas') = 'number',
            'Item ' || (v_idx+1) || ' ventas is not a number';
        ASSERT (v_item->>'ventas')::NUMERIC >= 0,
            'Item ' || (v_idx+1) || ' ventas is negative: ' || (v_item->>'ventas');

        v_total_ventas := v_total_ventas + (v_item->>'ventas')::NUMERIC;
        IF (v_item->>'ventas')::NUMERIC > 0 THEN
            v_has_nonzero_ventas := true;
        END IF;

        -- Check order matches categorias_producto.orden
        v_cat_name := v_item->>'categoria';
        ASSERT v_cat_name = v_expected_order[v_idx+1],
            'Order mismatch at position ' || (v_idx+1) || ': expected "' ||
            v_expected_order[v_idx+1] || '", got "' || v_cat_name || '"';

        -- 4. Assert montura categories have ventas = 0 (no dispensaciones map here)
        IF v_cat_name IN ('Monturas Premium', 'Monturas Estandar', 'Monturas Economicas') THEN
            ASSERT (v_item->>'ventas')::NUMERIC = 0,
                v_cat_name || ' should have ventas = 0, got ' || (v_item->>'ventas');
        END IF;
    END LOOP;

    -- 5. Assert at least some categories have ventas > 0 (proves fix)
    ASSERT v_has_nonzero_ventas,
        'No categories have ventas > 0 — fix not applied. Total ventas: ' || v_total_ventas;

    RAISE NOTICE 'T4 PASS: JSON structure valid — % categories in correct order, total ventas = %',
        jsonb_array_length(v_result), v_total_ventas;
END;
$$;
