CREATE OR REPLACE FUNCTION paciente_eliminaciones_restantes_hoy(p_optica_id text)
RETURNS integer
LANGUAGE sql
STABLE
AS $$
  SELECT GREATEST(0, 10 - COUNT(*))::integer
  FROM pacientes_delete_audit
  WHERE optica_id = p_optica_id
    AND deleted_at >= CURRENT_DATE
    AND deleted_at < CURRENT_DATE + INTERVAL '1 day';
$$;;
