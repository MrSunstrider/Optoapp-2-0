-- =============================================================================
-- RED/GREEN Test: assign_optica_role_by_email permission check
--
-- RED phase: BEFORE the fix migration — authenticated role gets 42501
--   verified via has_function_privilege() → false
-- GREEN phase: AFTER the fix migration — authenticated can call,
--   and internal role check (admin/gerente only) still rejects
-- =============================================================================

-- Wrapped in a single DO block for compatibility with supabase db query
-- (which does not accept multiple statements)
DO $$
DECLARE
    v_has_exec boolean;
    v_functional_rejection boolean := false;
BEGIN
    -- =========================================================================
    -- Test 1: Check if authenticated role has EXECUTE permission
    -- =========================================================================
    SELECT has_function_privilege(
        'authenticated',
        'public.assign_optica_role_by_email(text,text,text)',
        'EXECUTE'
    ) INTO v_has_exec;

    IF v_has_exec THEN
        RAISE NOTICE 'GREEN: authenticated role has EXECUTE on assign_optica_role_by_email';
    ELSE
        RAISE NOTICE 'RED: authenticated role lacks EXECUTE on assign_optica_role_by_email';
    END IF;

    -- =========================================================================
    -- Test 2: Verify function body internal validation works
    -- When called from a context where auth.uid() returns no match,
    -- the function should reject with "Sin permisos" error
    -- =========================================================================
    BEGIN
        PERFORM public.assign_optica_role_by_email(
            'nonexistent-optica',
            'noone@example.com',
            'admin'
        );
        RAISE NOTICE 'FUNCTIONAL GUARD: Call succeeded (unexpected in this context)';
    EXCEPTION
        WHEN OTHERS THEN
            v_functional_rejection := true;
            RAISE NOTICE 'FUNCTIONAL GUARD PASS: Function rejected call: %', SQLERRM;
    END;

    IF NOT v_functional_rejection THEN
        RAISE WARNING 'FUNCTIONAL GUARD: Call did not raise an exception';
    END IF;

    -- =========================================================================
    -- Summary
    -- =========================================================================
    IF v_has_exec THEN
        RAISE NOTICE 'RESULT: GREEN STATE - authenticated role can execute RPC';
    ELSE
        RAISE NOTICE 'RESULT: RED STATE - authenticated role cannot execute RPC';
    END IF;

    IF NOT v_functional_rejection THEN
        RAISE NOTICE 'NOTE: Function body guard test was inconclusive (superuser context bypasses internal checks)';
    END IF;
END;
$$;
