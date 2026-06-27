-- Fix check_rate_limit for web login (anon role)
-- 
-- Problem: The web app calls check_rate_limit during login (before auth),
-- using the anon key. PostgREST returns 401 because:
-- 1. The function was not SECURITY DEFINER, so RLS on pin_attempts blocked anon
-- 2. Even with EXECUTE grant and RLS policy, PostgREST may block anon RPC calls
--
-- Solution: Make the function SECURITY DEFINER so it runs as the owner (postgres)
-- and bypasses RLS. This is safe because the function only does rate-limiting
-- logic (insert/update on pin_attempts) — no user data access.

-- Revoke the anon EXECUTE grant (no longer needed with SECURITY DEFINER)
REVOKE EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) FROM anon;

-- Make the function SECURITY DEFINER
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
    v_window_ago timestamptz := v_now - (p_window_ms || ' milliseconds')::interval;
BEGIN
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

-- Remove the anon RLS policy (no longer needed with SECURITY DEFINER)
DROP POLICY IF EXISTS anon_rate_limit_access ON pin_attempts;
