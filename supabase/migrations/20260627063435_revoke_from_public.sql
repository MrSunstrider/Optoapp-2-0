-- Revoke EXECUTE from PUBLIC (anon inherits from PUBLIC)
REVOKE EXECUTE ON FUNCTION public.enforce_optica_limit_for_creator() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.paciente_eliminaciones_restantes_hoy(uuid) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) FROM PUBLIC;;
