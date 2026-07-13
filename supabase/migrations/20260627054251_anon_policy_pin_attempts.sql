-- Allow anon role to read/write pin_attempts for rate limiting during login
CREATE POLICY anon_rate_limit_access ON pin_attempts
  FOR ALL
  TO anon
  USING (true)
  WITH CHECK (true);;
