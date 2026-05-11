-- Seguridad: la app cliente no debe poder cambiar opticas.plan vía PostgREST (JWT anon/authenticated).
-- Billing / administración: service_role o edición SQL en consola (sin JWT de app).

CREATE OR REPLACE FUNCTION public.opticas_lock_plan_from_clients()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    jwt_role text;
BEGIN
    IF TG_OP <> 'UPDATE' THEN
        RETURN NEW;
    END IF;
    IF NEW.plan IS NOT DISTINCT FROM OLD.plan THEN
        RETURN NEW;
    END IF;
    BEGIN
        jwt_role := nullif(trim(both from coalesce(current_setting('request.jwt.claim.role', true), '')), '');
    EXCEPTION WHEN OTHERS THEN
        jwt_role := NULL;
    END;
    IF jwt_role IN ('anon', 'authenticated') THEN
        NEW.plan := OLD.plan;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_opticas_lock_plan ON public.opticas;
CREATE TRIGGER trg_opticas_lock_plan
    BEFORE UPDATE ON public.opticas
    FOR EACH ROW
    EXECUTE FUNCTION public.opticas_lock_plan_from_clients();

COMMENT ON FUNCTION public.opticas_lock_plan_from_clients() IS
    'Evita que la app (roles anon/authenticated) modifique plan; usar service_role o SQL para billing.';
