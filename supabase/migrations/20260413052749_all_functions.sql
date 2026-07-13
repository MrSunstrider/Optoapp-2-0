-- Essential functions that MUST exist before any migration references them.
-- These were created directly on the remote (Dashboard) and never existed
-- as migration files before their first reference.

-- Schema needed by RLS policies from the very first migrations
CREATE SCHEMA IF NOT EXISTS app_private;

-- app_private helpers (referenced by RLS policies and guard functions)
CREATE OR REPLACE FUNCTION app_private.has_optica_role(p_user_id uuid, p_optica_id text, p_roles text[] DEFAULT ARRAY['admin'::text, 'gerente'::text])
 RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $function$
  select exists (select 1 from public.usuario_optica uo where uo.user_id = p_user_id and uo.optica_id = p_optica_id and lower(trim(uo.rol)) = any (p_roles));
$function$;

CREATE OR REPLACE FUNCTION app_private.is_internal_owner()
 RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $function$
  select exists (select 1 from public.user_profiles up where up.user_id = auth.uid() and lower(trim(up.email)) = 'jaermadera@gmail.com');
$function$;

CREATE OR REPLACE FUNCTION app_private.is_optica_member(p_user_id uuid, p_optica_id text)
 RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $function$
  select exists (select 1 from public.usuario_optica uo where uo.user_id = p_user_id and uo.optica_id = p_optica_id);
$function$;

-- update_updated_at: referenced by triggers from 20260429214000, defined in 20260510000000
CREATE OR REPLACE FUNCTION public.update_updated_at()
 RETURNS trigger LANGUAGE plpgsql SET search_path TO 'public'
AS $function$
begin new.updated_at := timezone('utc', now()); return new; end;
$function$;

-- set_updated_audit_fields: referenced by triggers from 20260423042000,
-- defined later in migration chain but needed earlier
CREATE OR REPLACE FUNCTION public.set_updated_audit_fields()
 RETURNS trigger LANGUAGE plpgsql SET search_path TO 'public'
AS $function$
begin if new.updated_at is null then new.updated_at := timezone('utc', now()); end if;
begin new.updated_by := auth.uid(); exception when others then null; end; return new; end;
$function$;

-- rls_auto_enable: event trigger referenced by GRANT/REVOKE from 20260427053500
CREATE OR REPLACE FUNCTION public.rls_auto_enable()
 RETURNS event_trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'pg_catalog'
AS $function$
DECLARE cmd record;
BEGIN
  FOR cmd IN SELECT * FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE','CREATE TABLE AS','SELECT INTO') AND object_type IN ('table','partitioned table')
  LOOP
    IF cmd.schema_name = 'public' THEN
      BEGIN EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity); EXCEPTION WHEN OTHERS THEN null; END;
    END IF;
  END LOOP;
END;
$function$;

-- Extension required by 20260511000000 (pin_attempts rate limit)
CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA pg_catalog;
GRANT USAGE ON SCHEMA cron TO postgres;