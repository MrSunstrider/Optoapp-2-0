


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


CREATE SCHEMA IF NOT EXISTS "app_private";


ALTER SCHEMA "app_private" OWNER TO "postgres";


CREATE EXTENSION IF NOT EXISTS "pg_cron" WITH SCHEMA "pg_catalog";






COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE EXTENSION IF NOT EXISTS "pg_stat_statements" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "supabase_vault" WITH SCHEMA "vault";






CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";






CREATE OR REPLACE FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[] DEFAULT ARRAY['admin'::"text", 'gerente'::"text"]) RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = p_user_id
      and uo.optica_id = p_optica_id
      and lower(trim(uo.rol)) = any (p_roles)
  );
$$;


ALTER FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "app_private"."is_internal_owner"() RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
  select exists (
    select 1
    from public.user_profiles up
    where up.user_id = auth.uid()
      and lower(trim(up.email)) = 'jaermadera@gmail.com'
  );
$$;


ALTER FUNCTION "app_private"."is_internal_owner"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = p_user_id
      and uo.optica_id = p_optica_id
  );
$$;


ALTER FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."assert_backup_operation_allowed"("p_action" "text", "p_source_optica_id" "text", "p_target_optica_id" "text") RETURNS "void"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
declare
  v_uid uuid;
  v_is_admin_target boolean;
  v_is_admin_source boolean;
  v_action text := lower(btrim(coalesce(p_action, '')));
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sesión inválida.';
  end if;

  if v_action not in ('export', 'restore') then
    raise exception 'Acción no válida: %', p_action;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = v_uid
      and uo.optica_id = p_target_optica_id
      and lower(trim(uo.rol)) = 'admin'
  ) into v_is_admin_target;

  if not v_is_admin_target then
    raise exception 'Solo admin de la óptica activa puede realizar esta operación.';
  end if;

  if v_action = 'restore' then
    if btrim(coalesce(p_source_optica_id, '')) = '' then
      raise exception 'Respaldo sin óptica de origen.';
    end if;

    if p_source_optica_id <> p_target_optica_id then
      raise exception 'No se permite restaurar respaldos de otra óptica.';
    end if;

    select exists (
      select 1
      from public.usuario_optica uo
      where uo.user_id = v_uid
        and uo.optica_id = p_source_optica_id
        and lower(trim(uo.rol)) = 'admin'
    ) into v_is_admin_source;

    if not v_is_admin_source then
      raise exception 'Sin permisos admin sobre la óptica origen del respaldo.';
    end if;
  end if;
end;
$$;


ALTER FUNCTION "public"."assert_backup_operation_allowed"("p_action" "text", "p_source_optica_id" "text", "p_target_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."assign_optica_role_by_email"("p_optica_id" "text", "p_email" "text", "p_rol" "text") RETURNS "void"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
declare
  v_email text;
  v_uid uuid;
  v_rol text;
  v_is_allowed boolean;
begin
  v_email := lower(btrim(coalesce(p_email, '')));
  v_rol := lower(btrim(coalesce(p_rol, '')));

  if v_email = '' then
    raise exception 'Email requerido';
  end if;

  if v_rol not in ('admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas', 'invitado') then
    raise exception 'Rol no permitido: %', p_rol;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = p_optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  ) into v_is_allowed;

  if not v_is_allowed then
    raise exception 'Sin permisos para gestionar roles en esta óptica';
  end if;

  select up.user_id into v_uid
  from public.user_profiles up
  where up.email = v_email;

  if v_uid is null then
    raise exception 'No existe una cuenta con ese email. Debe registrarse primero.';
  end if;

  insert into public.usuario_optica (user_id, optica_id, rol)
  values (v_uid, p_optica_id, v_rol)
  on conflict (user_id, optica_id)
  do update set rol = excluded.rol;
end;
$$;


ALTER FUNCTION "public"."assign_optica_role_by_email"("p_optica_id" "text", "p_email" "text", "p_rol" "text") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."assign_optica_role_by_email"("p_optica_id" "text", "p_email" "text", "p_rol" "text") IS 'Permite a admin/gerente asignar membresía/rol en su óptica usando email de cuenta existente.';



CREATE OR REPLACE FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) RETURNS "jsonb"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_row record;
    v_now timestamptz := now();
    v_window_ago timestamptz := v_now - (p_window_ms || ' milliseconds')::interval;
BEGIN
    SELECT * INTO v_row
    FROM pin_attempts
    WHERE limit_key = p_limit_key
      AND window_start > v_window_ago
    ORDER BY window_start DESC
    LIMIT 1;

    IF NOT FOUND THEN
        INSERT INTO pin_attempts (limit_key, attempts, window_start)
        VALUES (p_limit_key, 1, v_now);
        RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - 1);
    END IF;

    IF v_row.attempts >= p_max_attempts THEN
        RETURN jsonb_build_object('allowed', false, 'remaining', 0);
    END IF;

    UPDATE pin_attempts SET attempts = v_row.attempts + 1 WHERE id = v_row.id;
    RETURN jsonb_build_object('allowed', true, 'remaining', p_max_attempts - v_row.attempts - 1);
END;
$$;


ALTER FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) OWNER TO "postgres";


COMMENT ON FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) IS 'SECURITY DEFINER intentionally — needed for PIN rate limiting. Accesses pin_attempts table.';



CREATE OR REPLACE FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text" DEFAULT ''::"text", "p_fiscal_doc_numero" "text" DEFAULT ''::"text", "p_razon_social" "text" DEFAULT ''::"text", "p_direccion_fiscal" "text" DEFAULT ''::"text") RETURNS "void"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
declare
  v_uid    uuid;
  v_nombre text;
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sin sesión';
  end if;

  v_nombre := trim(p_nombre);
  if v_nombre = '' then
    raise exception 'Nombre de óptica requerido';
  end if;

  insert into public.opticas (
    id, nombre, plan, plan_code,
    plan_source, plan_status,
    fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal
  ) values (
    p_optica_id, v_nombre, 'free', 'free', 'manual', 'active',
    upper(trim(p_fiscal_doc_tipo)),
    trim(p_fiscal_doc_numero),
    trim(p_razon_social),
    trim(p_direccion_fiscal)
  )
  on conflict (id) do update set
    nombre            = excluded.nombre,
    fiscal_doc_tipo   = excluded.fiscal_doc_tipo,
    fiscal_doc_numero = excluded.fiscal_doc_numero,
    razon_social      = excluded.razon_social,
    direccion_fiscal  = excluded.direccion_fiscal;

  insert into public.usuario_optica (user_id, optica_id, rol)
  values (v_uid, p_optica_id, 'admin')
  on conflict (user_id, optica_id) do update set rol = 'admin';
end;
$$;


ALTER FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text", "p_fiscal_doc_numero" "text", "p_razon_social" "text", "p_direccion_fiscal" "text") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text", "p_fiscal_doc_numero" "text", "p_razon_social" "text", "p_direccion_fiscal" "text") IS 'SECURITY DEFINER intentionally — needed for onboarding. ON CONFLICT DO UPDATE risk mitigated by enforce_admin_role_assignment_guard trigger.';



CREATE OR REPLACE FUNCTION "public"."enforce_admin_role_assignment_guard"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
declare
  v_actor_is_admin boolean;
  v_bootstrap_allowed boolean;
begin
  if lower(trim(coalesce(new.rol, ''))) <> 'admin' then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = new.optica_id
      and lower(trim(self.rol)) = 'admin'
  ) into v_actor_is_admin;

  if v_actor_is_admin then
    return new;
  end if;

  select (
    new.user_id = auth.uid()
    and not exists (
      select 1 from public.usuario_optica u where u.optica_id = new.optica_id
    )
  ) into v_bootstrap_allowed;

  if v_bootstrap_allowed then
    return new;
  end if;

  raise exception 'Solo un admin actual de la óptica puede asignar rol admin';
end;
$$;


ALTER FUNCTION "public"."enforce_admin_role_assignment_guard"() OWNER TO "postgres";


COMMENT ON FUNCTION "public"."enforce_admin_role_assignment_guard"() IS 'Bloquea asignación de rol admin cuando el actor no es admin vigente de la óptica.';



CREATE OR REPLACE FUNCTION "public"."enforce_dev_owner_guard"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
begin
  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' and not app_private.is_internal_owner() then
    raise exception 'El plan dev_owner está restringido al owner interno';
  end if;

  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' then
    new.plan_source := 'internal';
  end if;

  return new;
end;
$$;


ALTER FUNCTION "public"."enforce_dev_owner_guard"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."enforce_dev_owner_membership_guard"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
declare
  v_is_dev_owner_optica boolean;
begin
  select exists (
    select 1
    from public.opticas o
    where o.id = new.optica_id
      and lower(trim(o.plan_code)) = 'dev_owner'
  ) into v_is_dev_owner_optica;

  if not v_is_dev_owner_optica then
    return new;
  end if;

  if not public.is_internal_owner() then
    raise exception 'Membresía restringida: la óptica dev_owner solo puede ser gestionada por el owner interno';
  end if;

  return new;
end;
$$;


ALTER FUNCTION "public"."enforce_dev_owner_membership_guard"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."enforce_optica_limit_for_creator"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
DECLARE
  v_count bigint;
BEGIN
  select count(*) into v_count
  from public.usuario_optica uo
  where uo.user_id = auth.uid();

  if v_count >= 2 then
    raise exception 'Has alcanzado el límite de 2 ópticas del plan gratuito.';
  end if;

  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."enforce_optica_limit_for_creator"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."guard_opticas_business_profile_optional_update"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
declare
  v_can_edit boolean;
begin
  if new.distrito_ciudad_departamento is not distinct from old.distrito_ciudad_departamento
     and new.moneda is not distinct from old.moneda
     and new.pais is not distinct from old.pais
     and new.contacto_whatsapp_telefono is not distinct from old.contacto_whatsapp_telefono then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
      and uo.optica_id = old.id
      and lower(trim(uo.rol)) in ('admin', 'gerente')
  ) into v_can_edit;

  if not v_can_edit then
    raise exception 'Solo admin/gerente puede actualizar perfil de óptica';
  end if;

  new.distrito_ciudad_departamento := trim(coalesce(new.distrito_ciudad_departamento, ''));
  new.moneda := trim(coalesce(new.moneda, ''));
  new.pais := trim(coalesce(new.pais, ''));
  new.contacto_whatsapp_telefono := trim(coalesce(new.contacto_whatsapp_telefono, ''));

  return new;
end;
$$;


ALTER FUNCTION "public"."guard_opticas_business_profile_optional_update"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."guard_opticas_fiscal_update"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
declare
  v_can_edit boolean;
begin
  if new.fiscal_doc_tipo is not distinct from old.fiscal_doc_tipo
     and new.fiscal_doc_numero is not distinct from old.fiscal_doc_numero
     and new.razon_social is not distinct from old.razon_social
     and new.direccion_fiscal is not distinct from old.direccion_fiscal then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
      and uo.optica_id = old.id
      and lower(trim(uo.rol)) in ('admin', 'gerente')
  ) into v_can_edit;

  if not v_can_edit then
    raise exception 'Solo admin/gerente puede actualizar datos fiscales de la óptica';
  end if;

  new.fiscal_doc_tipo := upper(trim(coalesce(new.fiscal_doc_tipo, '')));
  if new.fiscal_doc_tipo not in ('RUC', 'RUS') then
    raise exception 'Tipo fiscal inválido. Use RUC o RUS';
  end if;

  new.fiscal_doc_numero := trim(coalesce(new.fiscal_doc_numero, ''));
  new.razon_social := trim(coalesce(new.razon_social, ''));
  new.direccion_fiscal := trim(coalesce(new.direccion_fiscal, ''));

  if new.fiscal_doc_numero = '' or new.razon_social = '' or new.direccion_fiscal = '' then
    raise exception 'RUC/RUS, razón social y dirección fiscal son obligatorios';
  end if;

  return new;
end;
$$;


ALTER FUNCTION "public"."guard_opticas_fiscal_update"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."guard_pacientes_delete"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
declare
  v_uid uuid;
  v_is_allowed boolean;
  v_deleted_today int;
  v_daily_limit int := 10;
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sesión inválida para eliminación de paciente.';
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = v_uid
      and uo.optica_id = old.optica_id
      and lower(trim(uo.rol)) in ('admin', 'gerente')
  ) into v_is_allowed;

  if not v_is_allowed then
    raise exception 'Solo admin o gerente pueden eliminar pacientes.';
  end if;

  select count(*)
  into v_deleted_today
  from public.pacientes_delete_audit a
  where a.optica_id = old.optica_id
    and a.deleted_by = v_uid
    and a.deleted_at >= date_trunc('day', timezone('utc', now()))
    and a.deleted_at < date_trunc('day', timezone('utc', now())) + interval '1 day';

  if v_deleted_today >= v_daily_limit then
    raise exception 'Límite diario de eliminaciones alcanzado (%).', v_daily_limit;
  end if;

  insert into public.pacientes_delete_audit (optica_id, paciente_id, deleted_by)
  values (old.optica_id, old.id, v_uid);

  return old;
end;
$$;


ALTER FUNCTION "public"."guard_pacientes_delete"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[] DEFAULT ARRAY['admin'::"text", 'gerente'::"text"]) RETURNS boolean
    LANGUAGE "sql" STABLE
    SET "search_path" TO 'public'
    AS $$
  select app_private.has_optica_role(p_user_id, p_optica_id, p_roles);
$$;


ALTER FUNCTION "public"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_internal_owner"() RETURNS boolean
    LANGUAGE "sql" STABLE
    SET "search_path" TO 'public'
    AS $$
  select app_private.is_internal_owner();
$$;


ALTER FUNCTION "public"."is_internal_owner"() OWNER TO "postgres";


COMMENT ON FUNCTION "public"."is_internal_owner"() IS 'Retorna true solo para la cuenta interna autorizada para plan dev_owner.';



CREATE OR REPLACE FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") RETURNS integer
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
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
$$;


ALTER FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") IS 'SECURITY DEFINER intentionally — needed for delete rate limiting.';



CREATE OR REPLACE FUNCTION "public"."recalcular_resumen_diario"("p_optica_id" "text", "p_fecha" "date") RETURNS "void"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_ventas_cantidad INTEGER; v_ventas_monto NUMERIC; v_ventas_costo NUMERIC;
    v_cobros_cantidad INTEGER; v_cobros_monto NUMERIC;
    v_saldo_total NUMERIC; v_saldo_cantidad INTEGER;
    v_inv_valor NUMERIC; v_inv_unidades INTEGER;
BEGIN
    WITH daily_ventas AS (
        SELECT monto_total, 0::numeric AS costo_unitario_snapshot FROM public.dispensaciones WHERE optica_id = p_optica_id AND fecha = p_fecha
        UNION ALL
        SELECT monto_total, 0::numeric AS costo_unitario_snapshot FROM public.servicios_extra WHERE optica_id = p_optica_id AND fecha = p_fecha
    )
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(monto_total),0), COALESCE(SUM(costo_unitario_snapshot),0) INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo FROM daily_ventas;
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(monto),0) INTO v_cobros_cantidad, v_cobros_monto FROM public.pagos WHERE optica_id=p_optica_id AND fecha=p_fecha AND tipo IS DISTINCT FROM 'Anulación';
    WITH pagos_dedup AS (
        SELECT COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match, pg.monto FROM public.pagos pg WHERE pg.optica_id=p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'
    ), all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total FROM public.dispensaciones WHERE optica_id = p_optica_id
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total FROM public.servicios_extra WHERE optica_id = p_optica_id
    )
    SELECT COALESCE(COUNT(*),0), COALESCE(SUM(v.monto_total-COALESCE(pd.total_pagado,0)),0) INTO v_saldo_cantidad, v_saldo_total
    FROM all_ventas v LEFT JOIN (SELECT venta_id_match,SUM(monto) AS total_pagado FROM pagos_dedup GROUP BY venta_id_match) pd ON pd.venta_id_match=v.venta_id
    WHERE v.monto_total-COALESCE(pd.total_pagado,0)>0.005;
    SELECT COALESCE(SUM(costo*stock_actual),0), COALESCE(SUM(stock_actual),0) INTO v_inv_valor, v_inv_unidades FROM public.monturas WHERE optica_id=p_optica_id;
    INSERT INTO public.resumen_diario (optica_id,fecha,ventas_cantidad,ventas_monto_total,ventas_costo_total,cobros_cantidad,cobros_monto_total,saldo_pendiente_total,saldo_pendiente_cantidad,inventario_valor,inventario_unidades)
    VALUES (p_optica_id,p_fecha,v_ventas_cantidad,v_ventas_monto,v_ventas_costo,v_cobros_cantidad,v_cobros_monto,v_saldo_total,v_saldo_cantidad,v_inv_valor,v_inv_unidades)
    ON CONFLICT (optica_id,fecha) DO UPDATE SET ventas_cantidad=EXCLUDED.ventas_cantidad, ventas_monto_total=EXCLUDED.ventas_monto_total, ventas_costo_total=EXCLUDED.ventas_costo_total, cobros_cantidad=EXCLUDED.cobros_cantidad, cobros_monto_total=EXCLUDED.cobros_monto_total, saldo_pendiente_total=EXCLUDED.saldo_pendiente_total, saldo_pendiente_cantidad=EXCLUDED.saldo_pendiente_cantidad, inventario_valor=EXCLUDED.inventario_valor, inventario_unidades=EXCLUDED.inventario_unidades, calculado_en=now();
END; $$;


ALTER FUNCTION "public"."recalcular_resumen_diario"("p_optica_id" "text", "p_fecha" "date") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rls_auto_enable"() RETURNS "event_trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'pg_catalog'
    AS $$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$$;


ALTER FUNCTION "public"."rls_auto_enable"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_adjust_montura_stock"("p_montura_id" "text", "p_optica_id" "text", "p_delta" integer, "p_reference_id" "text", "p_note" "text", "p_tipo" "text", "p_fecha" "text") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_new_stock integer;
    v_old_stock integer;
BEGIN
    UPDATE public.monturas
    SET stock_actual = stock_actual + p_delta
    WHERE id = p_montura_id
      AND optica_id = p_optica_id
    RETURNING stock_actual, stock_actual - p_delta
    INTO v_new_stock, v_old_stock;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_found');
    END IF;

    -- If the CHECK constraint didn't catch it (unlikely but safe),
    -- revert and signal insufficient.
    IF v_new_stock < 0 THEN
        UPDATE public.monturas
        SET stock_actual = v_old_stock
        WHERE id = p_montura_id
          AND optica_id = p_optica_id;
        RETURN jsonb_build_object('ok', false, 'error', 'insufficient');
    END IF;

    INSERT INTO public.montura_movimientos (
        id, montura_id, fecha, tipo, cantidad,
        stock_previo, stock_nuevo, referencia_id, nota, optica_id
    ) VALUES (
        gen_random_uuid()::text,
        p_montura_id,
        CAST(p_fecha AS date),
        p_tipo,
        ABS(p_delta),
        v_old_stock,
        v_new_stock,
        p_reference_id,
        p_note,
        p_optica_id
    );

    RETURN jsonb_build_object('ok', true, 'new_stock', v_new_stock);
END;
$$;


ALTER FUNCTION "public"."rpc_adjust_montura_stock"("p_montura_id" "text", "p_optica_id" "text", "p_delta" integer, "p_reference_id" "text", "p_note" "text", "p_tipo" "text", "p_fecha" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") RETURNS "jsonb"
    LANGUAGE "plpgsql" STABLE
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_ventas_mes NUMERIC; v_cobros_mes NUMERIC; v_costo_mes NUMERIC;
    v_gastos_mes NUMERIC; v_saldo_pendiente NUMERIC;
    v_margen_neto_pct NUMERIC; v_ticket_promedio NUMERIC;
    v_cantidad_ventas INTEGER; v_mes_anterior DATE;
    v_ventas_mes_anterior NUMERIC;
    v_margen_categoria jsonb; v_deudores_resumen jsonb;
    v_proyeccion jsonb; v_stock_estancado jsonb; v_valor_inventario NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';
    SELECT COALESCE(SUM(ventas_monto_total),0), COALESCE(SUM(cobros_monto_total),0),
           COALESCE(SUM(ventas_costo_total),0), COALESCE(SUM(ventas_cantidad),0)
    INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas
    FROM public.resumen_diario WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    v_ticket_promedio:=CASE WHEN v_cantidad_ventas>0 THEN v_ventas_mes/v_cantidad_ventas ELSE 0 END;
    SELECT COALESCE(SUM(monto),0) INTO v_gastos_mes FROM public.gastos_operativos
    WHERE optica_id=p_optica_id AND fecha>=p_mes AND fecha<p_mes+INTERVAL'1 month';
    SELECT COALESCE(saldo_pendiente_total,0) INTO v_saldo_pendiente FROM public.resumen_diario
    WHERE optica_id=p_optica_id AND fecha<p_mes+INTERVAL'1 month' ORDER BY fecha DESC LIMIT 1;
    v_margen_neto_pct:=CASE WHEN v_ventas_mes>0 THEN ROUND(((v_ventas_mes-v_costo_mes-v_gastos_mes)/v_ventas_mes)*100,1) ELSE 0 END;
    SELECT COALESCE(SUM(ventas_monto_total),0) INTO v_ventas_mes_anterior FROM public.resumen_diario
    WHERE optica_id=p_optica_id AND fecha>=v_mes_anterior AND fecha<p_mes;

    -- ====================================================================
    -- FIXED: margen_por_categoria -- inline revenue from dispensaciones
    --        + servicios_extra, mapped via CASE expression
    -- ====================================================================
    WITH category_revenue AS (
        SELECT
            CASE
                WHEN d.tipo_lente = 'Progresivo' THEN 'lente_progresivo'
                WHEN d.tipo_lente = 'Bifocal' THEN 'lente_bifocal'
                WHEN d.tipo_lente = 'Monofocal' AND d.material_lente = 'Resina' THEN 'lente_monofocal'
                WHEN d.tipo_lente = 'Monofocal' THEN 'lente_otro'
                ELSE 'lente_otro'
            END AS categoria_producto_id,
            SUM(d.monto_total) AS ventas
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id
          AND d.fecha >= p_mes AND d.fecha < p_mes + INTERVAL '1 month'
        GROUP BY categoria_producto_id
        UNION ALL
        SELECT 'servicio_extra', SUM(se.monto_total)
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id
          AND se.fecha >= p_mes AND se.fecha < p_mes + INTERVAL '1 month'
    ),
    aggregated_revenue AS (
        SELECT categoria_producto_id, SUM(ventas) AS ventas
        FROM category_revenue GROUP BY categoria_producto_id
    )
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'categoria', cat.nombre,
        'ventas', COALESCE(ar.ventas, 0),
        'costos', 0,
        'margen_pct', null::numeric
    ) ORDER BY cat.orden), '[]'::jsonb)
    INTO v_margen_categoria
    FROM public.categorias_producto cat
    LEFT JOIN aggregated_revenue ar ON ar.categoria_producto_id = cat.id;

    SELECT jsonb_build_object('cantidad',COUNT(*),'saldo_total',COALESCE(SUM(saldo),0))
    INTO v_deudores_resumen FROM public.rpc_deudores(p_optica_id);

    -- Proyeccion caja: UNION source tables instead of ventas
    WITH pagos_dedup AS (
        SELECT
            COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match,
            pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id=p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulaci�n'
    ),
    all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
    )
    SELECT jsonb_build_object('ingresos_esperados',
        COALESCE(SUM(v.monto_total-COALESCE(pd_total.total_pagado,0)),0),
        'egresos_programados',COALESCE((SELECT SUM(monto) FROM public.gastos_operativos
        WHERE optica_id=p_optica_id AND fecha_programada>=CURRENT_DATE),0),'saldo_neto',0)
    INTO v_proyeccion FROM all_ventas v
    LEFT JOIN (SELECT venta_id_match,SUM(monto) AS total_pagado FROM pagos_dedup GROUP BY venta_id_match) pd_total
    ON pd_total.venta_id_match=v.venta_id
    WHERE v.monto_total-COALESCE(pd_total.total_pagado,0)>0.005;

    v_proyeccion:=jsonb_set(v_proyeccion,'{saldo_neto}',
        to_jsonb(COALESCE((v_proyeccion->>'ingresos_esperados')::numeric,0)
               -COALESCE((v_proyeccion->>'egresos_programados')::numeric,0)));

    -- ====================================================================
    -- FIXED: stock_estancado -- real sales dates from montura_movimientos
    --        + dispensaciones; removed low-stock filter
    -- ====================================================================
    WITH ventas_montura AS (
        SELECT montura_id, MAX(fecha) AS ultima_venta
        FROM public.montura_movimientos
        WHERE optica_id = p_optica_id AND tipo = 'SALIDA_VENTA'
        GROUP BY montura_id
        UNION
        SELECT montura_id, MAX(fecha) AS ultima_venta
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id AND montura_id IS NOT NULL
        GROUP BY montura_id
    ),
    montura_venta_agg AS (
        SELECT montura_id, MAX(ultima_venta) AS ultima_venta
        FROM ventas_montura GROUP BY montura_id
    )
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'montura_id', m.id, 'sku', m.sku, 'modelo', m.modelo,
        'costo', COALESCE(m.costo, 0), 'stock_actual', m.stock_actual,
        'ultima_venta', mva.ultima_venta,
        'dias_sin_venta', CASE WHEN mva.ultima_venta IS NOT NULL
            THEN (CURRENT_DATE - mva.ultima_venta) ELSE 999 END
    ) ORDER BY CASE WHEN mva.ultima_venta IS NULL THEN 0 ELSE 1 END,
        mva.ultima_venta ASC NULLS LAST), '[]'::jsonb)
    INTO v_stock_estancado
    FROM public.monturas m
    LEFT JOIN montura_venta_agg mva ON mva.montura_id = m.id
    WHERE m.optica_id = p_optica_id AND m.activo = true AND m.stock_actual > 0;

    SELECT COALESCE(SUM(costo*stock_actual),0) INTO v_valor_inventario
    FROM public.monturas WHERE optica_id=p_optica_id AND activo=true;
    RETURN jsonb_build_object('ventas_mes',v_ventas_mes,'cobros_mes',v_cobros_mes,
        'costo_mes',v_costo_mes,'gastos_mes',v_gastos_mes,'saldo_pendiente',v_saldo_pendiente,
        'margen_neto_pct',v_margen_neto_pct,'ticket_promedio',v_ticket_promedio,
        'cantidad_ventas',v_cantidad_ventas,'ventas_mes_anterior',v_ventas_mes_anterior,
        'variacion_ventas_pct',CASE WHEN v_ventas_mes_anterior>0
        THEN ROUND(((v_ventas_mes-v_ventas_mes_anterior)/v_ventas_mes_anterior)*100,1) ELSE NULL END,
        'margen_por_categoria',v_margen_categoria,'deudores',v_deudores_resumen,
        'proyeccion_caja',v_proyeccion,'stock_estancado',v_stock_estancado,'valor_inventario',v_valor_inventario);
END;
$$;


ALTER FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") IS 'Active. Primary analytics function (8 indicators). Proyeccion_caja uses deduped pagos matching. Preferred over rpc_resumen_financiero and rpc_saldo_pendiente.';



CREATE OR REPLACE FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_efectivo numeric;
    v_movil_trans numeric;
    v_tarjeta numeric;
    v_total numeric;
BEGIN
    SELECT
        COALESCE(SUM(CASE
            WHEN metodo_pago = 'Efectivo' THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN metodo_pago IN ('Transferencia', 'Yape', 'Plin', 'Móvil')
            THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN metodo_pago = 'Tarjeta' THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(monto), 0)
    INTO
        v_efectivo,
        v_movil_trans,
        v_tarjeta,
        v_total
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    RETURN jsonb_build_object(
        'efectivo', v_efectivo,
        'movil_trans', v_movil_trans,
        'tarjeta', v_tarjeta,
        'total', v_total
    );
END;
$$;


ALTER FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") IS 'Active. Uses exact metodo_pago matching (not LIKE). Categorias: Efectivo, Tarjeta, Transferencia/Yape/Plin/Móvil.';



CREATE OR REPLACE FUNCTION "public"."rpc_count_pendientes"("p_optica_id" "text") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE v_entregas integer; v_servicios integer;
BEGIN
    WITH pendientes AS (SELECT 1 FROM public.dispensaciones WHERE optica_id = p_optica_id AND estado_entrega = 'Pendiente' AND fecha < CURRENT_DATE UNION ALL SELECT 1 FROM public.servicios_extra WHERE optica_id = p_optica_id AND estado = 'Pendiente' AND fecha < CURRENT_DATE)
    SELECT COUNT(*) INTO v_entregas FROM pendientes;
    WITH all_ventas AS (SELECT 'v_disp_' || id AS venta_id, monto_total FROM public.dispensaciones WHERE optica_id = p_optica_id UNION ALL SELECT 'v_serv_' || id AS venta_id, monto_total FROM public.servicios_extra WHERE optica_id = p_optica_id),
    pagos_agrupados AS (SELECT COALESCE(venta_id, 'v_disp_'||dispensacion_id, 'v_serv_'||servicio_extra_id) AS venta_id_match, SUM(monto) AS total_pagado FROM public.pagos WHERE optica_id = p_optica_id GROUP BY venta_id_match)
    SELECT COUNT(*) INTO v_servicios FROM all_ventas v LEFT JOIN pagos_agrupados pg ON pg.venta_id_match = v.venta_id WHERE v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;
    RETURN jsonb_build_object('entregas_pendientes', v_entregas, 'servicios_pendientes', v_servicios);
END; $$;


ALTER FUNCTION "public"."rpc_count_pendientes"("p_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_deudores"("p_optica_id" "text") RETURNS TABLE("paciente_nombre" "text", "paciente_telefono" "text", "venta_id" "text", "venta_fecha" "date", "monto_total" numeric, "total_pagado" numeric, "saldo" numeric, "dias_deuda" integer, "paciente_id" "text")
    LANGUAGE "plpgsql" STABLE
    SET "search_path" TO 'public'
    AS $$
BEGIN RETURN QUERY
WITH pagos_dedup AS (SELECT COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id) AS venta_id_match, pg.monto FROM public.pagos pg WHERE pg.optica_id = p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'),
all_ventas AS (SELECT 'v_disp_' || d.id AS venta_id, d.paciente_id, d.fecha, d.monto_total FROM public.dispensaciones d WHERE d.optica_id = p_optica_id UNION ALL SELECT 'v_serv_' || se.id AS venta_id, se.paciente_id, se.fecha, se.monto_total FROM public.servicios_extra se WHERE se.optica_id = p_optica_id)
SELECT COALESCE(p.nombre_completo, 'Sin paciente'), p.telefono, v.venta_id, v.fecha, v.monto_total, COALESCE(SUM(pd.monto), 0) AS total_pagado, v.monto_total - COALESCE(SUM(pd.monto), 0) AS saldo, CURRENT_DATE - v.fecha AS dias_deuda, v.paciente_id
FROM all_ventas v LEFT JOIN public.pacientes p ON p.id = v.paciente_id LEFT JOIN pagos_dedup pd ON pd.venta_id_match = v.venta_id
GROUP BY v.venta_id, v.fecha, v.monto_total, p.nombre_completo, p.telefono, v.paciente_id
HAVING v.monto_total - COALESCE(SUM(pd.monto), 0) > 0.005 ORDER BY dias_deuda DESC;
END; $$;


ALTER FUNCTION "public"."rpc_deudores"("p_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_pacientes_con_entrega_pendiente"("p_optica_id" "text") RETURNS "text"[]
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_ids text[];
BEGIN
    SELECT ARRAY_AGG(DISTINCT paciente_id)
    INTO v_ids
    FROM (
        SELECT paciente_id
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND estado_entrega = 'Pendiente'

        UNION

        SELECT paciente_id
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND estado = 'Pendiente'
    ) sub;

    RETURN COALESCE(v_ids, ARRAY[]::text[]);
END;
$$;


ALTER FUNCTION "public"."rpc_pacientes_con_entrega_pendiente"("p_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_pacientes_con_saldo"("p_optica_id" "text") RETURNS "text"[]
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_ids text[];
BEGIN
    SELECT ARRAY_AGG(DISTINCT paciente_id)
    INTO v_ids
    FROM (
        SELECT paciente_id
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND (monto_total - COALESCE(monto_pagado, 0)) > 0.005

        UNION

        SELECT paciente_id
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND (monto_total - COALESCE(a_cuenta, 0)) > 0.005
    ) sub;

    RETURN COALESCE(v_ids, ARRAY[]::text[]);
END;
$$;


ALTER FUNCTION "public"."rpc_pacientes_con_saldo"("p_optica_id" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE v_disp numeric; v_serv numeric; v_total numeric;
BEGIN
    SELECT COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_disp FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos WHERE optica_id = p_optica_id AND tipo IS DISTINCT FROM 'Anulación'
        GROUP BY venta_id
    ) pd ON pd.venta_id = v.id
    WHERE v.optica_id = p_optica_id AND v.origen = 'dispensacion'
      AND v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    SELECT COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_serv FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos WHERE optica_id = p_optica_id AND tipo IS DISTINCT FROM 'Anulación'
        GROUP BY venta_id
    ) pd ON pd.venta_id = v.id
    WHERE v.optica_id = p_optica_id AND v.origen = 'servicio_extra'
      AND v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    v_total := v_disp + v_serv;
    RETURN jsonb_build_object('saldo_dispensaciones', v_disp, 'saldo_servicios', v_serv, 'saldo_total', v_total);
END;
$$;


ALTER FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") IS 'DEPRECATED: Uses dispensaciones/servicios_extra directly instead of unified ventas table. Use rpc_analisis_mensual which includes saldo_pendiente.';



CREATE OR REPLACE FUNCTION "public"."set_sync_telemetry_audit_fields"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
begin
  new.updated_at := timezone('utc', now());
  begin
    new.last_actor := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;


ALTER FUNCTION "public"."set_sync_telemetry_audit_fields"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."set_sync_telemetry_updated_at"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;


ALTER FUNCTION "public"."set_sync_telemetry_updated_at"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."set_updated_audit_fields"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
begin
  if new.updated_at is null then
    new.updated_at := timezone('utc', now());
  end if;
  begin
    new.updated_by := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;


ALTER FUNCTION "public"."set_updated_audit_fields"() OWNER TO "postgres";


COMMENT ON FUNCTION "public"."set_updated_audit_fields"() IS 'Audits updated_by server-side. updated_at is preserved from the client; server-side fallback only applies when the value is null (should not occur on NOT NULL columns, but kept for defensive correctness).';



CREATE OR REPLACE FUNCTION "public"."suggest_next_ho"("p_optica_id" "uuid") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $_$
DECLARE
    v_year text := EXTRACT(YEAR FROM NOW())::text;
    v_max_num int;
BEGIN
    SELECT MAX(
        NULLIF(regexp_replace(
            historia_optometrica,
            '^HO-' || v_year || '-(\d+)$',
            '\1'
        ), historia_optometrica)::int
    ) INTO v_max_num
    FROM pacientes
    WHERE optica_id = p_optica_id
      AND historia_optometrica ~* ('^HO-' || v_year || '-\d+$');

    v_max_num := COALESCE(v_max_num, 0);
    RETURN jsonb_build_object(
        'next_ho', 'HO-' || v_year || '-' || LPAD((v_max_num + 1)::text, 4, '0')
    );
END;
$_$;


ALTER FUNCTION "public"."suggest_next_ho"("p_optica_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."sync_snapshot"("p_optica_id" "uuid") RETURNS "jsonb"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_pacientes_total int;
    v_disp_total int;
    v_disp_pendientes int;
    v_serv_total int;
    v_serv_pendientes int;
    v_eval_total int;
    v_eval_pendientes int;
    v_inv_total int;
    v_inv_critico int;
BEGIN
    -- Pacientes: total count
    SELECT COUNT(*) INTO v_pacientes_total
    FROM pacientes WHERE optica_id = p_optica_id;

    -- Dispensaciones: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado_entrega = 'Pendiente')
    INTO v_disp_total, v_disp_pendientes
    FROM dispensaciones WHERE optica_id = p_optica_id;

    -- Servicios extra: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado = 'Pendiente')
    INTO v_serv_total, v_serv_pendientes
    FROM servicios_extra WHERE optica_id = p_optica_id;

    -- Evaluaciones: total (con próxima cita) + pendientes (no atendidas ni canceladas)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (
            WHERE cita_estado IS NULL OR (cita_estado <> 'atendida' AND cita_estado <> 'cancelada')
        )
    INTO v_eval_total, v_eval_pendientes
    FROM evaluaciones
    WHERE optica_id = p_optica_id
      AND proxima_cita IS NOT NULL;

    -- Inventario (monturas activas): total + críticas (stock <= 2)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (WHERE stock_actual <= 2)
    INTO v_inv_total, v_inv_critico
    FROM monturas
    WHERE optica_id = p_optica_id
      AND activo = true;

    RETURN jsonb_build_object(
        'pacientes', jsonb_build_object('total', v_pacientes_total, 'pending', 0),
        'dispensaciones', jsonb_build_object('total', v_disp_total, 'pending', v_disp_pendientes),
        'servicios_extra', jsonb_build_object('total', v_serv_total, 'pending', v_serv_pendientes),
        'evaluaciones', jsonb_build_object('total', v_eval_total, 'pending', v_eval_pendientes),
        'inventario', jsonb_build_object('total', v_inv_total, 'pending', v_inv_critico)
    );
END;
$$;


ALTER FUNCTION "public"."sync_snapshot"("p_optica_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."sync_user_profiles_from_auth"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public', 'auth'
    AS $$
begin
  if new.email is not null and btrim(new.email) <> '' then
    insert into public.user_profiles (user_id, email)
    values (new.id, lower(btrim(new.email)))
    on conflict (user_id) do update set email = excluded.email;
  end if;
  return new;
end;
$$;


ALTER FUNCTION "public"."sync_user_profiles_from_auth"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."trg_pagos_set_venta_id"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
BEGIN
    IF NEW.venta_id IS NULL THEN
        IF NEW.dispensacion_id IS NOT NULL THEN
            NEW.venta_id := 'v_disp_' || NEW.dispensacion_id;
        ELSIF NEW.servicio_extra_id IS NOT NULL THEN
            NEW.venta_id := 'v_serv_' || NEW.servicio_extra_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."trg_pagos_set_venta_id"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."trg_pagos_update_monto_pagado"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
DECLARE
    v_disp_id TEXT;
    v_serv_id TEXT;
BEGIN
    v_disp_id := COALESCE(NEW.dispensacion_id, OLD.dispensacion_id);
    v_serv_id := COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id);

    IF v_disp_id IS NOT NULL THEN
        UPDATE public.dispensaciones
        SET monto_pagado = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE dispensacion_id = v_disp_id
              AND tipo IS DISTINCT FROM 'Anulación'
        )
        WHERE id = v_disp_id;
    END IF;

    IF v_serv_id IS NOT NULL THEN
        UPDATE public.servicios_extra
        SET a_cuenta = (
            SELECT COALESCE(SUM(monto), 0)
            FROM public.pagos
            WHERE servicio_extra_id = v_serv_id
              AND tipo IS DISTINCT FROM 'Anulación'
        )
        WHERE id = v_serv_id;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;


ALTER FUNCTION "public"."trg_pagos_update_monto_pagado"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_updated_at"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;


ALTER FUNCTION "public"."update_updated_at"() OWNER TO "postgres";


COMMENT ON FUNCTION "public"."update_updated_at"() IS 'Actualiza updated_at en tablas sin columna updated_by (cierres_caja, optica_settings).';


SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."app_releases" (
    "id" bigint NOT NULL,
    "version" "text" NOT NULL,
    "apk_download_url" "text" NOT NULL,
    "release_notes" "text" DEFAULT ''::"text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."app_releases" OWNER TO "postgres";


ALTER TABLE "public"."app_releases" ALTER COLUMN "id" ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME "public"."app_releases_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE IF NOT EXISTS "public"."categorias_montura" (
    "id" "text" NOT NULL,
    "nombre" "text" NOT NULL,
    "descripcion" "text" DEFAULT ''::"text" NOT NULL,
    "optica_id" "text" NOT NULL
);


ALTER TABLE "public"."categorias_montura" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."categorias_producto" (
    "id" "text" NOT NULL,
    "nombre" "text" NOT NULL,
    "familia" "text" NOT NULL,
    "orden" integer DEFAULT 0 NOT NULL,
    CONSTRAINT "categorias_producto_familia_check" CHECK (("familia" = ANY (ARRAY['lente'::"text", 'montura'::"text", 'servicio'::"text"])))
);


ALTER TABLE "public"."categorias_producto" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."cierres_caja" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "fecha_operativa" "date" NOT NULL,
    "estado" "text" NOT NULL,
    "total_efectivo_cent" integer DEFAULT 0 NOT NULL,
    "total_tarjeta_cent" integer DEFAULT 0 NOT NULL,
    "total_transferencia_cent" integer DEFAULT 0 NOT NULL,
    "total_yape_cent" integer DEFAULT 0 NOT NULL,
    "total_plin_cent" integer DEFAULT 0 NOT NULL,
    "total_general_cent" integer DEFAULT 0 NOT NULL,
    "observaciones" "text",
    "closed_by" "uuid",
    "closed_at" timestamp with time zone,
    "reopened_by" "uuid",
    "reopened_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "cerrado_at" timestamp with time zone,
    "cerrado_por" "uuid",
    CONSTRAINT "cierres_caja_estado_check" CHECK (("estado" = ANY (ARRAY['abierto'::"text", 'cerrado'::"text"]))),
    CONSTRAINT "cierres_caja_total_efectivo_cent_check" CHECK (("total_efectivo_cent" >= 0)),
    CONSTRAINT "cierres_caja_total_general_cent_check" CHECK (("total_general_cent" >= 0)),
    CONSTRAINT "cierres_caja_total_plin_cent_check" CHECK (("total_plin_cent" >= 0)),
    CONSTRAINT "cierres_caja_total_tarjeta_cent_check" CHECK (("total_tarjeta_cent" >= 0)),
    CONSTRAINT "cierres_caja_total_transferencia_cent_check" CHECK (("total_transferencia_cent" >= 0)),
    CONSTRAINT "cierres_caja_total_yape_cent_check" CHECK (("total_yape_cent" >= 0))
);


ALTER TABLE "public"."cierres_caja" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."configuracion_financiera" (
    "optica_id" "text" NOT NULL,
    "margen_neto_objetivo" numeric DEFAULT 15.0,
    "ticket_promedio_objetivo" numeric,
    "caida_ventas_alerta_pct" numeric DEFAULT 10.0,
    "deuda_vieja_alerta_dias" integer DEFAULT 30,
    "deuda_total_alerta_monto" numeric DEFAULT 3000.0,
    "stock_estancado_alerta_dias" integer DEFAULT 180,
    "stock_bajo_alerta_unidades" integer DEFAULT 2,
    "min_ventas_para_recomendar" integer DEFAULT 5,
    "frecuencia_recalculo_dias" integer DEFAULT 1
);


ALTER TABLE "public"."configuracion_financiera" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."costos_productos" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "categoria_producto_id" "text" NOT NULL,
    "producto_descripcion" "text",
    "costo_unitario" numeric NOT NULL,
    "vigente_desde" "date" DEFAULT CURRENT_DATE NOT NULL,
    "vigente_hasta" "date",
    "fecha_actualizacion" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."costos_productos" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."dispensacion_items" (
    "id" "text" NOT NULL,
    "dispensacion_id" "text" NOT NULL,
    "tipo_lente" "text" DEFAULT ''::"text",
    "material_lente" "text" DEFAULT ''::"text",
    "tratamientos" "text" DEFAULT ''::"text",
    "color_lente" "text" DEFAULT ''::"text",
    "distancia_lente" "text" DEFAULT ''::"text",
    "altura" "text" DEFAULT ''::"text",
    "sub_tipo_bifocal" "text" DEFAULT ''::"text",
    "notas_diseno" "text" DEFAULT ''::"text",
    "montura_id" "text" DEFAULT ''::"text",
    "origen_montura" "text" DEFAULT ''::"text",
    "tipo_aro" "text" DEFAULT ''::"text",
    "material_montura" "text" DEFAULT ''::"text",
    "descripcion_montura" "text" DEFAULT ''::"text",
    "tipo_montura" "text" DEFAULT ''::"text",
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "filtro_discromatopsia_tipo" "text" DEFAULT ''::"text" NOT NULL
);


ALTER TABLE "public"."dispensacion_items" OWNER TO "postgres";


COMMENT ON TABLE "public"."dispensacion_items" IS 'Items de lente + montura dentro de una dispensación (F2-T2). Cada item puede tener su propia montura.';



CREATE TABLE IF NOT EXISTS "public"."dispensaciones" (
    "id" "text" NOT NULL,
    "paciente_id" "text" NOT NULL,
    "fecha" "date" NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "tipo_montura" "text" DEFAULT ''::"text" NOT NULL,
    "material_montura" "text" DEFAULT ''::"text" NOT NULL,
    "tipo_lente" "text" DEFAULT ''::"text" NOT NULL,
    "material_lente" "text" DEFAULT ''::"text" NOT NULL,
    "tratamientos" "text" DEFAULT ''::"text" NOT NULL,
    "color_lente" "text" DEFAULT ''::"text" NOT NULL,
    "notas_diseno" "text" DEFAULT ''::"text" NOT NULL,
    "origen_montura" "text" DEFAULT ''::"text" NOT NULL,
    "tipo_aro" "text" DEFAULT ''::"text" NOT NULL,
    "descripcion_montura" "text" DEFAULT ''::"text" NOT NULL,
    "monto_total" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "metodo_pago" "text" DEFAULT ''::"text" NOT NULL,
    "monto_pagado" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "estado_entrega" "text" DEFAULT 'Pendiente'::"text" NOT NULL,
    "fecha_vencimiento_garantia" "date",
    "distancia_lente" "text" DEFAULT ''::"text" NOT NULL,
    "sub_tipo_bifocal" "text" DEFAULT ''::"text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "altura" "text" DEFAULT ''::"text" NOT NULL,
    "ot" "text" DEFAULT ''::"text" NOT NULL,
    "montura_id" "text",
    "updated_by" "uuid",
    "fecha_entrega" "date",
    "filtro_discromatopsia_tipo" "text" DEFAULT ''::"text" NOT NULL,
    "reclamo_origen_id" "text",
    CONSTRAINT "dispensaciones_estado_entrega_domain_chk" CHECK (("estado_entrega" = ANY (ARRAY['Pendiente'::"text", 'Entregado'::"text"]))),
    CONSTRAINT "dispensaciones_estado_entrega_not_blank_chk" CHECK (("btrim"("estado_entrega") <> ''::"text")),
    CONSTRAINT "dispensaciones_monto_pagado_chk" CHECK (("monto_pagado" >= (0)::numeric)),
    CONSTRAINT "dispensaciones_monto_total_chk" CHECK (("monto_total" >= (0)::numeric))
);


ALTER TABLE "public"."dispensaciones" OWNER TO "postgres";


COMMENT ON TABLE "public"."dispensaciones" IS 'Dispensaciones / Órdenes de trabajo. OT ya no es única por óptica.';



CREATE TABLE IF NOT EXISTS "public"."evaluaciones" (
    "id" "text" NOT NULL,
    "paciente_id" "text" NOT NULL,
    "fecha" "date" NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "motivo_consulta" "text" DEFAULT ''::"text",
    "sintomas" "text" DEFAULT ''::"text",
    "antecedentes_personales_oculares" "text" DEFAULT ''::"text",
    "antecedentes_personales_sistemicos" "text" DEFAULT ''::"text",
    "antecedentes_familiares_oculares" "text" DEFAULT ''::"text",
    "antecedentes_familiares_sistemicos" "text" DEFAULT ''::"text",
    "medicacion" "text" DEFAULT ''::"text",
    "alergias" "text" DEFAULT ''::"text",
    "necesidad_visual" "text" DEFAULT ''::"text",
    "av_sc_od_lejos" "text" DEFAULT ''::"text",
    "av_sc_oi_lejos" "text" DEFAULT ''::"text",
    "av_sc_od_cerca" "text" DEFAULT ''::"text",
    "av_sc_oi_cerca" "text" DEFAULT ''::"text",
    "av_sc_ao" "text" DEFAULT ''::"text",
    "av_sc_ao_cerca" "text" DEFAULT ''::"text",
    "av_cc_od_lejos" "text" DEFAULT ''::"text",
    "av_cc_oi_lejos" "text" DEFAULT ''::"text",
    "av_cc_od_cerca" "text" DEFAULT ''::"text",
    "av_cc_oi_cerca" "text" DEFAULT ''::"text",
    "av_cc_ao_px" "text" DEFAULT ''::"text",
    "av_cc_ao_cerca" "text" DEFAULT ''::"text",
    "obj_od_esf" "text" DEFAULT ''::"text",
    "obj_od_cil" "text" DEFAULT ''::"text",
    "obj_od_eje" "text" DEFAULT ''::"text",
    "obj_oi_esf" "text" DEFAULT ''::"text",
    "obj_oi_cil" "text" DEFAULT ''::"text",
    "obj_oi_eje" "text" DEFAULT ''::"text",
    "subj_od_esf" "text" DEFAULT ''::"text",
    "subj_od_cil" "text" DEFAULT ''::"text",
    "subj_od_eje" "text" DEFAULT ''::"text",
    "subj_oi_esf" "text" DEFAULT ''::"text",
    "subj_oi_cil" "text" DEFAULT ''::"text",
    "subj_oi_eje" "text" DEFAULT ''::"text",
    "receta_od_esf" "text" DEFAULT ''::"text",
    "receta_od_cil" "text" DEFAULT ''::"text",
    "receta_od_eje" "text" DEFAULT ''::"text",
    "receta_od_av" "text" DEFAULT ''::"text",
    "receta_oi_esf" "text" DEFAULT ''::"text",
    "receta_oi_cil" "text" DEFAULT ''::"text",
    "receta_oi_eje" "text" DEFAULT ''::"text",
    "receta_oi_av" "text" DEFAULT ''::"text",
    "add_cerca_od" "text" DEFAULT ''::"text",
    "add_cerca_oi" "text" DEFAULT ''::"text",
    "add_intermedia_od" "text" DEFAULT ''::"text",
    "add_intermedia_oi" "text" DEFAULT ''::"text",
    "add_av" "text" DEFAULT ''::"text",
    "dip_lejos" "text" DEFAULT ''::"text",
    "dip_cerca" "text" DEFAULT ''::"text",
    "dip_intermedio" "text" DEFAULT ''::"text",
    "diagnostico" "text" DEFAULT ''::"text",
    "diagnostico_od" "text" DEFAULT ''::"text",
    "diagnostico_oi" "text" DEFAULT ''::"text",
    "diagnostico_otros" "text" DEFAULT ''::"text",
    "plan_tratamiento" "text" DEFAULT ''::"text",
    "observaciones" "text" DEFAULT ''::"text",
    "proxima_fecha_control" "date",
    "proxima_cita" "date",
    "balance_od" boolean DEFAULT false,
    "balance_oi" boolean DEFAULT false,
    "otros_presbicia" boolean DEFAULT false,
    "otros_anisometropia" boolean DEFAULT false,
    "otros_ambliopia" boolean DEFAULT false,
    "lc_od_esf" "text" DEFAULT ''::"text",
    "lc_od_cil" "text" DEFAULT ''::"text",
    "lc_od_eje" "text" DEFAULT ''::"text",
    "lc_oi_esf" "text" DEFAULT ''::"text",
    "lc_oi_cil" "text" DEFAULT ''::"text",
    "lc_oi_eje" "text" DEFAULT ''::"text",
    "lc_radio_base_od" "text" DEFAULT ''::"text",
    "lc_diametro_od" "text" DEFAULT ''::"text",
    "lc_radio_base_oi" "text" DEFAULT ''::"text",
    "lc_diametro_oi" "text" DEFAULT ''::"text",
    "lc_laboratorio" "text" DEFAULT ''::"text",
    "lc_tipo_lente" "text" DEFAULT ''::"text",
    "lc_material" "text" DEFAULT ''::"text",
    "lc_observaciones" "text" DEFAULT ''::"text",
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "ph_od" "text" DEFAULT ''::"text",
    "ph_oi" "text" DEFAULT ''::"text",
    "kappa_od" "text" DEFAULT ''::"text",
    "kappa_oi" "text" DEFAULT ''::"text",
    "hirshberg" "text" DEFAULT ''::"text",
    "ducciones_od" "text" DEFAULT ''::"text",
    "ducciones_oi" "text" DEFAULT ''::"text",
    "versiones_ao" "text" DEFAULT ''::"text",
    "estereopsis_valor" "text" DEFAULT ''::"text",
    "estereopsis_segundos" "text" DEFAULT ''::"text",
    "lang" "text" DEFAULT ''::"text",
    "worth" "text" DEFAULT ''::"text",
    "ishihara" "text" DEFAULT ''::"text",
    "farnsworth" "text" DEFAULT ''::"text",
    "schirmer_od" "text" DEFAULT ''::"text",
    "schirmer_oi" "text" DEFAULT ''::"text",
    "osdi_puntuacion" integer,
    "osdi_clasificacion" "text" DEFAULT ''::"text",
    "sensibilidad_contraste" "text" DEFAULT ''::"text",
    "sensibilidad_frecuencia" "text" DEFAULT ''::"text",
    "amsler" "text" DEFAULT ''::"text",
    "campo_visual" "text" DEFAULT ''::"text",
    "campo_visual_descripcion" "text" DEFAULT ''::"text",
    "cover_test_6m" "text" DEFAULT ''::"text",
    "cover_test_40cm" "text" DEFAULT ''::"text",
    "cover_test_10cm" "text" DEFAULT ''::"text",
    "ppc_or" "text" DEFAULT ''::"text",
    "ppc_luz" "text" DEFAULT ''::"text",
    "ppc_frl" "text" DEFAULT ''::"text",
    "reflejo_fotomotor" "text" DEFAULT ''::"text",
    "reflejo_consensual" "text" DEFAULT ''::"text",
    "reflejo_acomodativo" "text" DEFAULT ''::"text",
    "k1_od" "text" DEFAULT ''::"text",
    "k2_od" "text" DEFAULT ''::"text",
    "k1_oi" "text" DEFAULT ''::"text",
    "k2_oi" "text" DEFAULT ''::"text",
    "prisma_od_valor" "text" DEFAULT ''::"text",
    "prisma_od_base" "text" DEFAULT ''::"text",
    "prisma_oi_valor" "text" DEFAULT ''::"text",
    "prisma_oi_base" "text" DEFAULT ''::"text",
    "auto_presbicia" boolean DEFAULT false,
    "auto_anisometropia" boolean DEFAULT false,
    "auto_ambliopia" boolean DEFAULT false,
    "lc_fecha_adaptacion" "date",
    "dip_lejos_mm" numeric(6,2),
    "dip_total_mm" numeric(6,2),
    "dnp_od_mm" numeric(6,2),
    "dnp_oi_mm" numeric(6,2),
    "cita_estado" "text" DEFAULT 'programada'::"text" NOT NULL,
    "updated_by" "uuid",
    "indicaciones" "text",
    CONSTRAINT "evaluaciones_dip_lejos_mm_chk" CHECK ((("dip_lejos_mm" IS NULL) OR ("dip_lejos_mm" >= (0)::numeric))),
    CONSTRAINT "evaluaciones_dip_total_mm_chk" CHECK ((("dip_total_mm" IS NULL) OR ("dip_total_mm" >= (0)::numeric))),
    CONSTRAINT "evaluaciones_dnp_od_mm_chk" CHECK ((("dnp_od_mm" IS NULL) OR ("dnp_od_mm" >= (0)::numeric))),
    CONSTRAINT "evaluaciones_dnp_oi_mm_chk" CHECK ((("dnp_oi_mm" IS NULL) OR ("dnp_oi_mm" >= (0)::numeric)))
);


ALTER TABLE "public"."evaluaciones" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."feedback_recomendaciones" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "recomendacion_id" "text" NOT NULL,
    "fue_util" boolean NOT NULL,
    "fecha" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."feedback_recomendaciones" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."gastos_operativos" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "categoria" "text" NOT NULL,
    "descripcion" "text",
    "monto" numeric NOT NULL,
    "fecha" "date" DEFAULT CURRENT_DATE NOT NULL,
    "fecha_programada" "date",
    "nota" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "es_recurrente" boolean DEFAULT false,
    "frecuencia" "text" DEFAULT 'mensual'::"text",
    CONSTRAINT "gastos_operativos_categoria_check" CHECK (("categoria" = ANY (ARRAY['alquiler'::"text", 'servicios'::"text", 'personal'::"text", 'proveedores'::"text", 'insumos'::"text", 'marketing'::"text", 'impuestos'::"text", 'otro'::"text"])))
);


ALTER TABLE "public"."gastos_operativos" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."inventario_fisico" (
    "id" "text" NOT NULL,
    "fecha" "date" DEFAULT CURRENT_DATE NOT NULL,
    "estado" "text" DEFAULT 'EN_PROGRESO'::"text" NOT NULL,
    "optica_id" "text" NOT NULL,
    "user_id" "text" NOT NULL,
    "notas" "text" DEFAULT ''::"text" NOT NULL
);


ALTER TABLE "public"."inventario_fisico" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."inventario_fisico_detalle" (
    "id" "text" NOT NULL,
    "inventario_id" "text" NOT NULL,
    "montura_id" "text" NOT NULL,
    "stock_sistema" integer NOT NULL,
    "stock_contado" integer,
    "diferencia" integer,
    "optica_id" "text" NOT NULL
);


ALTER TABLE "public"."inventario_fisico_detalle" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."invitaciones" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "codigo" "text" NOT NULL,
    "rol" "text" DEFAULT 'colaborador'::"text" NOT NULL,
    "creado_por" "uuid",
    "usado_por" "uuid",
    "usado_en" timestamp with time zone,
    "expira_en" timestamp with time zone DEFAULT ("now"() + '7 days'::interval) NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."invitaciones" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."margen_por_categoria" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "categoria_producto_id" "text" NOT NULL,
    "periodo" "text" NOT NULL,
    "tipo_periodo" "text" NOT NULL,
    "ventas_totales" numeric NOT NULL,
    "costo_total" numeric NOT NULL,
    "cantidad_ventas" integer NOT NULL,
    "margen_bruto" numeric NOT NULL,
    "margen_porcentaje" numeric NOT NULL,
    "ticket_promedio" numeric NOT NULL,
    "calculado_en" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "margen_por_categoria_tipo_periodo_check" CHECK (("tipo_periodo" = ANY (ARRAY['mensual'::"text", 'trimestral'::"text", 'anual'::"text"])))
);


ALTER TABLE "public"."margen_por_categoria" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."montura_movimientos" (
    "id" "text" NOT NULL,
    "montura_id" "text" NOT NULL,
    "fecha" "date" DEFAULT CURRENT_DATE NOT NULL,
    "tipo" "text" NOT NULL,
    "cantidad" integer NOT NULL,
    "stock_previo" integer NOT NULL,
    "stock_nuevo" integer NOT NULL,
    "referencia_id" "text" DEFAULT ''::"text" NOT NULL,
    "nota" "text" DEFAULT ''::"text" NOT NULL,
    "optica_id" "text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_by" "uuid",
    "user_id" "text" DEFAULT ''::"text" NOT NULL,
    "costo_unitario" double precision DEFAULT 0 NOT NULL,
    "tipo_documento" "text" DEFAULT ''::"text" NOT NULL
);


ALTER TABLE "public"."montura_movimientos" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."montura_proveedor" (
    "id" "text" NOT NULL,
    "montura_id" "text" NOT NULL,
    "proveedor_id" "text" NOT NULL,
    "costo_proveedor" double precision DEFAULT 0 NOT NULL,
    "precio_sugerido" double precision DEFAULT 0 NOT NULL,
    "activo" boolean DEFAULT true NOT NULL
);


ALTER TABLE "public"."montura_proveedor" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."monturas" (
    "id" "text" NOT NULL,
    "sku" "text" DEFAULT ''::"text" NOT NULL,
    "marca" "text" DEFAULT ''::"text" NOT NULL,
    "modelo" "text" DEFAULT ''::"text" NOT NULL,
    "color" "text" DEFAULT ''::"text" NOT NULL,
    "talla" "text" DEFAULT ''::"text" NOT NULL,
    "costo" numeric DEFAULT 0.00 NOT NULL,
    "precio" numeric DEFAULT 0.00 NOT NULL,
    "stock_actual" integer DEFAULT 0 NOT NULL,
    "stock_minimo" integer DEFAULT 0 NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "tipo_aro" "text" DEFAULT ''::"text" NOT NULL,
    "material_montura" "text" DEFAULT ''::"text" NOT NULL,
    "updated_by" "uuid",
    "categoria" "text" DEFAULT ''::"text" NOT NULL,
    "coleccion" "text" DEFAULT ''::"text" NOT NULL,
    "temporada" "text" DEFAULT ''::"text" NOT NULL,
    "estado_comercial" "text" DEFAULT ''::"text" NOT NULL,
    "genero" "text" DEFAULT ''::"text" NOT NULL,
    "ancho_mm" real,
    "puente_mm" real,
    "altura_mm" real,
    "imagen_uri" "text",
    CONSTRAINT "monturas_costo_chk" CHECK (("costo" >= (0)::numeric)),
    CONSTRAINT "monturas_precio_chk" CHECK (("precio" >= (0)::numeric)),
    CONSTRAINT "monturas_stock_actual_chk" CHECK (("stock_actual" >= 0)),
    CONSTRAINT "monturas_stock_actual_non_negative" CHECK (("stock_actual" >= 0)),
    CONSTRAINT "monturas_stock_minimo_chk" CHECK (("stock_minimo" >= 0))
);


ALTER TABLE "public"."monturas" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."user_profiles" (
    "user_id" "uuid" NOT NULL,
    "email" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL
);


ALTER TABLE "public"."user_profiles" OWNER TO "postgres";


COMMENT ON TABLE "public"."user_profiles" IS 'Perfil público mínimo para asignación de roles por email (sin exponer auth.users)';



CREATE TABLE IF NOT EXISTS "public"."usuario_optica" (
    "user_id" "uuid" NOT NULL,
    "optica_id" "text" NOT NULL,
    "rol" "text" DEFAULT 'admin'::"text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."usuario_optica" OWNER TO "postgres";


COMMENT ON TABLE "public"."usuario_optica" IS 'Membresía usuario ↔ óptica con rol';



CREATE OR REPLACE VIEW "public"."optica_members" WITH ("security_invoker"='true') AS
 SELECT "uo"."optica_id",
    "uo"."user_id",
    COALESCE("up"."email", ''::"text") AS "email",
    "uo"."rol",
    "uo"."created_at"
   FROM ("public"."usuario_optica" "uo"
     LEFT JOIN "public"."user_profiles" "up" ON (("up"."user_id" = "uo"."user_id")));


ALTER VIEW "public"."optica_members" OWNER TO "postgres";


COMMENT ON VIEW "public"."optica_members" IS 'Miembros por óptica con email visible para administración interna';



CREATE TABLE IF NOT EXISTS "public"."optica_settings" (
    "optica_id" "text" NOT NULL,
    "config_json" "jsonb" DEFAULT '{}'::"jsonb" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."optica_settings" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."opticas" (
    "id" "text" NOT NULL,
    "nombre" "text" DEFAULT ''::"text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "laboratorio_nombre" "text" DEFAULT ''::"text" NOT NULL,
    "laboratorio_contacto" "text" DEFAULT ''::"text" NOT NULL,
    "plan" "text" DEFAULT 'free'::"text" NOT NULL,
    "plan_code" "text" DEFAULT 'free'::"text" NOT NULL,
    "plan_source" "text" DEFAULT 'manual'::"text" NOT NULL,
    "plan_status" "text" DEFAULT 'active'::"text" NOT NULL,
    "current_period_end" timestamp with time zone,
    "fiscal_doc_tipo" "text" DEFAULT ''::"text" NOT NULL,
    "fiscal_doc_numero" "text" DEFAULT ''::"text" NOT NULL,
    "razon_social" "text" DEFAULT ''::"text" NOT NULL,
    "direccion_fiscal" "text" DEFAULT ''::"text" NOT NULL,
    "distrito_ciudad_departamento" "text",
    "moneda" "text",
    "pais" "text",
    "contacto_whatsapp_telefono" "text",
    "max_pacientes_por_optica" integer,
    "max_usuarios_por_optica" integer,
    CONSTRAINT "opticas_plan_code_chk" CHECK (("lower"(TRIM(BOTH FROM "plan_code")) = ANY (ARRAY['free'::"text", 'pro_individual'::"text", 'pro_multisite_15'::"text", 'enterprise'::"text", 'dev_owner'::"text"]))),
    CONSTRAINT "opticas_plan_source_chk" CHECK (("lower"(TRIM(BOTH FROM "plan_source")) = ANY (ARRAY['playstore'::"text", 'web'::"text", 'manual'::"text", 'internal'::"text"]))),
    CONSTRAINT "opticas_plan_status_chk" CHECK (("lower"(TRIM(BOTH FROM "plan_status")) = ANY (ARRAY['active'::"text", 'grace'::"text", 'canceled'::"text"])))
);


ALTER TABLE "public"."opticas" OWNER TO "postgres";


COMMENT ON TABLE "public"."opticas" IS 'Tenant (óptica); optica_id en tablas de negocio referencia opticas.id';



COMMENT ON COLUMN "public"."opticas"."laboratorio_nombre" IS 'Nombre del laboratorio (ticket / WhatsApp)';



COMMENT ON COLUMN "public"."opticas"."laboratorio_contacto" IS 'WhatsApp o teléfono del laboratorio';



COMMENT ON COLUMN "public"."opticas"."plan" IS 'DEPRECATED — usar plan_code. Columna legacy mantenida por compatibilidad con clientes viejos.';



COMMENT ON COLUMN "public"."opticas"."plan_code" IS 'Plan comercial/interno: free | pro_individual | pro_multisite_15 | enterprise | dev_owner';



COMMENT ON COLUMN "public"."opticas"."fiscal_doc_tipo" IS 'Tipo de documento fiscal: RUC o RUS';



COMMENT ON COLUMN "public"."opticas"."fiscal_doc_numero" IS 'Número del documento fiscal';



COMMENT ON COLUMN "public"."opticas"."razon_social" IS 'Razón social para emisión de comprobantes';



COMMENT ON COLUMN "public"."opticas"."direccion_fiscal" IS 'Dirección fiscal de la óptica';



CREATE TABLE IF NOT EXISTS "public"."orden_compra_items" (
    "id" "text" NOT NULL,
    "orden_id" "text" NOT NULL,
    "montura_id" "text" NOT NULL,
    "cantidad" integer NOT NULL,
    "costo_unitario" double precision DEFAULT 0 NOT NULL,
    "recibido" integer DEFAULT 0 NOT NULL,
    "optica_id" "text" NOT NULL
);


ALTER TABLE "public"."orden_compra_items" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."ordenes_compra" (
    "id" "text" NOT NULL,
    "numero" "text" NOT NULL,
    "proveedor_id" "text" NOT NULL,
    "fecha" "date" NOT NULL,
    "estado" "text" DEFAULT 'PENDIENTE'::"text" NOT NULL,
    "total" double precision DEFAULT 0 NOT NULL,
    "optica_id" "text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "updated_by" "text"
);


ALTER TABLE "public"."ordenes_compra" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."pacientes" (
    "id" "text" NOT NULL,
    "nombre_completo" "text" NOT NULL,
    "edad" integer DEFAULT 0 NOT NULL,
    "telefono" "text" DEFAULT ''::"text" NOT NULL,
    "fecha_creacion" "date" NOT NULL,
    "dni" "text",
    "fecha_nacimiento" "date",
    "sexo" "text",
    "email" "text",
    "direccion" "text",
    "distrito" "text",
    "ocupacion" "text",
    "acompanante" "text",
    "hobbies" "text",
    "ultimas_etiquetas" "text" DEFAULT ''::"text" NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "historia_optometrica" "text",
    "updated_by" "uuid"
);


ALTER TABLE "public"."pacientes" OWNER TO "postgres";


COMMENT ON COLUMN "public"."pacientes"."historia_optometrica" IS 'Número de historia optométrica del paciente (opcional).';



CREATE TABLE IF NOT EXISTS "public"."pacientes_delete_audit" (
    "id" bigint NOT NULL,
    "optica_id" "text" NOT NULL,
    "paciente_id" "text" NOT NULL,
    "deleted_by" "uuid" NOT NULL,
    "deleted_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL
);


ALTER TABLE "public"."pacientes_delete_audit" OWNER TO "postgres";


CREATE SEQUENCE IF NOT EXISTS "public"."pacientes_delete_audit_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE "public"."pacientes_delete_audit_id_seq" OWNER TO "postgres";


ALTER SEQUENCE "public"."pacientes_delete_audit_id_seq" OWNED BY "public"."pacientes_delete_audit"."id";



CREATE TABLE IF NOT EXISTS "public"."pagos" (
    "id" "text" NOT NULL,
    "dispensacion_id" "text",
    "servicio_extra_id" "text",
    "fecha" "date" NOT NULL,
    "tipo" "text" DEFAULT ''::"text" NOT NULL,
    "monto" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "metodo_pago" "text" DEFAULT ''::"text" NOT NULL,
    "nota" "text" DEFAULT ''::"text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "updated_by" "uuid",
    "venta_id" "text",
    CONSTRAINT "pagos_metodo_pago_not_blank_chk" CHECK (("btrim"("metodo_pago") <> ''::"text")),
    CONSTRAINT "pagos_monto_chk" CHECK (("monto" >= (0)::numeric)),
    CONSTRAINT "pagos_optica_id_not_blank_chk" CHECK (("btrim"("optica_id") <> ''::"text")),
    CONSTRAINT "pagos_origen_xor_chk" CHECK (((("dispensacion_id" IS NOT NULL) AND ("servicio_extra_id" IS NULL)) OR (("dispensacion_id" IS NULL) AND ("servicio_extra_id" IS NOT NULL)))),
    CONSTRAINT "pagos_tipo_not_blank_chk" CHECK (("btrim"("tipo") <> ''::"text"))
);


ALTER TABLE "public"."pagos" OWNER TO "postgres";


COMMENT ON TABLE "public"."pagos" IS 'venta_id integrity enforced at app layer (Android ViewModels). FK dropped due to async REST sync architecture.';



COMMENT ON CONSTRAINT "pagos_metodo_pago_not_blank_chk" ON "public"."pagos" IS 'Evita método de pago vacío en pagos (P3-T3).';



COMMENT ON CONSTRAINT "pagos_optica_id_not_blank_chk" ON "public"."pagos" IS 'Evita tenant vacío en pagos (P3-T3).';



COMMENT ON CONSTRAINT "pagos_tipo_not_blank_chk" ON "public"."pagos" IS 'Evita tipo vacío en pagos (P3-T3).';



CREATE TABLE IF NOT EXISTS "public"."pin_attempts" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "limit_key" "text" NOT NULL,
    "attempts" integer DEFAULT 1 NOT NULL,
    "window_start" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."pin_attempts" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."proveedores" (
    "id" "text" NOT NULL,
    "nombre" "text" NOT NULL,
    "ruc" "text" NOT NULL,
    "telefono" "text" DEFAULT ''::"text" NOT NULL,
    "email" "text" DEFAULT ''::"text" NOT NULL,
    "direccion" "text" DEFAULT ''::"text" NOT NULL,
    "contacto" "text" DEFAULT ''::"text" NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "optica_id" "text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "updated_by" "text"
);


ALTER TABLE "public"."proveedores" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."regalos_dispensacion" (
    "id" "text" NOT NULL,
    "dispensacion_id" "text" NOT NULL,
    "producto_id" "text" NOT NULL,
    "cantidad" integer DEFAULT 1 NOT NULL,
    "costo_unitario" real NOT NULL,
    "descripcion" "text" NOT NULL,
    "motivo" "text" DEFAULT ''::"text" NOT NULL,
    "optica_id" "text" NOT NULL
);


ALTER TABLE "public"."regalos_dispensacion" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."resumen_diario" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "optica_id" "text" NOT NULL,
    "fecha" "date" NOT NULL,
    "ventas_cantidad" integer DEFAULT 0 NOT NULL,
    "ventas_monto_total" numeric DEFAULT 0 NOT NULL,
    "ventas_costo_total" numeric DEFAULT 0 NOT NULL,
    "cobros_cantidad" integer DEFAULT 0 NOT NULL,
    "cobros_monto_total" numeric DEFAULT 0 NOT NULL,
    "saldo_pendiente_total" numeric DEFAULT 0 NOT NULL,
    "saldo_pendiente_cantidad" integer DEFAULT 0 NOT NULL,
    "inventario_valor" numeric,
    "inventario_unidades" integer,
    "calculado_en" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."resumen_diario" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."schema_migrations_flags" (
    "key" "text" NOT NULL,
    "applied_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."schema_migrations_flags" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."servicios_extra" (
    "id" "text" NOT NULL,
    "ot" "text" DEFAULT ''::"text" NOT NULL,
    "descripcion" "text" DEFAULT ''::"text" NOT NULL,
    "monto_total" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "a_cuenta" numeric(10,2) DEFAULT 0.00 NOT NULL,
    "estado" "text" DEFAULT 'Pendiente'::"text" NOT NULL,
    "fecha" "date" NOT NULL,
    "paciente_id" "text",
    "metodo_pago" "text" DEFAULT ''::"text" NOT NULL,
    "optica_id" "text" DEFAULT 'mi_optica_base'::"text" NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_by" "uuid",
    "fecha_entrega" "date",
    CONSTRAINT "servicios_extra_a_cuenta_chk" CHECK (("a_cuenta" >= (0)::numeric)),
    CONSTRAINT "servicios_extra_descripcion_not_blank_chk" CHECK (("btrim"("descripcion") <> ''::"text")),
    CONSTRAINT "servicios_extra_estado_domain_chk" CHECK (("estado" = ANY (ARRAY['Pendiente'::"text", 'Entregado'::"text"]))),
    CONSTRAINT "servicios_extra_estado_not_blank_chk" CHECK (("btrim"("estado") <> ''::"text")),
    CONSTRAINT "servicios_extra_metodo_pago_not_blank_chk" CHECK (("btrim"("metodo_pago") <> ''::"text")),
    CONSTRAINT "servicios_extra_monto_total_chk" CHECK (("monto_total" >= (0)::numeric)),
    CONSTRAINT "servicios_extra_optica_id_not_blank_chk" CHECK (("btrim"("optica_id") <> ''::"text")),
    CONSTRAINT "servicios_extra_ot_not_dash_placeholder_chk" CHECK (("btrim"(COALESCE("ot", ''::"text")) <> '-'::"text"))
);


ALTER TABLE "public"."servicios_extra" OWNER TO "postgres";


COMMENT ON COLUMN "public"."servicios_extra"."paciente_id" IS 'FK opcional: puede ser NULL para servicios no asociados a un paciente.';



COMMENT ON COLUMN "public"."servicios_extra"."fecha_entrega" IS 'Fecha de entrega efectiva del servicio extra';



COMMENT ON CONSTRAINT "servicios_extra_descripcion_not_blank_chk" ON "public"."servicios_extra" IS 'Evita descripciones vacías o con solo espacios (P3-T2).';



COMMENT ON CONSTRAINT "servicios_extra_estado_not_blank_chk" ON "public"."servicios_extra" IS 'Evita estado vacío; la app usa Pendiente/Entregado (P3-T2).';



COMMENT ON CONSTRAINT "servicios_extra_metodo_pago_not_blank_chk" ON "public"."servicios_extra" IS 'Evita método de pago vacío en fila agregada de servicio extra (P3-T2).';



COMMENT ON CONSTRAINT "servicios_extra_optica_id_not_blank_chk" ON "public"."servicios_extra" IS 'Evita tenant vacío para servicios_extra (P3-T2).';



COMMENT ON CONSTRAINT "servicios_extra_ot_not_dash_placeholder_chk" ON "public"."servicios_extra" IS 'Evita usar ''-'' como placeholder de OT vacia; usar cadena vacia.';



CREATE TABLE IF NOT EXISTS "public"."sync_telemetry_optica" (
    "optica_id" "text" NOT NULL,
    "last_sync_at" timestamp with time zone,
    "last_status" "text" DEFAULT 'idle'::"text" NOT NULL,
    "last_stage" "text" DEFAULT ''::"text" NOT NULL,
    "last_error" "text" DEFAULT ''::"text" NOT NULL,
    "last_actor" "uuid",
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "sync_telemetry_optica_last_status_check" CHECK (("last_status" = ANY (ARRAY['idle'::"text", 'ok'::"text", 'error'::"text"])))
);


ALTER TABLE "public"."sync_telemetry_optica" OWNER TO "postgres";


COMMENT ON TABLE "public"."sync_telemetry_optica" IS 'Telemetría resumida de última sincronización por óptica (uso operativo/soporte).';



ALTER TABLE ONLY "public"."pacientes_delete_audit" ALTER COLUMN "id" SET DEFAULT "nextval"('"public"."pacientes_delete_audit_id_seq"'::"regclass");



ALTER TABLE ONLY "public"."app_releases"
    ADD CONSTRAINT "app_releases_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."app_releases"
    ADD CONSTRAINT "app_releases_version_key" UNIQUE ("version");



ALTER TABLE ONLY "public"."categorias_montura"
    ADD CONSTRAINT "categorias_montura_nombre_optica_id_key" UNIQUE ("nombre", "optica_id");



ALTER TABLE ONLY "public"."categorias_montura"
    ADD CONSTRAINT "categorias_montura_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."categorias_producto"
    ADD CONSTRAINT "categorias_producto_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."cierres_caja"
    ADD CONSTRAINT "cierres_caja_optica_id_fecha_operativa_key" UNIQUE ("optica_id", "fecha_operativa");



ALTER TABLE ONLY "public"."cierres_caja"
    ADD CONSTRAINT "cierres_caja_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."configuracion_financiera"
    ADD CONSTRAINT "configuracion_financiera_pkey" PRIMARY KEY ("optica_id");



ALTER TABLE ONLY "public"."costos_productos"
    ADD CONSTRAINT "costos_productos_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."dispensacion_items"
    ADD CONSTRAINT "dispensacion_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."dispensaciones"
    ADD CONSTRAINT "dispensaciones_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."evaluaciones"
    ADD CONSTRAINT "evaluaciones_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."feedback_recomendaciones"
    ADD CONSTRAINT "feedback_recomendaciones_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."gastos_operativos"
    ADD CONSTRAINT "gastos_operativos_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."inventario_fisico_detalle"
    ADD CONSTRAINT "inventario_fisico_detalle_inventario_id_montura_id_key" UNIQUE ("inventario_id", "montura_id");



ALTER TABLE ONLY "public"."inventario_fisico_detalle"
    ADD CONSTRAINT "inventario_fisico_detalle_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."inventario_fisico"
    ADD CONSTRAINT "inventario_fisico_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."invitaciones"
    ADD CONSTRAINT "invitaciones_codigo_key" UNIQUE ("codigo");



ALTER TABLE ONLY "public"."invitaciones"
    ADD CONSTRAINT "invitaciones_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."margen_por_categoria"
    ADD CONSTRAINT "margen_por_categoria_optica_id_categoria_producto_id_period_key" UNIQUE ("optica_id", "categoria_producto_id", "periodo", "tipo_periodo");



ALTER TABLE ONLY "public"."margen_por_categoria"
    ADD CONSTRAINT "margen_por_categoria_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."montura_movimientos"
    ADD CONSTRAINT "montura_movimientos_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."montura_proveedor"
    ADD CONSTRAINT "montura_proveedor_montura_id_proveedor_id_key" UNIQUE ("montura_id", "proveedor_id");



ALTER TABLE ONLY "public"."montura_proveedor"
    ADD CONSTRAINT "montura_proveedor_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."monturas"
    ADD CONSTRAINT "monturas_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."optica_settings"
    ADD CONSTRAINT "optica_settings_pkey" PRIMARY KEY ("optica_id");



ALTER TABLE ONLY "public"."opticas"
    ADD CONSTRAINT "opticas_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."orden_compra_items"
    ADD CONSTRAINT "orden_compra_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."ordenes_compra"
    ADD CONSTRAINT "ordenes_compra_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pacientes_delete_audit"
    ADD CONSTRAINT "pacientes_delete_audit_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pacientes"
    ADD CONSTRAINT "pacientes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pagos"
    ADD CONSTRAINT "pagos_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pin_attempts"
    ADD CONSTRAINT "pin_attempts_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."proveedores"
    ADD CONSTRAINT "proveedores_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."proveedores"
    ADD CONSTRAINT "proveedores_ruc_optica_id_key" UNIQUE ("ruc", "optica_id");



ALTER TABLE ONLY "public"."regalos_dispensacion"
    ADD CONSTRAINT "regalos_dispensacion_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."resumen_diario"
    ADD CONSTRAINT "resumen_diario_optica_id_fecha_key" UNIQUE ("optica_id", "fecha");



ALTER TABLE ONLY "public"."resumen_diario"
    ADD CONSTRAINT "resumen_diario_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."schema_migrations_flags"
    ADD CONSTRAINT "schema_migrations_flags_pkey" PRIMARY KEY ("key");



ALTER TABLE ONLY "public"."servicios_extra"
    ADD CONSTRAINT "servicios_extra_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."sync_telemetry_optica"
    ADD CONSTRAINT "sync_telemetry_optica_pkey" PRIMARY KEY ("optica_id");



ALTER TABLE ONLY "public"."user_profiles"
    ADD CONSTRAINT "user_profiles_email_key" UNIQUE ("email");



ALTER TABLE ONLY "public"."user_profiles"
    ADD CONSTRAINT "user_profiles_pkey" PRIMARY KEY ("user_id");



ALTER TABLE ONLY "public"."usuario_optica"
    ADD CONSTRAINT "usuario_optica_pkey" PRIMARY KEY ("user_id", "optica_id");



CREATE INDEX "idx_app_releases_version" ON "public"."app_releases" USING "btree" ("version" DESC);



CREATE INDEX "idx_categorias_montura_optica_id" ON "public"."categorias_montura" USING "btree" ("optica_id");



CREATE INDEX "idx_cierres_caja_optica_fecha" ON "public"."cierres_caja" USING "btree" ("optica_id", "fecha_operativa" DESC);



CREATE INDEX "idx_costos_vigentes" ON "public"."costos_productos" USING "btree" ("optica_id", "categoria_producto_id") WHERE ("vigente_hasta" IS NULL);



CREATE INDEX "idx_dispensacion_items_dispensacion_id" ON "public"."dispensacion_items" USING "btree" ("dispensacion_id");



CREATE INDEX "idx_dispensacion_items_optica_id" ON "public"."dispensacion_items" USING "btree" ("optica_id");



CREATE INDEX "idx_dispensaciones_optica_fecha" ON "public"."dispensaciones" USING "btree" ("optica_id", "fecha" DESC);



CREATE INDEX "idx_dispensaciones_paciente_id" ON "public"."dispensaciones" USING "btree" ("paciente_id");



CREATE INDEX "idx_evaluaciones_optica_fecha" ON "public"."evaluaciones" USING "btree" ("optica_id", "fecha" DESC);



CREATE INDEX "idx_evaluaciones_paciente_id" ON "public"."evaluaciones" USING "btree" ("paciente_id");



CREATE INDEX "idx_gastos_opt_fecha" ON "public"."gastos_operativos" USING "btree" ("optica_id", "fecha");



CREATE INDEX "idx_inventario_fisico_detalle_optica_id" ON "public"."inventario_fisico_detalle" USING "btree" ("optica_id");



CREATE INDEX "idx_inventario_fisico_optica_id" ON "public"."inventario_fisico" USING "btree" ("optica_id");



CREATE INDEX "idx_invitaciones_optica" ON "public"."invitaciones" USING "btree" ("optica_id");



CREATE INDEX "idx_margen_cat_opt_per" ON "public"."margen_por_categoria" USING "btree" ("optica_id", "periodo");



CREATE INDEX "idx_montura_movimientos_montura_id" ON "public"."montura_movimientos" USING "btree" ("montura_id");



CREATE INDEX "idx_montura_movimientos_optica_id" ON "public"."montura_movimientos" USING "btree" ("optica_id");



CREATE INDEX "idx_monturas_categoria" ON "public"."monturas" USING "btree" ("categoria");



CREATE INDEX "idx_monturas_estado_comercial" ON "public"."monturas" USING "btree" ("estado_comercial");



CREATE INDEX "idx_monturas_optica_id" ON "public"."monturas" USING "btree" ("optica_id");



CREATE UNIQUE INDEX "idx_monturas_sku_optica" ON "public"."monturas" USING "btree" ("optica_id", "sku");



CREATE UNIQUE INDEX "idx_movimientos_conflict" ON "public"."montura_movimientos" USING "btree" ("referencia_id", "tipo", "montura_id");



CREATE INDEX "idx_orden_compra_items_optica_id" ON "public"."orden_compra_items" USING "btree" ("optica_id");



CREATE INDEX "idx_ordenes_compra_estado" ON "public"."ordenes_compra" USING "btree" ("estado");



CREATE INDEX "idx_ordenes_compra_optica_id" ON "public"."ordenes_compra" USING "btree" ("optica_id");



CREATE INDEX "idx_pacientes_delete_audit_optica_day" ON "public"."pacientes_delete_audit" USING "btree" ("optica_id", "deleted_by", "deleted_at");



CREATE INDEX "idx_pacientes_optica_id" ON "public"."pacientes" USING "btree" ("optica_id");



CREATE INDEX "idx_pacientes_optica_updated_at" ON "public"."pacientes" USING "btree" ("optica_id", "updated_at" DESC);



CREATE INDEX "idx_pagos_dispensacion_id" ON "public"."pagos" USING "btree" ("dispensacion_id");



CREATE INDEX "idx_pagos_optica_fecha" ON "public"."pagos" USING "btree" ("optica_id", "fecha" DESC);



CREATE INDEX "idx_pagos_servicio_extra_id" ON "public"."pagos" USING "btree" ("servicio_extra_id");



CREATE INDEX "idx_pagos_venta" ON "public"."pagos" USING "btree" ("venta_id");



CREATE INDEX "idx_pin_attempts_key_window" ON "public"."pin_attempts" USING "btree" ("limit_key", "window_start");



CREATE INDEX "idx_proveedores_optica_id" ON "public"."proveedores" USING "btree" ("optica_id");



CREATE INDEX "idx_regalos_disp" ON "public"."regalos_dispensacion" USING "btree" ("dispensacion_id");



CREATE INDEX "idx_resumen_diario_opt_fecha" ON "public"."resumen_diario" USING "btree" ("optica_id", "fecha");



CREATE INDEX "idx_servicios_extra_optica_fecha" ON "public"."servicios_extra" USING "btree" ("optica_id", "fecha" DESC);



CREATE INDEX "idx_servicios_extra_paciente_id" ON "public"."servicios_extra" USING "btree" ("paciente_id");



CREATE INDEX "idx_usuario_optica_optica" ON "public"."usuario_optica" USING "btree" ("optica_id");



CREATE INDEX "idx_usuario_optica_user" ON "public"."usuario_optica" USING "btree" ("user_id");



CREATE UNIQUE INDEX "pacientes_optica_historia_optometrica_uq" ON "public"."pacientes" USING "btree" ("optica_id", "upper"("btrim"("historia_optometrica"))) WHERE (NULLIF("btrim"(COALESCE("historia_optometrica", ''::"text")), ''::"text") IS NOT NULL);



CREATE UNIQUE INDEX "servicios_extra_optica_ot_uq" ON "public"."servicios_extra" USING "btree" ("optica_id", "upper"("btrim"("ot"))) WHERE (NULLIF("btrim"(COALESCE("ot", ''::"text")), ''::"text") IS NOT NULL);



CREATE OR REPLACE TRIGGER "dispensaciones_updated_at" BEFORE UPDATE ON "public"."dispensaciones" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "evaluaciones_updated_at" BEFORE UPDATE ON "public"."evaluaciones" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "pacientes_updated_at" BEFORE UPDATE ON "public"."pacientes" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "pagos_updated_at" BEFORE UPDATE ON "public"."pagos" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "servicios_extra_updated_at" BEFORE UPDATE ON "public"."servicios_extra" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "trg_cierres_caja_updated_at" BEFORE UPDATE ON "public"."cierres_caja" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "trg_dispensaciones_set_updated_audit" BEFORE UPDATE ON "public"."dispensaciones" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_evaluaciones_set_updated_audit" BEFORE UPDATE ON "public"."evaluaciones" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_guard_opticas_fiscal_update" BEFORE UPDATE OF "fiscal_doc_tipo", "fiscal_doc_numero", "razon_social", "direccion_fiscal" ON "public"."opticas" FOR EACH ROW EXECUTE FUNCTION "public"."guard_opticas_fiscal_update"();



CREATE OR REPLACE TRIGGER "trg_guard_pacientes_delete" BEFORE DELETE ON "public"."pacientes" FOR EACH ROW EXECUTE FUNCTION "public"."guard_pacientes_delete"();



CREATE OR REPLACE TRIGGER "trg_montura_movimientos_set_updated_audit" BEFORE UPDATE ON "public"."montura_movimientos" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_monturas_set_updated_audit" BEFORE UPDATE ON "public"."monturas" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_optica_settings_updated_at" BEFORE UPDATE ON "public"."optica_settings" FOR EACH ROW EXECUTE FUNCTION "public"."update_updated_at"();



CREATE OR REPLACE TRIGGER "trg_opticas_dev_owner_guard" BEFORE INSERT OR UPDATE OF "plan_code", "plan_source" ON "public"."opticas" FOR EACH ROW EXECUTE FUNCTION "public"."enforce_dev_owner_guard"();



CREATE OR REPLACE TRIGGER "trg_opticas_limit_guard" BEFORE INSERT ON "public"."opticas" FOR EACH ROW EXECUTE FUNCTION "public"."enforce_optica_limit_for_creator"();



CREATE OR REPLACE TRIGGER "trg_pacientes_set_updated_audit" BEFORE UPDATE ON "public"."pacientes" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_pagos_before_insert_venta_id" BEFORE INSERT ON "public"."pagos" FOR EACH ROW EXECUTE FUNCTION "public"."trg_pagos_set_venta_id"();



CREATE OR REPLACE TRIGGER "trg_pagos_maintain_monto_pagado" AFTER INSERT OR DELETE OR UPDATE ON "public"."pagos" FOR EACH ROW EXECUTE FUNCTION "public"."trg_pagos_update_monto_pagado"();



CREATE OR REPLACE TRIGGER "trg_pagos_set_updated_audit" BEFORE UPDATE ON "public"."pagos" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_servicios_extra_set_updated_audit" BEFORE UPDATE ON "public"."servicios_extra" FOR EACH ROW EXECUTE FUNCTION "public"."set_updated_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_sync_telemetry_optica_audit" BEFORE INSERT OR UPDATE ON "public"."sync_telemetry_optica" FOR EACH ROW EXECUTE FUNCTION "public"."set_sync_telemetry_audit_fields"();



CREATE OR REPLACE TRIGGER "trg_sync_telemetry_optica_updated_at" BEFORE UPDATE ON "public"."sync_telemetry_optica" FOR EACH ROW EXECUTE FUNCTION "public"."set_sync_telemetry_updated_at"();



CREATE OR REPLACE TRIGGER "trg_usuario_optica_admin_role_guard" BEFORE INSERT OR UPDATE ON "public"."usuario_optica" FOR EACH ROW EXECUTE FUNCTION "public"."enforce_admin_role_assignment_guard"();



CREATE OR REPLACE TRIGGER "trg_usuario_optica_dev_owner_guard" BEFORE INSERT OR UPDATE ON "public"."usuario_optica" FOR EACH ROW EXECUTE FUNCTION "public"."enforce_dev_owner_membership_guard"();



ALTER TABLE ONLY "public"."categorias_montura"
    ADD CONSTRAINT "categorias_montura_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."cierres_caja"
    ADD CONSTRAINT "cierres_caja_closed_by_fkey" FOREIGN KEY ("closed_by") REFERENCES "auth"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."cierres_caja"
    ADD CONSTRAINT "cierres_caja_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."cierres_caja"
    ADD CONSTRAINT "cierres_caja_reopened_by_fkey" FOREIGN KEY ("reopened_by") REFERENCES "auth"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."configuracion_financiera"
    ADD CONSTRAINT "configuracion_financiera_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."costos_productos"
    ADD CONSTRAINT "costos_productos_categoria_producto_id_fkey" FOREIGN KEY ("categoria_producto_id") REFERENCES "public"."categorias_producto"("id");



ALTER TABLE ONLY "public"."costos_productos"
    ADD CONSTRAINT "costos_productos_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."dispensacion_items"
    ADD CONSTRAINT "dispensacion_items_dispensacion_id_fkey" FOREIGN KEY ("dispensacion_id") REFERENCES "public"."dispensaciones"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."dispensaciones"
    ADD CONSTRAINT "dispensaciones_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."dispensaciones"
    ADD CONSTRAINT "dispensaciones_paciente_id_fkey" FOREIGN KEY ("paciente_id") REFERENCES "public"."pacientes"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."evaluaciones"
    ADD CONSTRAINT "evaluaciones_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."evaluaciones"
    ADD CONSTRAINT "evaluaciones_paciente_id_fkey" FOREIGN KEY ("paciente_id") REFERENCES "public"."pacientes"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."feedback_recomendaciones"
    ADD CONSTRAINT "feedback_recomendaciones_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."gastos_operativos"
    ADD CONSTRAINT "gastos_operativos_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."inventario_fisico_detalle"
    ADD CONSTRAINT "inventario_fisico_detalle_inventario_id_fkey" FOREIGN KEY ("inventario_id") REFERENCES "public"."inventario_fisico"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."inventario_fisico_detalle"
    ADD CONSTRAINT "inventario_fisico_detalle_montura_id_fkey" FOREIGN KEY ("montura_id") REFERENCES "public"."monturas"("id");



ALTER TABLE ONLY "public"."inventario_fisico"
    ADD CONSTRAINT "inventario_fisico_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."invitaciones"
    ADD CONSTRAINT "invitaciones_creado_por_fkey" FOREIGN KEY ("creado_por") REFERENCES "auth"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."invitaciones"
    ADD CONSTRAINT "invitaciones_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."invitaciones"
    ADD CONSTRAINT "invitaciones_usado_por_fkey" FOREIGN KEY ("usado_por") REFERENCES "auth"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."margen_por_categoria"
    ADD CONSTRAINT "margen_por_categoria_categoria_producto_id_fkey" FOREIGN KEY ("categoria_producto_id") REFERENCES "public"."categorias_producto"("id");



ALTER TABLE ONLY "public"."margen_por_categoria"
    ADD CONSTRAINT "margen_por_categoria_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."montura_movimientos"
    ADD CONSTRAINT "montura_movimientos_montura_id_fkey" FOREIGN KEY ("montura_id") REFERENCES "public"."monturas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."montura_movimientos"
    ADD CONSTRAINT "montura_movimientos_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."montura_proveedor"
    ADD CONSTRAINT "montura_proveedor_montura_id_fkey" FOREIGN KEY ("montura_id") REFERENCES "public"."monturas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."montura_proveedor"
    ADD CONSTRAINT "montura_proveedor_proveedor_id_fkey" FOREIGN KEY ("proveedor_id") REFERENCES "public"."proveedores"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."monturas"
    ADD CONSTRAINT "monturas_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."optica_settings"
    ADD CONSTRAINT "optica_settings_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."orden_compra_items"
    ADD CONSTRAINT "orden_compra_items_montura_id_fkey" FOREIGN KEY ("montura_id") REFERENCES "public"."monturas"("id");



ALTER TABLE ONLY "public"."orden_compra_items"
    ADD CONSTRAINT "orden_compra_items_orden_id_fkey" FOREIGN KEY ("orden_id") REFERENCES "public"."ordenes_compra"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."ordenes_compra"
    ADD CONSTRAINT "ordenes_compra_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."ordenes_compra"
    ADD CONSTRAINT "ordenes_compra_proveedor_id_fkey" FOREIGN KEY ("proveedor_id") REFERENCES "public"."proveedores"("id");



ALTER TABLE ONLY "public"."pacientes_delete_audit"
    ADD CONSTRAINT "pacientes_delete_audit_deleted_by_fkey" FOREIGN KEY ("deleted_by") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pacientes_delete_audit"
    ADD CONSTRAINT "pacientes_delete_audit_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pacientes"
    ADD CONSTRAINT "pacientes_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."pagos"
    ADD CONSTRAINT "pagos_dispensacion_id_fkey" FOREIGN KEY ("dispensacion_id") REFERENCES "public"."dispensaciones"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pagos"
    ADD CONSTRAINT "pagos_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."pagos"
    ADD CONSTRAINT "pagos_servicio_extra_id_fkey" FOREIGN KEY ("servicio_extra_id") REFERENCES "public"."servicios_extra"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."proveedores"
    ADD CONSTRAINT "proveedores_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."regalos_dispensacion"
    ADD CONSTRAINT "regalos_dispensacion_dispensacion_id_fkey" FOREIGN KEY ("dispensacion_id") REFERENCES "public"."dispensaciones"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."regalos_dispensacion"
    ADD CONSTRAINT "regalos_dispensacion_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."resumen_diario"
    ADD CONSTRAINT "resumen_diario_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id");



ALTER TABLE ONLY "public"."servicios_extra"
    ADD CONSTRAINT "servicios_extra_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE RESTRICT;



ALTER TABLE ONLY "public"."servicios_extra"
    ADD CONSTRAINT "servicios_extra_paciente_id_fkey" FOREIGN KEY ("paciente_id") REFERENCES "public"."pacientes"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."sync_telemetry_optica"
    ADD CONSTRAINT "sync_telemetry_optica_last_actor_fkey" FOREIGN KEY ("last_actor") REFERENCES "auth"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."sync_telemetry_optica"
    ADD CONSTRAINT "sync_telemetry_optica_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."user_profiles"
    ADD CONSTRAINT "user_profiles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."usuario_optica"
    ADD CONSTRAINT "usuario_optica_optica_id_fkey" FOREIGN KEY ("optica_id") REFERENCES "public"."opticas"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."usuario_optica"
    ADD CONSTRAINT "usuario_optica_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



CREATE POLICY "Anyone can read releases" ON "public"."app_releases" FOR SELECT TO "authenticated", "anon" USING (true);



CREATE POLICY "Service role can insert releases" ON "public"."app_releases" FOR INSERT TO "service_role" WITH CHECK (true);



CREATE POLICY "Service role can update releases" ON "public"."app_releases" FOR UPDATE TO "service_role" USING (true);



ALTER TABLE "public"."app_releases" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."categorias_montura" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "categorias_montura_delete" ON "public"."categorias_montura" FOR DELETE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "categorias_montura_insert" ON "public"."categorias_montura" FOR INSERT WITH CHECK (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "categorias_montura_select" ON "public"."categorias_montura" FOR SELECT USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "categorias_montura_update" ON "public"."categorias_montura" FOR UPDATE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



ALTER TABLE "public"."categorias_producto" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "categorias_producto_delete" ON "public"."categorias_producto" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica"
  WHERE (("usuario_optica"."user_id" = "auth"."uid"()) AND ("lower"(TRIM(BOTH FROM "usuario_optica"."rol")) = 'admin'::"text")))));



CREATE POLICY "categorias_producto_insert" ON "public"."categorias_producto" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica"
  WHERE (("usuario_optica"."user_id" = "auth"."uid"()) AND ("lower"(TRIM(BOTH FROM "usuario_optica"."rol")) = 'admin'::"text")))));



CREATE POLICY "categorias_producto_select" ON "public"."categorias_producto" FOR SELECT USING (true);



ALTER TABLE "public"."cierres_caja" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "cierres_caja_delete_admin_manager" ON "public"."cierres_caja" FOR DELETE TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "cierres_caja"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



CREATE POLICY "cierres_caja_insert_admin_manager" ON "public"."cierres_caja" FOR INSERT TO "authenticated" WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "cierres_caja"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



CREATE POLICY "cierres_caja_select_member" ON "public"."cierres_caja" FOR SELECT TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "cierres_caja"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid"))))));



CREATE POLICY "cierres_caja_update_admin_manager" ON "public"."cierres_caja" FOR UPDATE TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "cierres_caja"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"])))))) WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "cierres_caja"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



ALTER TABLE "public"."configuracion_financiera" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "configuracion_financiera_delete" ON "public"."configuracion_financiera" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text"]));



CREATE POLICY "configuracion_financiera_insert" ON "public"."configuracion_financiera" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "configuracion_financiera_select" ON "public"."configuracion_financiera" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "configuracion_financiera_update" ON "public"."configuracion_financiera" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



ALTER TABLE "public"."costos_productos" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "costos_productos_delete" ON "public"."costos_productos" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text"]));



CREATE POLICY "costos_productos_insert" ON "public"."costos_productos" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "costos_productos_select" ON "public"."costos_productos" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "costos_productos_update" ON "public"."costos_productos" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



ALTER TABLE "public"."dispensacion_items" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "dispensacion_items_delete" ON "public"."dispensacion_items" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "dispensacion_items_insert" ON "public"."dispensacion_items" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "dispensacion_items_select" ON "public"."dispensacion_items" FOR SELECT USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "dispensacion_items_update" ON "public"."dispensacion_items" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



ALTER TABLE "public"."dispensaciones" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "dispensaciones_delete" ON "public"."dispensaciones" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "dispensaciones_insert" ON "public"."dispensaciones" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "dispensaciones_select" ON "public"."dispensaciones" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "dispensaciones_update" ON "public"."dispensaciones" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



ALTER TABLE "public"."evaluaciones" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "evaluaciones_delete" ON "public"."evaluaciones" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "evaluaciones_insert" ON "public"."evaluaciones" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text"]));



CREATE POLICY "evaluaciones_select" ON "public"."evaluaciones" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "evaluaciones_update" ON "public"."evaluaciones" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text"]));



ALTER TABLE "public"."feedback_recomendaciones" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "feedback_recomendaciones_insert" ON "public"."feedback_recomendaciones" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "feedback_recomendaciones_select" ON "public"."feedback_recomendaciones" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



ALTER TABLE "public"."gastos_operativos" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "gastos_operativos_delete" ON "public"."gastos_operativos" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "gastos_operativos_insert" ON "public"."gastos_operativos" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "gastos_operativos_select" ON "public"."gastos_operativos" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "gastos_operativos_update" ON "public"."gastos_operativos" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "if_delete" ON "public"."inventario_fisico" FOR DELETE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "if_insert" ON "public"."inventario_fisico" FOR INSERT WITH CHECK (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "if_select" ON "public"."inventario_fisico" FOR SELECT USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "if_update" ON "public"."inventario_fisico" FOR UPDATE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "ifd_delete" ON "public"."inventario_fisico_detalle" FOR DELETE USING (("inventario_id" IN ( SELECT "inventario_fisico"."id"
   FROM "public"."inventario_fisico"
  WHERE ("inventario_fisico"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "ifd_insert" ON "public"."inventario_fisico_detalle" FOR INSERT WITH CHECK (("inventario_id" IN ( SELECT "inventario_fisico"."id"
   FROM "public"."inventario_fisico"
  WHERE ("inventario_fisico"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "ifd_select" ON "public"."inventario_fisico_detalle" FOR SELECT USING (("inventario_id" IN ( SELECT "inventario_fisico"."id"
   FROM "public"."inventario_fisico"
  WHERE ("inventario_fisico"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "ifd_update" ON "public"."inventario_fisico_detalle" FOR UPDATE USING (("inventario_id" IN ( SELECT "inventario_fisico"."id"
   FROM "public"."inventario_fisico"
  WHERE ("inventario_fisico"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



ALTER TABLE "public"."inventario_fisico" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."inventario_fisico_detalle" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."invitaciones" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "invitaciones_service_role_all" ON "public"."invitaciones" TO "service_role" USING (true) WITH CHECK (true);



ALTER TABLE "public"."margen_por_categoria" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "margen_por_categoria_select" ON "public"."margen_por_categoria" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



ALTER TABLE "public"."montura_movimientos" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "montura_movimientos_delete" ON "public"."montura_movimientos" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "montura_movimientos_insert" ON "public"."montura_movimientos" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "montura_movimientos_select" ON "public"."montura_movimientos" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "montura_movimientos_update" ON "public"."montura_movimientos" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



ALTER TABLE "public"."montura_proveedor" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "montura_proveedor_delete" ON "public"."montura_proveedor" FOR DELETE USING (("montura_id" IN ( SELECT "monturas"."id"
   FROM "public"."monturas"
  WHERE ("monturas"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "montura_proveedor_insert" ON "public"."montura_proveedor" FOR INSERT WITH CHECK (("montura_id" IN ( SELECT "monturas"."id"
   FROM "public"."monturas"
  WHERE ("monturas"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "montura_proveedor_select" ON "public"."montura_proveedor" FOR SELECT USING (("montura_id" IN ( SELECT "monturas"."id"
   FROM "public"."monturas"
  WHERE ("monturas"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "montura_proveedor_update" ON "public"."montura_proveedor" FOR UPDATE USING (("montura_id" IN ( SELECT "monturas"."id"
   FROM "public"."monturas"
  WHERE ("monturas"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



ALTER TABLE "public"."monturas" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "monturas_delete" ON "public"."monturas" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "monturas_insert" ON "public"."monturas" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "monturas_select" ON "public"."monturas" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "monturas_update" ON "public"."monturas" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "oci_delete" ON "public"."orden_compra_items" FOR DELETE USING (("orden_id" IN ( SELECT "ordenes_compra"."id"
   FROM "public"."ordenes_compra"
  WHERE ("ordenes_compra"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "oci_insert" ON "public"."orden_compra_items" FOR INSERT WITH CHECK (("orden_id" IN ( SELECT "ordenes_compra"."id"
   FROM "public"."ordenes_compra"
  WHERE ("ordenes_compra"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "oci_select" ON "public"."orden_compra_items" FOR SELECT USING (("orden_id" IN ( SELECT "ordenes_compra"."id"
   FROM "public"."ordenes_compra"
  WHERE ("ordenes_compra"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



CREATE POLICY "oci_update" ON "public"."orden_compra_items" FOR UPDATE USING (("orden_id" IN ( SELECT "ordenes_compra"."id"
   FROM "public"."ordenes_compra"
  WHERE ("ordenes_compra"."optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")))));



ALTER TABLE "public"."optica_settings" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "optica_settings_delete_admin_manager" ON "public"."optica_settings" FOR DELETE TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "optica_settings"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



CREATE POLICY "optica_settings_insert_admin_manager" ON "public"."optica_settings" FOR INSERT TO "authenticated" WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "optica_settings"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



CREATE POLICY "optica_settings_select_member" ON "public"."optica_settings" FOR SELECT TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "optica_settings"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid"))))));



CREATE POLICY "optica_settings_update_admin_manager" ON "public"."optica_settings" FOR UPDATE TO "authenticated" USING ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "optica_settings"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"])))))) WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "uo"
  WHERE (("uo"."optica_id" = "optica_settings"."optica_id") AND ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"("uo"."rol") = ANY (ARRAY['admin'::"text", 'gerente'::"text"]))))));



ALTER TABLE "public"."opticas" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "opticas_insert_authenticated" ON "public"."opticas" FOR INSERT TO "authenticated" WITH CHECK (((( SELECT "auth"."uid"() AS "uid") IS NOT NULL) AND (NULLIF("btrim"("id"), ''::"text") IS NOT NULL) AND (NULLIF("btrim"("nombre"), ''::"text") IS NOT NULL)));



CREATE POLICY "opticas_select_member" ON "public"."opticas" FOR SELECT USING ((("id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = "auth"."uid"()))) AND (("lower"(TRIM(BOTH FROM "plan_code")) <> 'dev_owner'::"text") OR "app_private"."is_internal_owner"())));



CREATE POLICY "opticas_update_member" ON "public"."opticas" FOR UPDATE USING ((("id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = "auth"."uid"()))) AND (("lower"(TRIM(BOTH FROM "plan_code")) <> 'dev_owner'::"text") OR "app_private"."is_internal_owner"()))) WITH CHECK ((("id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = "auth"."uid"()))) AND (("lower"(TRIM(BOTH FROM "plan_code")) <> 'dev_owner'::"text") OR "app_private"."is_internal_owner"())));



ALTER TABLE "public"."orden_compra_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."ordenes_compra" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "ordenes_compra_delete" ON "public"."ordenes_compra" FOR DELETE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "ordenes_compra_insert" ON "public"."ordenes_compra" FOR INSERT WITH CHECK (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "ordenes_compra_select" ON "public"."ordenes_compra" FOR SELECT USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "ordenes_compra_update" ON "public"."ordenes_compra" FOR UPDATE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



ALTER TABLE "public"."pacientes" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pacientes_delete" ON "public"."pacientes" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



ALTER TABLE "public"."pacientes_delete_audit" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pacientes_delete_audit_no_client_access" ON "public"."pacientes_delete_audit" TO "authenticated" USING (false) WITH CHECK (false);



CREATE POLICY "pacientes_insert" ON "public"."pacientes" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "pacientes_select" ON "public"."pacientes" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "pacientes_update" ON "public"."pacientes" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



ALTER TABLE "public"."pagos" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pagos_delete" ON "public"."pagos" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "pagos_insert" ON "public"."pagos" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "pagos_select" ON "public"."pagos" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "pagos_update" ON "public"."pagos" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



ALTER TABLE "public"."pin_attempts" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."proveedores" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "proveedores_delete" ON "public"."proveedores" FOR DELETE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "proveedores_insert" ON "public"."proveedores" FOR INSERT WITH CHECK (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "proveedores_select" ON "public"."proveedores" FOR SELECT USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



CREATE POLICY "proveedores_update" ON "public"."proveedores" FOR UPDATE USING (("optica_id" = ("auth"."jwt"() ->> 'optica_id'::"text")));



ALTER TABLE "public"."regalos_dispensacion" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "regalos_dispensacion_delete" ON "public"."regalos_dispensacion" FOR DELETE USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "regalos_dispensacion_insert" ON "public"."regalos_dispensacion" FOR INSERT WITH CHECK ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "regalos_dispensacion_select" ON "public"."regalos_dispensacion" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "regalos_dispensacion_update" ON "public"."regalos_dispensacion" FOR UPDATE USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



ALTER TABLE "public"."resumen_diario" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "resumen_diario_select" ON "public"."resumen_diario" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



ALTER TABLE "public"."schema_migrations_flags" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "schema_migrations_flags_no_client_access" ON "public"."schema_migrations_flags" TO "authenticated" USING (false) WITH CHECK (false);



CREATE POLICY "service_role_full_access" ON "public"."pin_attempts" TO "service_role" USING (true) WITH CHECK (true);



ALTER TABLE "public"."servicios_extra" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "servicios_extra_delete" ON "public"."servicios_extra" FOR DELETE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"]));



CREATE POLICY "servicios_extra_insert" ON "public"."servicios_extra" FOR INSERT WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



CREATE POLICY "servicios_extra_select" ON "public"."servicios_extra" FOR SELECT USING ("app_private"."is_optica_member"("auth"."uid"(), "optica_id"));



CREATE POLICY "servicios_extra_update" ON "public"."servicios_extra" FOR UPDATE USING ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"])) WITH CHECK ("app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text", 'especialista'::"text", 'asesor'::"text", 'asesora'::"text", 'ventas'::"text"]));



ALTER TABLE "public"."sync_telemetry_optica" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "sync_telemetry_optica_insert_member" ON "public"."sync_telemetry_optica" FOR INSERT TO "authenticated" WITH CHECK (("optica_id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")))));



CREATE POLICY "sync_telemetry_optica_select_member" ON "public"."sync_telemetry_optica" FOR SELECT TO "authenticated" USING (("optica_id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")))));



CREATE POLICY "sync_telemetry_optica_update_member" ON "public"."sync_telemetry_optica" FOR UPDATE TO "authenticated" USING (("optica_id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid"))))) WITH CHECK (("optica_id" IN ( SELECT "uo"."optica_id"
   FROM "public"."usuario_optica" "uo"
  WHERE ("uo"."user_id" = ( SELECT "auth"."uid"() AS "uid")))));



ALTER TABLE "public"."user_profiles" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "user_profiles_select_access" ON "public"."user_profiles" FOR SELECT USING ((("user_id" = ( SELECT "auth"."uid"() AS "uid")) OR (EXISTS ( SELECT 1
   FROM "public"."usuario_optica" "self"
  WHERE (("self"."user_id" = ( SELECT "auth"."uid"() AS "uid")) AND ("lower"(TRIM(BOTH FROM "self"."rol")) = ANY (ARRAY['admin'::"text", 'gerente'::"text"])))))));



ALTER TABLE "public"."usuario_optica" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "usuario_optica_select_member_scope" ON "public"."usuario_optica" FOR SELECT USING ((("user_id" = "auth"."uid"()) OR "app_private"."has_optica_role"("auth"."uid"(), "optica_id", ARRAY['admin'::"text", 'gerente'::"text"])));





ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";


ALTER PUBLICATION "supabase_realtime" ADD TABLE ONLY "public"."usuario_optica";



GRANT USAGE ON SCHEMA "app_private" TO "authenticated";
GRANT USAGE ON SCHEMA "app_private" TO "service_role";
GRANT USAGE ON SCHEMA "app_private" TO "anon";






GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";



REVOKE ALL ON FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) FROM PUBLIC;
GRANT ALL ON FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) TO "authenticated";
GRANT ALL ON FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) TO "service_role";
GRANT ALL ON FUNCTION "app_private"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) TO "anon";



REVOKE ALL ON FUNCTION "app_private"."is_internal_owner"() FROM PUBLIC;
GRANT ALL ON FUNCTION "app_private"."is_internal_owner"() TO "authenticated";
GRANT ALL ON FUNCTION "app_private"."is_internal_owner"() TO "service_role";



REVOKE ALL ON FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") TO "service_role";
GRANT ALL ON FUNCTION "app_private"."is_optica_member"("p_user_id" "uuid", "p_optica_id" "text") TO "anon";











































































































































































REVOKE ALL ON FUNCTION "public"."assert_backup_operation_allowed"("p_action" "text", "p_source_optica_id" "text", "p_target_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."assert_backup_operation_allowed"("p_action" "text", "p_source_optica_id" "text", "p_target_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."assert_backup_operation_allowed"("p_action" "text", "p_source_optica_id" "text", "p_target_optica_id" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."assign_optica_role_by_email"("p_optica_id" "text", "p_email" "text", "p_rol" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."assign_optica_role_by_email"("p_optica_id" "text", "p_email" "text", "p_rol" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) TO "authenticated";
GRANT ALL ON FUNCTION "public"."check_rate_limit"("p_limit_key" "text", "p_window_ms" integer, "p_max_attempts" integer) TO "service_role";



REVOKE ALL ON FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text", "p_fiscal_doc_numero" "text", "p_razon_social" "text", "p_direccion_fiscal" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text", "p_fiscal_doc_numero" "text", "p_razon_social" "text", "p_direccion_fiscal" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."create_optica_for_current_user"("p_optica_id" "text", "p_nombre" "text", "p_fiscal_doc_tipo" "text", "p_fiscal_doc_numero" "text", "p_razon_social" "text", "p_direccion_fiscal" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."enforce_admin_role_assignment_guard"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."enforce_admin_role_assignment_guard"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."enforce_dev_owner_guard"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."enforce_dev_owner_guard"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."enforce_dev_owner_membership_guard"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."enforce_dev_owner_membership_guard"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."enforce_optica_limit_for_creator"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."enforce_optica_limit_for_creator"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."guard_opticas_business_profile_optional_update"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."guard_opticas_business_profile_optional_update"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."guard_opticas_fiscal_update"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."guard_opticas_fiscal_update"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."guard_pacientes_delete"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."guard_pacientes_delete"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) TO "service_role";
GRANT ALL ON FUNCTION "public"."has_optica_role"("p_user_id" "uuid", "p_optica_id" "text", "p_roles" "text"[]) TO "authenticated";



REVOKE ALL ON FUNCTION "public"."is_internal_owner"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."is_internal_owner"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."paciente_eliminaciones_restantes_hoy"("p_optica_id" "uuid") TO "service_role";



REVOKE ALL ON FUNCTION "public"."recalcular_resumen_diario"("p_optica_id" "text", "p_fecha" "date") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."recalcular_resumen_diario"("p_optica_id" "text", "p_fecha" "date") TO "authenticated";
GRANT ALL ON FUNCTION "public"."recalcular_resumen_diario"("p_optica_id" "text", "p_fecha" "date") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rls_auto_enable"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rls_auto_enable"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_adjust_montura_stock"("p_montura_id" "text", "p_optica_id" "text", "p_delta" integer, "p_reference_id" "text", "p_note" "text", "p_tipo" "text", "p_fecha" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_adjust_montura_stock"("p_montura_id" "text", "p_optica_id" "text", "p_delta" integer, "p_reference_id" "text", "p_note" "text", "p_tipo" "text", "p_fecha" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_adjust_montura_stock"("p_montura_id" "text", "p_optica_id" "text", "p_delta" integer, "p_reference_id" "text", "p_note" "text", "p_tipo" "text", "p_fecha" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_analisis_mensual"("p_optica_id" "text", "p_mes" "date") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_cierre_caja_resumen"("p_optica_id" "text", "p_from" "date", "p_to" "date") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_count_pendientes"("p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_count_pendientes"("p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_count_pendientes"("p_optica_id" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_deudores"("p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_deudores"("p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_deudores"("p_optica_id" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_pacientes_con_entrega_pendiente"("p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_pacientes_con_entrega_pendiente"("p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_pacientes_con_entrega_pendiente"("p_optica_id" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_pacientes_con_saldo"("p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_pacientes_con_saldo"("p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_pacientes_con_saldo"("p_optica_id" "text") TO "service_role";



REVOKE ALL ON FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."rpc_saldo_pendiente"("p_optica_id" "text") TO "service_role";



GRANT ALL ON FUNCTION "public"."set_sync_telemetry_audit_fields"() TO "anon";
GRANT ALL ON FUNCTION "public"."set_sync_telemetry_audit_fields"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."set_sync_telemetry_audit_fields"() TO "service_role";



GRANT ALL ON FUNCTION "public"."set_sync_telemetry_updated_at"() TO "anon";
GRANT ALL ON FUNCTION "public"."set_sync_telemetry_updated_at"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."set_sync_telemetry_updated_at"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."set_updated_audit_fields"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."set_updated_audit_fields"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."suggest_next_ho"("p_optica_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."suggest_next_ho"("p_optica_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."suggest_next_ho"("p_optica_id" "uuid") TO "service_role";



REVOKE ALL ON FUNCTION "public"."sync_snapshot"("p_optica_id" "uuid") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."sync_snapshot"("p_optica_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."sync_snapshot"("p_optica_id" "uuid") TO "service_role";



REVOKE ALL ON FUNCTION "public"."sync_user_profiles_from_auth"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."sync_user_profiles_from_auth"() TO "service_role";



GRANT ALL ON FUNCTION "public"."trg_pagos_set_venta_id"() TO "anon";
GRANT ALL ON FUNCTION "public"."trg_pagos_set_venta_id"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."trg_pagos_set_venta_id"() TO "service_role";



GRANT ALL ON FUNCTION "public"."trg_pagos_update_monto_pagado"() TO "anon";
GRANT ALL ON FUNCTION "public"."trg_pagos_update_monto_pagado"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."trg_pagos_update_monto_pagado"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_updated_at"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_updated_at"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_updated_at"() TO "service_role";
























GRANT ALL ON TABLE "public"."app_releases" TO "anon";
GRANT ALL ON TABLE "public"."app_releases" TO "authenticated";
GRANT ALL ON TABLE "public"."app_releases" TO "service_role";



GRANT ALL ON SEQUENCE "public"."app_releases_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."app_releases_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."app_releases_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."categorias_montura" TO "anon";
GRANT ALL ON TABLE "public"."categorias_montura" TO "authenticated";
GRANT ALL ON TABLE "public"."categorias_montura" TO "service_role";



GRANT ALL ON TABLE "public"."categorias_producto" TO "anon";
GRANT ALL ON TABLE "public"."categorias_producto" TO "authenticated";
GRANT ALL ON TABLE "public"."categorias_producto" TO "service_role";



GRANT ALL ON TABLE "public"."cierres_caja" TO "anon";
GRANT ALL ON TABLE "public"."cierres_caja" TO "authenticated";
GRANT ALL ON TABLE "public"."cierres_caja" TO "service_role";



GRANT ALL ON TABLE "public"."configuracion_financiera" TO "anon";
GRANT ALL ON TABLE "public"."configuracion_financiera" TO "authenticated";
GRANT ALL ON TABLE "public"."configuracion_financiera" TO "service_role";



GRANT ALL ON TABLE "public"."costos_productos" TO "anon";
GRANT ALL ON TABLE "public"."costos_productos" TO "authenticated";
GRANT ALL ON TABLE "public"."costos_productos" TO "service_role";



GRANT ALL ON TABLE "public"."dispensacion_items" TO "anon";
GRANT ALL ON TABLE "public"."dispensacion_items" TO "authenticated";
GRANT ALL ON TABLE "public"."dispensacion_items" TO "service_role";



GRANT ALL ON TABLE "public"."dispensaciones" TO "anon";
GRANT ALL ON TABLE "public"."dispensaciones" TO "authenticated";
GRANT ALL ON TABLE "public"."dispensaciones" TO "service_role";



GRANT ALL ON TABLE "public"."evaluaciones" TO "anon";
GRANT ALL ON TABLE "public"."evaluaciones" TO "authenticated";
GRANT ALL ON TABLE "public"."evaluaciones" TO "service_role";



GRANT ALL ON TABLE "public"."feedback_recomendaciones" TO "anon";
GRANT ALL ON TABLE "public"."feedback_recomendaciones" TO "authenticated";
GRANT ALL ON TABLE "public"."feedback_recomendaciones" TO "service_role";



GRANT ALL ON TABLE "public"."gastos_operativos" TO "anon";
GRANT ALL ON TABLE "public"."gastos_operativos" TO "authenticated";
GRANT ALL ON TABLE "public"."gastos_operativos" TO "service_role";



GRANT ALL ON TABLE "public"."inventario_fisico" TO "anon";
GRANT ALL ON TABLE "public"."inventario_fisico" TO "authenticated";
GRANT ALL ON TABLE "public"."inventario_fisico" TO "service_role";



GRANT ALL ON TABLE "public"."inventario_fisico_detalle" TO "anon";
GRANT ALL ON TABLE "public"."inventario_fisico_detalle" TO "authenticated";
GRANT ALL ON TABLE "public"."inventario_fisico_detalle" TO "service_role";



GRANT ALL ON TABLE "public"."invitaciones" TO "anon";
GRANT ALL ON TABLE "public"."invitaciones" TO "authenticated";
GRANT ALL ON TABLE "public"."invitaciones" TO "service_role";



GRANT ALL ON TABLE "public"."margen_por_categoria" TO "anon";
GRANT ALL ON TABLE "public"."margen_por_categoria" TO "authenticated";
GRANT ALL ON TABLE "public"."margen_por_categoria" TO "service_role";



GRANT ALL ON TABLE "public"."montura_movimientos" TO "anon";
GRANT ALL ON TABLE "public"."montura_movimientos" TO "authenticated";
GRANT ALL ON TABLE "public"."montura_movimientos" TO "service_role";



GRANT ALL ON TABLE "public"."montura_proveedor" TO "anon";
GRANT ALL ON TABLE "public"."montura_proveedor" TO "authenticated";
GRANT ALL ON TABLE "public"."montura_proveedor" TO "service_role";



GRANT ALL ON TABLE "public"."monturas" TO "anon";
GRANT ALL ON TABLE "public"."monturas" TO "authenticated";
GRANT ALL ON TABLE "public"."monturas" TO "service_role";



GRANT ALL ON TABLE "public"."user_profiles" TO "anon";
GRANT ALL ON TABLE "public"."user_profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."user_profiles" TO "service_role";



GRANT ALL ON TABLE "public"."usuario_optica" TO "anon";
GRANT ALL ON TABLE "public"."usuario_optica" TO "authenticated";
GRANT ALL ON TABLE "public"."usuario_optica" TO "service_role";



GRANT ALL ON TABLE "public"."optica_members" TO "anon";
GRANT ALL ON TABLE "public"."optica_members" TO "authenticated";
GRANT ALL ON TABLE "public"."optica_members" TO "service_role";



GRANT ALL ON TABLE "public"."optica_settings" TO "anon";
GRANT ALL ON TABLE "public"."optica_settings" TO "authenticated";
GRANT ALL ON TABLE "public"."optica_settings" TO "service_role";



GRANT ALL ON TABLE "public"."opticas" TO "anon";
GRANT ALL ON TABLE "public"."opticas" TO "authenticated";
GRANT ALL ON TABLE "public"."opticas" TO "service_role";



GRANT ALL ON TABLE "public"."orden_compra_items" TO "anon";
GRANT ALL ON TABLE "public"."orden_compra_items" TO "authenticated";
GRANT ALL ON TABLE "public"."orden_compra_items" TO "service_role";



GRANT ALL ON TABLE "public"."ordenes_compra" TO "anon";
GRANT ALL ON TABLE "public"."ordenes_compra" TO "authenticated";
GRANT ALL ON TABLE "public"."ordenes_compra" TO "service_role";



GRANT ALL ON TABLE "public"."pacientes" TO "anon";
GRANT ALL ON TABLE "public"."pacientes" TO "authenticated";
GRANT ALL ON TABLE "public"."pacientes" TO "service_role";



GRANT ALL ON TABLE "public"."pacientes_delete_audit" TO "service_role";



GRANT ALL ON SEQUENCE "public"."pacientes_delete_audit_id_seq" TO "anon";
GRANT ALL ON SEQUENCE "public"."pacientes_delete_audit_id_seq" TO "authenticated";
GRANT ALL ON SEQUENCE "public"."pacientes_delete_audit_id_seq" TO "service_role";



GRANT ALL ON TABLE "public"."pagos" TO "anon";
GRANT ALL ON TABLE "public"."pagos" TO "authenticated";
GRANT ALL ON TABLE "public"."pagos" TO "service_role";



GRANT ALL ON TABLE "public"."pin_attempts" TO "anon";
GRANT ALL ON TABLE "public"."pin_attempts" TO "authenticated";
GRANT ALL ON TABLE "public"."pin_attempts" TO "service_role";



GRANT ALL ON TABLE "public"."proveedores" TO "anon";
GRANT ALL ON TABLE "public"."proveedores" TO "authenticated";
GRANT ALL ON TABLE "public"."proveedores" TO "service_role";



GRANT ALL ON TABLE "public"."regalos_dispensacion" TO "anon";
GRANT ALL ON TABLE "public"."regalos_dispensacion" TO "authenticated";
GRANT ALL ON TABLE "public"."regalos_dispensacion" TO "service_role";



GRANT ALL ON TABLE "public"."resumen_diario" TO "anon";
GRANT ALL ON TABLE "public"."resumen_diario" TO "authenticated";
GRANT ALL ON TABLE "public"."resumen_diario" TO "service_role";



GRANT ALL ON TABLE "public"."schema_migrations_flags" TO "anon";
GRANT ALL ON TABLE "public"."schema_migrations_flags" TO "authenticated";
GRANT ALL ON TABLE "public"."schema_migrations_flags" TO "service_role";



GRANT ALL ON TABLE "public"."servicios_extra" TO "anon";
GRANT ALL ON TABLE "public"."servicios_extra" TO "authenticated";
GRANT ALL ON TABLE "public"."servicios_extra" TO "service_role";



GRANT ALL ON TABLE "public"."sync_telemetry_optica" TO "service_role";
GRANT SELECT,INSERT,UPDATE ON TABLE "public"."sync_telemetry_optica" TO "authenticated";
GRANT SELECT ON TABLE "public"."sync_telemetry_optica" TO "anon";









ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "service_role";







































