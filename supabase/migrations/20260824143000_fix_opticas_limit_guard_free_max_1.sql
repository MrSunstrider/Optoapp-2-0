-- FREE product: max 1 óptica (sucursal) per user.
-- Replaces the temporary 2-óptica guard from 20260627005400_remove_free_plan_restrictions.sql.
-- Count admin|gerente so self-demotion admin→gerente cannot bypass the FREE cap.
-- Invited roles (asesor, etc.) still do not block first-óptica onboarding.

CREATE OR REPLACE FUNCTION public.enforce_optica_limit_for_creator()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_count bigint;
BEGIN
  select count(*) into v_count
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente');

  if v_count >= 1 then
    raise exception 'Has alcanzado el límite de 1 óptica del plan gratuito.';
  end if;

  RETURN NEW;
END;
$$;
