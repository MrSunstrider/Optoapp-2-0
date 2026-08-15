-- =============================================================================
-- Change: fix-inventory-double-stock-writer / WU-4
--
-- A sale's stock effect had two writers: the app's local dispensación save
-- (montura_movimientos.tipo = 'SALIDA_VENTA') and this RPC, replayed by the
-- finanzas upload for every item of a full dispensaciones snapshot
-- (tipo = 'venta'). Because the two used different `tipo` values,
-- idx_movimientos_conflict (referencia_id, tipo, montura_id) never deduped
-- them — it only blocked the replay, raising 23505 24 times per sync.
--
-- The client no longer calls this function during sync. Two things remain:
--   1. purge the phantom `venta` rows the second writer left behind;
--   2. harden the function so no caller (this app, optoapp-web, or a manual
--      call) can double-apply, overdraw, or cross a tenant boundary.
--
-- The function is NOT dropped: optoapp-web shares this schema.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Harden rpc_adjust_montura_stock.
--
-- This runs BEFORE the purge. The purge removes the very rows the old identity
-- check relied on, so hardening first is what keeps a client still running the
-- pre-fix build from re-applying the decrement as if it were a new sale.
--
-- Signature, jsonb result shape, SECURITY DEFINER and search_path are preserved
-- so existing callers keep working. Behaviour changes:
--   * role guard      — SECURITY DEFINER bypasses RLS, so the policy is mirrored
--   * FOR UPDATE      — the read-modify-write is serialized per montura
--   * idempotency     — a replay of a recorded fact is a no-op, not a 23505
--   * insufficiency   — refused before the update, instead of mutate-then-restore
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_adjust_montura_stock(
    p_montura_id text,
    p_optica_id text,
    p_delta integer,
    p_reference_id text,
    p_note text,
    p_tipo text,
    p_fecha text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_old_stock integer;
    v_new_stock integer;
    v_recorded integer;
    v_recorded_tipo text;
    v_inserted integer;
    v_is_sale boolean := lower(p_tipo) IN ('venta', 'salida_venta');
BEGIN
    IF NOT app_private.has_optica_role(
        auth.uid(),
        p_optica_id,
        ARRAY['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']
    ) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'forbidden');
    END IF;

    SELECT stock_actual INTO v_old_stock
    FROM public.monturas
    WHERE id = p_montura_id
      AND optica_id = p_optica_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_found');
    END IF;

    -- A movement fact is identified by (referencia_id, montura_id) within the
    -- tenant, plus its type. Seeing it again means the caller is retrying.
    --
    -- For sales the type is an alias, not part of the identity: the local
    -- dispensación save records 'SALIDA_VENTA' and the retired sync writer used
    -- 'venta' for the very same sale. Collapsing them is what makes purging the
    -- phantom rows safe — otherwise a client still on the old build would find
    -- nothing, treat the replay as a brand-new sale and decrement stock again.
    SELECT stock_nuevo, tipo INTO v_recorded, v_recorded_tipo
    FROM public.montura_movimientos
    WHERE referencia_id = p_reference_id
      AND montura_id = p_montura_id
      AND optica_id = p_optica_id
      AND (tipo = p_tipo OR (v_is_sale AND lower(tipo) IN ('venta', 'salida_venta')))
    ORDER BY (tipo = p_tipo) DESC
    LIMIT 1;

    IF FOUND THEN
        RETURN jsonb_build_object(
            'ok', true,
            'idempotent', true,
            'new_stock', v_old_stock,
            'recorded_tipo', v_recorded_tipo,
            'recorded_stock', v_recorded);
    END IF;

    v_new_stock := v_old_stock + p_delta;

    IF v_new_stock < 0 THEN
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
    )
    ON CONFLICT (referencia_id, tipo, montura_id) DO NOTHING;

    GET DIAGNOSTICS v_inserted = ROW_COUNT;

    -- The fact was recorded by a path that does not take the montura lock (the
    -- inventario movement upsert). Its ledger row stands; stock must not stack.
    IF v_inserted = 0 THEN
        RETURN jsonb_build_object('ok', true, 'idempotent', true, 'new_stock', v_old_stock);
    END IF;

    UPDATE public.monturas
    SET stock_actual = v_new_stock
    WHERE id = p_montura_id
      AND optica_id = p_optica_id;

    RETURN jsonb_build_object('ok', true, 'new_stock', v_new_stock);
END;
$function$;

-- ----------------------------------------------------------------------------
-- 2. Purge phantom sale movements, now that a replay cannot re-create them.
--
-- A row is a phantom only if it is a 'venta' carrying the second writer's note
-- AND a SALIDA_VENTA row exists for the same (referencia_id, montura_id,
-- optica_id). A legitimate 'venta' with no such twin is left alone.
--
-- stock_actual is deliberately NOT rewritten: it already agrees with the
-- SALIDA_VENTA ledger, because the inventario monturas upsert overwrote every
-- decrement this RPC had applied.
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_purged BIGINT;
BEGIN
    WITH purged AS (
        DELETE FROM public.montura_movimientos m
        WHERE m.tipo = 'venta'
          AND m.nota = 'venta_dispensacion'
          AND EXISTS (
            SELECT 1 FROM public.montura_movimientos twin
            WHERE twin.tipo = 'SALIDA_VENTA'
              AND twin.referencia_id = m.referencia_id
              AND twin.montura_id = m.montura_id
              AND twin.optica_id = m.optica_id
          )
        RETURNING 1
    )
    SELECT count(*) INTO v_purged FROM purged;

    RAISE NOTICE 'inventory purge: % phantom venta movement(s) removed', v_purged;
END;
$$;

COMMENT ON FUNCTION public.rpc_adjust_montura_stock(text, text, integer, text, text, text, text) IS
'Adjusts montura stock and appends one movement. Idempotent per (referencia_id, montura_id, optica_id) + tipo, where ''venta'' and ''SALIDA_VENTA'' count as the same sale. Guarded by app_private.has_optica_role because SECURITY DEFINER bypasses RLS. Not called by the Android finanzas sync — the local dispensación save is the single writer for sale effects.';
