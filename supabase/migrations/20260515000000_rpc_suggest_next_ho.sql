-- M9: Replace client-side full table scan for next HO suggestion.
-- Computes the next available HO number server-side.

CREATE OR REPLACE FUNCTION public.suggest_next_ho(p_optica_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_year text := EXTRACT(YEAR FROM NOW())::text;
    v_max_num int;
BEGIN
    SELECT MAX(
        NULLIF(regexp_replace(
            historia_optometrica,
            '^HO-' || v_year || '-(\d+)$',
            '\1'
        ), historia_optometrica)::int
    ) INTO v_max_num
    FROM pacientes
    WHERE optica_id = p_optica_id
      AND historia_optometrica ~* ('^HO-' || v_year || '-\d+$');

    v_max_num := COALESCE(v_max_num, 0);
    RETURN jsonb_build_object(
        'next_ho', 'HO-' || v_year || '-' || LPAD((v_max_num + 1)::text, 4, '0')
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.suggest_next_ho(uuid) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.suggest_next_ho(uuid) TO authenticated, service_role;
