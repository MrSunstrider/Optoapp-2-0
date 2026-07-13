-- Fix: preserve client-provided updated_at instead of always overriding with server clock.
-- Root cause of false sync conflicts: the old trigger set updated_at = NOW() unconditionally
-- on every upsert, making remote always newer than local after upload.
CREATE OR REPLACE FUNCTION public.set_updated_audit_fields()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.updated_at IS NULL THEN
    NEW.updated_at := timezone('utc', now());
  END IF;
  RETURN NEW;
END;
$$;;
