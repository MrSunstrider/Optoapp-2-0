-- T3: Integration test — stock_estancado shows real ultima_venta dates
-- Validates that sold monturas show computed dias_sin_venta and never-sold
-- monturas show dias_sin_venta = 999, ultima_venta = null.
--
-- Before fix: all ultima_venta = hardcoded NULL, stock_estancado empty
--             (low-stock filter excludes all monturas with stock>stock_minimo)
-- After fix:  9 monturas with stock>0, 4 sold via SALIDA_VENTA/disp, 5 never-sold

DO $$
DECLARE
    v_result jsonb;
    v_item jsonb;
    v_optica_id TEXT := '25af5a92-4a2d-4e7a-957f-61bec87a07d8';
    v_mes DATE := '2026-07-01';
    v_item_count INTEGER;
    v_sold_count INTEGER := 0;
    v_unsold_count INTEGER := 0;
BEGIN
    -- Call the function
    SELECT public.rpc_analisis_mensual(v_optica_id, v_mes) INTO v_result;
    v_result := v_result -> 'stock_estancado';

    v_item_count := jsonb_array_length(v_result);

    -- Assert: stock_estancado is not empty (9 active monturas with stock > 0)
    ASSERT v_item_count = 9,
        'Expected 9 monturas in stock_estancado, got ' || v_item_count;

    -- Verify each item has the required fields
    FOR v_item IN SELECT * FROM jsonb_array_elements(v_result)
    LOOP
        ASSERT v_item ? 'montura_id', 'Item missing montura_id';
        ASSERT v_item ? 'sku', 'Item missing sku';
        ASSERT v_item ? 'modelo', 'Item missing modelo';
        ASSERT v_item ? 'costo', 'Item missing costo';
        ASSERT v_item ? 'stock_actual', 'Item missing stock_actual';
        ASSERT v_item ? 'ultima_venta', 'Item missing ultima_venta';
        ASSERT v_item ? 'dias_sin_venta', 'Item missing dias_sin_venta';

        IF (v_item->>'ultima_venta') IS NOT NULL AND (v_item->>'ultima_venta') != '' THEN
            v_sold_count := v_sold_count + 1;
            -- Assert: sold items have a computed dias_sin_venta (not 999)
            ASSERT (v_item->>'dias_sin_venta')::INTEGER < 999,
                'Sold montura ' || (v_item->>'montura_id') || ' should have dias_sin_venta < 999';
        ELSE
            v_unsold_count := v_unsold_count + 1;
            -- Assert: unsold items have dias_sin_venta = 999
            ASSERT (v_item->>'dias_sin_venta')::INTEGER = 999,
                'Unsold montura ' || (v_item->>'montura_id') || ' should have dias_sin_venta = 999, got ' || (v_item->>'dias_sin_venta');
        END IF;
    END LOOP;

    -- Assert: at least some monturas are sold (has SALIDA_VENTA or dispensacion montura link)
    ASSERT v_sold_count >= 4,
        'Expected at least 4 sold monturas, found ' || v_sold_count;

    -- Assert: at least some monturas are never-sold
    ASSERT v_unsold_count >= 1,
        'Expected at least 1 never-sold montura, found ' || v_unsold_count;

    RAISE NOTICE 'T3 PASS: stock_estancado has % items (% sold, % unsold)',
        v_item_count, v_sold_count, v_unsold_count;
END;
$$;
