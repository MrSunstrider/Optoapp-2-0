-- Persist rate-limit attempts across serverless cold starts.

CREATE TABLE IF NOT EXISTS public.pin_attempts (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    limit_key text NOT NULL,
    attempts integer NOT NULL DEFAULT 1,
    window_start timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pin_attempts_key_window
    ON public.pin_attempts (limit_key, window_start);

ALTER TABLE public.pin_attempts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "service_role_full_access" ON public.pin_attempts;
CREATE POLICY "service_role_full_access" ON public.pin_attempts
    FOR ALL TO service_role USING (true) WITH CHECK (true);

-- RPC: single round-trip, atomic rate-limit check
CREATE OR REPLACE FUNCTION public.check_rate_limit(
    p_limit_key text,
    p_window_ms integer,
    p_max_attempts integer
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
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
$$;

REVOKE EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO authenticated, service_role;

-- pg_cron cleanup (hourly, removes rows older than 24h)
DO $cron$
BEGIN
  IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'cleanup-pin-attempts') THEN
    PERFORM cron.unschedule('cleanup-pin-attempts');
  END IF;
  PERFORM cron.schedule(
    'cleanup-pin-attempts',
    '0 * * * *',
    $sql$ DELETE FROM public.pin_attempts WHERE window_start < now() - interval '24 hours' $sql$
  );
END;
$cron$;
