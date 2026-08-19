-- RED/GREEN: create_optica_for_current_user bootstrap contract (WU1).
-- Catalog-only DO-block: compatible with supabase db query (single statement).
-- RED: current RETURNS void + COALESCE(NULLIF(p_optica_id + INSERT policy.
-- GREEN: RETURNS text, ignore client id, GET DIAGNOSTICS, no INSERT policy.
DO $$
DECLARE
    v_rettype oid;
    v_def text;
    v_policy_exists boolean;
    v_trigger_exists boolean;
    v_auth_exec boolean;
    v_anon_exec boolean;
BEGIN
    SELECT p.prorettype
    INTO v_rettype
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public'
      AND p.proname = 'create_optica_for_current_user'
      AND pg_get_function_identity_arguments(p.oid) LIKE 'p_optica_id text%';

    IF v_rettype IS DISTINCT FROM 'text'::regtype THEN
        RAISE EXCEPTION 'create_optica_for_current_user prorettype is not text';
    END IF;

    v_def := pg_get_functiondef(
        'public.create_optica_for_current_user(text,text,text,text,text,text)'::regprocedure
    );

    IF position('COALESCE(NULLIF(p_optica_id' in v_def) > 0 THEN
        RAISE EXCEPTION 'function still uses COALESCE(NULLIF(p_optica_id';
    END IF;

    IF position('gen_random_uuid' in v_def) = 0 THEN
        RAISE EXCEPTION 'function does not use gen_random_uuid';
    END IF;

    IF position('GET DIAGNOSTICS' in v_def) = 0 OR position('ROW_COUNT' in v_def) = 0 THEN
        RAISE EXCEPTION 'function missing GET DIAGNOSTICS / ROW_COUNT';
    END IF;

    IF position('max_opticas' in v_def) > 0 THEN
        RAISE EXCEPTION 'function encodes max_opticas in body';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'opticas'
          AND policyname = 'opticas_insert_authenticated'
    ) INTO v_policy_exists;

    IF v_policy_exists THEN
        RAISE EXCEPTION 'policy opticas_insert_authenticated still exists';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM pg_trigger t
        JOIN pg_class c ON c.oid = t.tgrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = 'opticas'
          AND t.tgname = 'trg_opticas_limit_guard'
          AND NOT t.tgisinternal
    ) INTO v_trigger_exists;

    IF NOT v_trigger_exists THEN
        RAISE EXCEPTION 'trg_opticas_limit_guard missing';
    END IF;

    SELECT has_function_privilege(
        'authenticated',
        'public.create_optica_for_current_user(text,text,text,text,text,text)',
        'EXECUTE'
    ) INTO v_auth_exec;
    SELECT has_function_privilege(
        'anon',
        'public.create_optica_for_current_user(text,text,text,text,text,text)',
        'EXECUTE'
    ) INTO v_anon_exec;

    IF v_auth_exec IS NOT TRUE THEN
        RAISE EXCEPTION 'authenticated lacks EXECUTE';
    END IF;
    IF v_anon_exec IS NOT FALSE THEN
        RAISE EXCEPTION 'anon has EXECUTE';
    END IF;

    RAISE NOTICE 'GREEN: create_optica_for_current_user bootstrap contract holds';
END;
$$;
