-- Grant EXECUTE on check_rate_limit to anon role
-- The web app calls this function during login (before authentication),
-- using the anon key. Without this grant, the login endpoint returns 500.
GRANT EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) TO anon;
