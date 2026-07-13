-- Fix 1: set_updated_audit_fields — add search_path
CREATE OR REPLACE FUNCTION public.set_updated_audit_fields()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
BEGIN
  NEW.updated_at = now();
  NEW.updated_by = coalesce(current_setting('request.jwt.claim.sub', true), 'system');
  RETURN NEW;
END;
$function$;

-- Fix 2: paciente_eliminaciones_restantes_hoy — add search_path
CREATE OR REPLACE FUNCTION public.paciente_eliminaciones_restantes_hoy(p_optica_id uuid)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
  v_limit integer;
  v_today_start timestamptz;
  v_count integer;
BEGIN
  v_today_start := date_trunc('day', now());
  
  SELECT max_pacientes_por_optica INTO v_limit
  FROM opticas WHERE id = p_optica_id;
  
  IF v_limit IS NULL THEN
    SELECT count(*) INTO v_count
    FROM pacientes_delete_audit
    WHERE optica_id = p_optica_id
      AND deleted_at >= v_today_start;
    RETURN GREATEST(5 - v_count, 0);
  END IF;
  
  SELECT count(*) INTO v_count
  FROM pacientes_delete_audit
  WHERE optica_id = p_optica_id
    AND deleted_at >= v_today_start;
  
  RETURN GREATEST(3 - v_count, 0);
END;
$function$;

-- Fix 3: Drop the anon_rate_limit_access policy (no longer needed with SECURITY DEFINER)
DROP POLICY IF EXISTS anon_rate_limit_access ON pin_attempts;

-- Fix 4: Revoke EXECUTE from anon for SECURITY DEFINER functions
-- (anon should not be able to call these directly)
REVOKE EXECUTE ON FUNCTION public.check_rate_limit(text, integer, integer) FROM anon;
REVOKE EXECUTE ON FUNCTION public.enforce_optica_limit_for_creator() FROM anon;;
