-- Catalog contract for scoped user_profiles SELECT (A6).
DO $$
DECLARE
    v_qual text;
BEGIN
    SELECT qual INTO v_qual
    FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'user_profiles'
      AND policyname = 'user_profiles_select_access';

    IF v_qual IS NULL THEN
        RAISE EXCEPTION 'user_profiles_select_access missing';
    END IF;

    IF v_qual NOT LIKE '%usuario_optica%' THEN
        RAISE EXCEPTION 'policy must join usuario_optica: %', v_qual;
    END IF;

    IF v_qual NOT LIKE '%peer%' AND v_qual NOT LIKE '%optica_id%' THEN
        RAISE EXCEPTION 'policy must share optica_id: %', v_qual;
    END IF;

    RAISE NOTICE 'GREEN: user_profiles SELECT is scoped by shared optica';
END;
$$;
