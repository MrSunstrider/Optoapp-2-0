-- Fix C-1 (GGA): check_rate_limit was dropped by cleanup migration
-- Restores the function verbatim from 20260628000000 and grants EXECUTE to anon
-- The web companion (optoapp-web) calls this function during unauthenticated login

CREATE OR REPLACE FUNCTION public.check_rate_limit(
  p_limit_key text,
  p_window_ms integer,
  p_max_attempts integer
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_row record;
    v_now timestamptz := now();
    v_window_ago timestamptz;
BEGIN
    IF p_window_ms < 1 THEN
        RAISE EXCEPTION 'p_window_ms must be positive, got %', p_window_ms;
    END IF;
    IF p_max_attempts < 1 THEN
        RAISE EXCEPTION 'p_max_attempts must be at least 1, got %', p_max_attempts;
    END IF;
    v_window_ago := v_now - (p_window_ms || ' milliseconds')::interval;
    SELECT * INTO v_row
    FROM pin_attempts
    WHERE limit_key = p_limit_key
      AND window_start > v_window_ago
    ORDER BY window_start DESC
    LIMIT 1;

    IF NOT FOUND THEN
        INSERT INTO pin_attempts (limit_key, attempts, window_start)
        VALUES (p_limit_key, 1, v_now);
        RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - 1);
    END IF;

    IF v_row.attempts >= p_max_attempts THEN
        RETURN jsonb_build_object('allowed', false, 'remaining', 0);
    END IF;

    UPDATE pin_attempts SET attempts = v_row.attempts + 1 WHERE id = v_row.id;
    RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - v_row.attempts - 1);
END;
$function$;

GRANT EXECUTE ON FUNCTION public.check_rate_limit(TEXT, INT, INT) TO anon;
