-- Fix: Drop and recreate paciente_eliminaciones_restantes_hoy to remove duplicate
DROP FUNCTION IF EXISTS public.paciente_eliminaciones_restantes_hoy(uuid);

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

-- Revoke EXECUTE from anon for all SECURITY DEFINER functions
-- that should not be callable without authentication
REVOKE EXECUTE ON FUNCTION public.enforce_optica_limit_for_creator() FROM anon;
REVOKE EXECUTE ON FUNCTION public.paciente_eliminaciones_restantes_hoy(uuid) FROM anon;
REVOKE EXECUTE ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) FROM anon;;
