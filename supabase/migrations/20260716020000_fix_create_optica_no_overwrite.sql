-- Fix 1.6: create_optica_for_current_user — ON CONFLICT DO NOTHING
-- Root cause: ON CONFLICT (id) DO UPDATE allowed anyone who knows an optica_id
-- to overwrite that optica's data. Combined with client-generated IDs, this
-- made the overwrite predictable.
--
-- Fix:
--   1. Server generates UUID (opt_<uuid>) when client provides empty id
--   2. ON CONFLICT (id) DO NOTHING — existing optica records are never overwritten
--   3. ON CONFLICT (user_id, optica_id) DO NOTHING — role is never changed
--   4. SET search_path = '' — security hardening (fully qualified names only)

CREATE OR REPLACE FUNCTION public.create_optica_for_current_user(
    p_optica_id         text,
    p_nombre            text,
    p_fiscal_doc_tipo   text DEFAULT '',
    p_fiscal_doc_numero text DEFAULT '',
    p_razon_social      text DEFAULT '',
    p_direccion_fiscal  text DEFAULT ''
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id   uuid;
    v_optica_id text;
    v_nombre    text;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    v_nombre := trim(p_nombre);
    IF v_nombre = '' THEN
        RAISE EXCEPTION 'Nombre de óptica requerido';
    END IF;

    -- Use server-generated UUID instead of client-provided ID
    v_optica_id := COALESCE(NULLIF(p_optica_id, ''), 'opt_' || replace(gen_random_uuid()::text, '-', ''));

    -- Insert optica — DO NOTHING on conflict (was DO UPDATE)
    INSERT INTO public.opticas (id, nombre, fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal)
    VALUES (v_optica_id, v_nombre,
        upper(trim(p_fiscal_doc_tipo)),
        trim(p_fiscal_doc_numero),
        trim(p_razon_social),
        trim(p_direccion_fiscal))
    ON CONFLICT (id) DO NOTHING;

    -- Register caller as admin of the new optica
    INSERT INTO public.usuario_optica (user_id, optica_id, rol)
    VALUES (v_user_id, v_optica_id, 'admin')
    ON CONFLICT (user_id, optica_id) DO NOTHING;
END;
$$;

ALTER FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) TO authenticated;
