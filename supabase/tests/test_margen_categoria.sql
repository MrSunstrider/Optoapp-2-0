-- T2: Integration test — margen_por_categoria returns real revenue values
-- Validates that specific categories have correct expected totals.
--
-- Known data for optica '25af5a92-4a2d-4e7a-957f-61bec87a07d8' July 2026:
--   Monofocal/Resina: 7 disp → S/1,310 (maps to lente_monofocal)
--   Bifocal/Resina:    4 disp → S/  580 (maps to lente_bifocal)
--   Progresivo/Resina: 1 disp → S/  600 (maps to lente_progresivo)
--   Servicios extra:   10 rows → S/  428 (maps to servicio_extra)
-- Before fix: all ventas = 0

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
    v_result := v_result -> 'margen_por_categoria';

    -- Assert: Lentes Monofocales = 1310 (Monofocal/Resina: 310+400+260+40+90+120+90)
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Monofocales';
    ASSERT v_cat IS NOT NULL, 'Lentes Monofocales not found';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas = 1310.00,
        'Expected Lentes Monofocales ventas = 1310, got ' || v_ventas;

    -- Assert: Lentes Bifocales = 580 (Bifocal/Resina: 180+60+200+140)
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Bifocales';
    ASSERT v_cat IS NOT NULL, 'Lentes Bifocales not found';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas = 580.00,
        'Expected Lentes Bifocales ventas = 580, got ' || v_ventas;

    -- Assert: Lentes Progresivos = 600 (Progresivo/Resina: 600)
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Lentes Progresivos';
    ASSERT v_cat IS NOT NULL, 'Lentes Progresivos not found';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas = 600.00,
        'Expected Lentes Progresivos ventas = 600, got ' || v_ventas;

    -- Assert: Servicios Extra = 428
    SELECT value INTO v_cat
    FROM jsonb_array_elements(v_result)
    WHERE value->>'categoria' = 'Servicios Extra';
    ASSERT v_cat IS NOT NULL, 'Servicios Extra not found';
    v_ventas := (v_cat->>'ventas')::NUMERIC;
    ASSERT v_ventas = 428.00,
        'Expected Servicios Extra ventas = 428, got ' || v_ventas;

    RAISE NOTICE 'T2 PASS: margen_por_categoria returns correct revenue totals';
END;
$$;
