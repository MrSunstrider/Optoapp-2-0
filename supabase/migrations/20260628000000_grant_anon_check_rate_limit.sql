-- Grant EXECUTE on check_rate_limit to anon role
-- The web app calls this function during login (before authentication),
-- using the anon key. Without this grant, the login endpoint returns 500.
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO anon;

-- Allow anon role to read/write pin_attempts for rate limiting during login
-- RLS was blocking anon even with EXECUTE grant (function is not SECURITY DEFINER)
CREATE POLICY anon_rate_limit_access ON pin_attempts
  FOR ALL
  TO anon
  USING (true)
  WITH CHECK (true);
