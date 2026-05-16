CREATE OR REPLACE FUNCTION public.enforce_optica_limit_for_creator()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid;
  v_current_count integer;
  v_allowed integer;
  v_has_unlimited boolean;
  v_total_opticas integer;
BEGIN
  v_uid := auth.uid();

  -- Si no hay sesión (anon key), verificar si el sistema está en bootstrap
  IF v_uid IS NULL THEN
    SELECT count(*) INTO v_total_opticas FROM public.opticas;
    -- Bootstrap: si no hay ópticas, permitir la primera creación
    IF coalesce(v_total_opticas, 0) = 0 THEN
      RETURN new;
    END IF;
    RAISE EXCEPTION 'Sesión requerida para crear óptica';
  END IF;

  SELECT count(DISTINCT uo.optica_id)
  INTO v_current_count
  FROM public.usuario_optica uo
  WHERE uo.user_id = v_uid
    AND lower(trim(uo.rol)) IN ('admin', 'gerente');

  IF coalesce(v_current_count, 0) = 0 THEN
    RETURN new;
  END IF;

  SELECT EXISTS (
    SELECT 1
    FROM public.usuario_optica uo
    JOIN public.opticas o ON o.id = uo.optica_id
    WHERE uo.user_id = v_uid
      AND lower(trim(uo.rol)) IN ('admin', 'gerente')
      AND o.max_opticas IS NULL
  ) INTO v_has_unlimited;

  IF v_has_unlimited THEN
    RETURN new;
  END IF;

  SELECT max(o.max_opticas)
  INTO v_allowed
  FROM public.usuario_optica uo
  JOIN public.opticas o ON o.id = uo.optica_id
  WHERE uo.user_id = v_uid
    AND lower(trim(uo.rol)) IN ('admin', 'gerente');

  IF v_allowed IS NULL THEN
    RAISE EXCEPTION 'No se pudo determinar el límite de ópticas de tu plan';
  END IF;

  IF v_current_count >= v_allowed THEN
    RAISE EXCEPTION 'Has alcanzado el límite de ópticas de tu plan (%). Actualiza a un plan superior.', v_allowed;
  END IF;

  RETURN new;
END;
$$;
