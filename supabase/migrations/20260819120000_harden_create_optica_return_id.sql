-- Harden create_optica_for_current_user: ignore client id, admin iff INSERT, return server id.
-- DROP+CREATE required because return type changes from void to text.
-- Direct authenticated INSERT into opticas is forbidden; RPC is SECURITY DEFINER.

DROP FUNCTION IF EXISTS public.create_optica_for_current_user(text, text, text, text, text, text);

CREATE FUNCTION public.create_optica_for_current_user(
    p_optica_id         text,
    p_nombre            text,
    p_fiscal_doc_tipo   text DEFAULT '',
    p_fiscal_doc_numero text DEFAULT '',
    p_razon_social      text DEFAULT '',
    p_direccion_fiscal  text DEFAULT ''
)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id   uuid;
    v_optica_id text;
    v_nombre    text;
    v_inserted  integer;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    v_nombre := trim(p_nombre);
    IF v_nombre = '' THEN
        RAISE EXCEPTION 'Nombre de óptica requerido';
    END IF;

    -- Always ignore p_optica_id; identity is server-generated.
    v_optica_id := 'opt_' || replace(gen_random_uuid()::text, '-', '');

    INSERT INTO public.opticas (id, nombre, fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal)
    VALUES (
        v_optica_id,
        v_nombre,
        upper(trim(p_fiscal_doc_tipo)),
        trim(p_fiscal_doc_numero),
        trim(p_razon_social),
        trim(p_direccion_fiscal)
    )
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS v_inserted = ROW_COUNT;

    IF v_inserted <> 1 THEN
        RAISE EXCEPTION 'No se pudo crear la óptica';
    END IF;

    INSERT INTO public.usuario_optica (user_id, optica_id, rol)
    VALUES (v_user_id, v_optica_id, 'admin')
    ON CONFLICT (user_id, optica_id) DO NOTHING;

    RETURN v_optica_id;
END;
$$;

ALTER FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) OWNER TO postgres;

REVOKE ALL ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) FROM anon;
GRANT EXECUTE ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) TO authenticated;

DROP POLICY IF EXISTS opticas_insert_authenticated ON public.opticas;
