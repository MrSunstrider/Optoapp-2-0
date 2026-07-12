-- T1: CASE mapping correctness (unit test)
-- Validates that rpc_analisis_mensual maps (tipo_lente, material_lente) to
-- the correct categoria_producto_id via inline CASE expression.
--
-- Expected (after fix): Monofocal/Resina → lente_monofocal with ventas > 0
--                       Progresivo/*    → lente_progresivo with ventas > 0
--                       Bifocal/*       → lente_bifocal with ventas > 0
-- Before fix: all ventas = 0 (empty margen_por_categoria table)

DO $$
DECLARE
    v_result jsonb;
    v_cat jsonb;
    v_optica_id TEXT := '25af5a92-4a2d-4e7a-957f-61bec87a07d8';
    v_mes DATE := '2026-07-01';
    v_ventas NUMERIC;
BEGIN
    -- Call the function
    SELECT public.rpc_analisis_mensual(v_optica_id, v_mes) INTO v_result;

    -- Extract margen_por_categoria array
    v_result := v_result -> 'margen_por_categoria';

    -- Assert: exactly 9 categories exist
    ASSERT jsonb_array_length(v_result) = 9,
        'Expected 9 categories in margen_por_categoria, got ' || jsonb_array_length(v_result);

    -- Assert: Lentes Monofocales (Monofocal/Resina = S/1,310) has non-zero ventas
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Monofocales';
    ASSERT v_cat IS NOT NULL, 'Lentes Monofocales not found in margen_por_categoria';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas > 0,
        'Expected Lentes Monofocales ventas > 0, got ' || v_ventas;

    -- Assert: Lentes Progresivos (Progresivo/Resina = S/600) has non-zero ventas
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Progresivos';
    ASSERT v_cat IS NOT NULL, 'Lentes Progresivos not found in margen_por_categoria';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas > 0,
        'Expected Lentes Progresivos ventas > 0, got ' || v_ventas;

    -- Assert: Lentes Bifocales (Bifocal/Resina = S/580) has non-zero ventas
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Bifocales';
    ASSERT v_cat IS NOT NULL, 'Lentes Bifocales not found in margen_por_categoria';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas > 0,
        'Expected Lentes Bifocales ventas > 0, got ' || v_ventas;

    -- Assert: Servicios Extra has non-zero ventas (S/428 from servicios_extra UNION ALL)
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Servicios Extra';
    ASSERT v_cat IS NOT NULL, 'Servicios Extra not found in margen_por_categoria';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas > 0,
        'Expected Servicios Extra ventas > 0, got ' || v_ventas;

    RAISE NOTICE 'T1 PASS: CASE mapping produces non-zero ventas for know categories';
END;
$$;
