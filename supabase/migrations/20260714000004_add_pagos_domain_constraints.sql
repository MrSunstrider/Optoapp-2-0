-- ============================================================================
-- Migration: Add pagos domain CHECK constraints
--
-- Prevents silent data corruption in the financial pipeline by enforcing
-- domain-level validation on pagos.tipo and pagos.metodo_pago.
--
-- The CHECK constraints are added as NOT VALID to avoid blocking on
-- existing data. Violations are reported as WARNING before the DDL runs.
--
-- Re-runnable: each ALTER TABLE uses a DO-block guard for idempotency.
-- ============================================================================

-- ============================================================================
-- Step 1: Report existing violations BEFORE adding constraints
-- ============================================================================

DO $$
DECLARE
    v_invalid_tipo INTEGER;
    v_invalid_metodo INTEGER;
    v_tipo_examples TEXT;
    v_metodo_examples TEXT;
BEGIN
    SELECT COUNT(*) INTO v_invalid_tipo
    FROM public.pagos
    WHERE tipo IS NOT NULL
      AND tipo NOT IN ('Abono', 'Pago completo', 'Reembolso', 'Reverso', 'Anulación');

    SELECT COUNT(*) INTO v_invalid_metodo
    FROM public.pagos
    WHERE metodo_pago IS NOT NULL
      AND metodo_pago NOT IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'Móvil');

    IF v_invalid_tipo > 0 THEN
        SELECT string_agg(DISTINCT tipo, ', ') INTO v_tipo_examples
        FROM public.pagos
        WHERE tipo NOT IN ('Abono', 'Pago completo', 'Reembolso', 'Reverso', 'Anulación');
        RAISE WARNING 'Found % pagos with invalid tipo values (examples: %). Constraint will be NOT VALID.',
            v_invalid_tipo, COALESCE(v_tipo_examples, 'unknown');
    ELSE
        RAISE NOTICE 'All pagos.tipo values are valid — no violations found.';
    END IF;

    IF v_invalid_metodo > 0 THEN
        SELECT string_agg(DISTINCT metodo_pago, ', ') INTO v_metodo_examples
        FROM public.pagos
        WHERE metodo_pago NOT IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'Móvil');
        RAISE WARNING 'Found % pagos with invalid metodo_pago values (examples: %). Constraint will be NOT VALID.',
            v_invalid_metodo, COALESCE(v_metodo_examples, 'unknown');
    ELSE
        RAISE NOTICE 'All pagos.metodo_pago values are valid — no violations found.';
    END IF;
END;
$$;

-- ============================================================================
-- Step 2: Add CHECK constraint on pagos.tipo
--
-- Valid values match the trigger trg_pagos_update_monto_pagado pattern
-- (tipo IS DISTINCT FROM 'Anulación'). 'Abono' is the universal default.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_pagos_tipo'
          AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    ) THEN
        ALTER TABLE public.pagos
        ADD CONSTRAINT chk_pagos_tipo
        CHECK (tipo IN ('Abono', 'Pago completo', 'Reembolso', 'Reverso', 'Anulación'))
        NOT VALID;

        RAISE NOTICE 'Constraint chk_pagos_tipo added (NOT VALID).';
    ELSE
        RAISE NOTICE 'Constraint chk_pagos_tipo already exists — skipping.';
    END IF;
END;
$$;

-- ============================================================================
-- Step 3: Add CHECK constraint on pagos.metodo_pago
--
-- Valid values match rpc_cierre_caja_resumen aggregation categories.
-- 'Efectivo' is the universal default.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_pagos_metodo'
          AND connamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    ) THEN
        ALTER TABLE public.pagos
        ADD CONSTRAINT chk_pagos_metodo
        CHECK (metodo_pago IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'Móvil'))
        NOT VALID;

        RAISE NOTICE 'Constraint chk_pagos_metodo added (NOT VALID).';
    ELSE
        RAISE NOTICE 'Constraint chk_pagos_metodo already exists — skipping.';
    END IF;
END;
$$;
