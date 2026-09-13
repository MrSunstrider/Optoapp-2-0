-- RED/GREEN: SECURITY DEFINER EXECUTE grant contract (fix-security-definer-grants-contract).
-- Catalog-only DO-block: compatible with supabase db query / MCP execute_sql.
DO $$
DECLARE
  v_anon boolean;
  v_auth boolean;
  v_svc boolean;
BEGIN
  -- paciente_eliminaciones_restantes_hoy: authenticated + service_role only
  SELECT has_function_privilege('anon', 'public.paciente_eliminaciones_restantes_hoy(text)', 'EXECUTE') INTO v_anon;
  SELECT has_function_privilege('authenticated', 'public.paciente_eliminaciones_restantes_hoy(text)', 'EXECUTE') INTO v_auth;
  SELECT has_function_privilege('service_role', 'public.paciente_eliminaciones_restantes_hoy(text)', 'EXECUTE') INTO v_svc;
  IF v_anon IS NOT FALSE THEN RAISE EXCEPTION 'paciente: anon must lack EXECUTE'; END IF;
  IF v_auth IS NOT TRUE THEN RAISE EXCEPTION 'paciente: authenticated must have EXECUTE'; END IF;
  IF v_svc IS NOT TRUE THEN RAISE EXCEPTION 'paciente: service_role must have EXECUTE'; END IF;

  -- recalcular_resumen_diario_admin: service_role only
  SELECT has_function_privilege('anon', 'public.recalcular_resumen_diario_admin(text, date)', 'EXECUTE') INTO v_anon;
  SELECT has_function_privilege('authenticated', 'public.recalcular_resumen_diario_admin(text, date)', 'EXECUTE') INTO v_auth;
  SELECT has_function_privilege('service_role', 'public.recalcular_resumen_diario_admin(text, date)', 'EXECUTE') INTO v_svc;
  IF v_anon IS NOT FALSE THEN RAISE EXCEPTION 'admin: anon must lack EXECUTE'; END IF;
  IF v_auth IS NOT FALSE THEN RAISE EXCEPTION 'admin: authenticated must lack EXECUTE'; END IF;
  IF v_svc IS NOT TRUE THEN RAISE EXCEPTION 'admin: service_role must have EXECUTE'; END IF;

  -- check_rate_limit: intentional anon + authenticated
  SELECT has_function_privilege('anon', 'public.check_rate_limit(text, integer, integer)', 'EXECUTE') INTO v_anon;
  SELECT has_function_privilege('authenticated', 'public.check_rate_limit(text, integer, integer)', 'EXECUTE') INTO v_auth;
  IF v_anon IS NOT TRUE THEN RAISE EXCEPTION 'rate_limit: anon must have EXECUTE'; END IF;
  IF v_auth IS NOT TRUE THEN RAISE EXCEPTION 'rate_limit: authenticated must have EXECUTE'; END IF;

  -- intentional authenticated app RPCs (anon denied)
  IF has_function_privilege('authenticated', 'public.recalcular_resumen_diario(text, date)', 'EXECUTE') IS NOT TRUE THEN
    RAISE EXCEPTION 'recalcular_resumen_diario: authenticated must have EXECUTE';
  END IF;
  IF has_function_privilege('anon', 'public.recalcular_resumen_diario(text, date)', 'EXECUTE') IS NOT FALSE THEN
    RAISE EXCEPTION 'recalcular_resumen_diario: anon must lack EXECUTE';
  END IF;

  IF has_function_privilege(
    'authenticated',
    'public.create_optica_for_current_user(text, text, text, text, text, text)',
    'EXECUTE'
  ) IS NOT TRUE THEN
    RAISE EXCEPTION 'create_optica: authenticated must have EXECUTE';
  END IF;
  IF has_function_privilege(
    'anon',
    'public.create_optica_for_current_user(text, text, text, text, text, text)',
    'EXECUTE'
  ) IS NOT FALSE THEN
    RAISE EXCEPTION 'create_optica: anon must lack EXECUTE';
  END IF;

  IF has_function_privilege(
    'authenticated',
    'public.rpc_adjust_montura_stock(text, text, integer, text, text, text, text)',
    'EXECUTE'
  ) IS NOT TRUE THEN
    RAISE EXCEPTION 'rpc_adjust_montura_stock: authenticated must have EXECUTE';
  END IF;
  IF has_function_privilege(
    'anon',
    'public.rpc_adjust_montura_stock(text, text, integer, text, text, text, text)',
    'EXECUTE'
  ) IS NOT FALSE THEN
    RAISE EXCEPTION 'rpc_adjust_montura_stock: anon must lack EXECUTE';
  END IF;

  RAISE NOTICE 'GREEN: security definer grant contract holds';
END;
$$;
