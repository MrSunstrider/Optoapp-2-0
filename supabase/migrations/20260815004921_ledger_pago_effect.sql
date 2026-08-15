-- ============================================================================
-- Ledger contract: pago_effect + expanded estado domains + reversa_pago_id
-- Change: fix-sync-financial-ledger / WU-1 (task 1.3)
--
-- DB-first typed cash ledger:
--   * monto stays a NON-NEGATIVE magnitude (pagos_monto_chk is NOT touched).
--   * Sign of cash effect derives ONLY from tipo via public.pago_effect().
--   * Cancel keeps originals + one linked Reverso (reversa_pago_id, idempotent).
--   * Remote estado domains expand so Anulado/Reclamada sync without CHECK 23514.
--
-- Idempotent: every object uses CREATE OR REPLACE or a pg_constraint /
-- pg_class guard, so the file is safe to re-run. Transaction-safe: no
-- CONCURRENTLY, so the CLI wraps the whole file in one transaction.
--
-- Scope note: this slice converges the effect helper, the write-path trigger,
-- and the constraint/schema contract. Read-only BI aggregate convergence
-- (recalcular_resumen_diario, rpc_cierre_caja_resumen, rpc_deudores,
-- rpc_analisis_mensual) is a follow-up slice — see apply-progress. It is safe
-- to defer while zero Reverso/Reembolso rows exist and MUST land before the
-- WU-2 client writers that create those rows are deployed.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. pago_effect(tipo, monto) — single source of truth for cash sign.
--    Mirrors Kotlin PagoEffect (WU-2). Pure/IMMUTABLE, no table access.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.pago_effect(p_tipo text, p_monto numeric)
RETURNS numeric
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT CASE btrim(COALESCE(p_tipo, ''))
        WHEN 'Abono'         THEN p_monto
        WHEN 'Pago completo' THEN p_monto
        WHEN 'Reembolso'     THEN -p_monto
        WHEN 'Reverso'       THEN -p_monto
        ELSE 0  -- Anulación (legacy audit) + any unknown tipo => no cash effect
    END;
$$;

COMMENT ON FUNCTION public.pago_effect(text, numeric) IS
'Signed cash effect of a pago from its tipo; monto is a non-negative magnitude. Abono/Pago completo=+monto, Reembolso/Reverso=-monto, Anulación/unknown=0.';

ALTER FUNCTION public.pago_effect(text, numeric) OWNER TO postgres;
REVOKE EXECUTE ON FUNCTION public.pago_effect(text, numeric) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.pago_effect(text, numeric) TO authenticated, service_role;

-- ----------------------------------------------------------------------------
-- 2. Expand estado domains so cancelled/claimed sales sync (no CHECK 23514).
--    Additive: existing {Pendiente,Entregado} rows remain valid, so we add
--    NOT VALID then VALIDATE immediately (preflight proves inventories clean).
-- ----------------------------------------------------------------------------
-- servicios_extra.estado: + Anulado
ALTER TABLE public.servicios_extra
    DROP CONSTRAINT IF EXISTS servicios_extra_estado_domain_chk;
ALTER TABLE public.servicios_extra
    ADD CONSTRAINT servicios_extra_estado_domain_chk
    CHECK (estado IN ('Pendiente', 'Entregado', 'Anulado')) NOT VALID;
ALTER TABLE public.servicios_extra
    VALIDATE CONSTRAINT servicios_extra_estado_domain_chk;

-- dispensaciones.estado_entrega: + Anulado, + Reclamada
ALTER TABLE public.dispensaciones
    DROP CONSTRAINT IF EXISTS dispensaciones_estado_entrega_domain_chk;
ALTER TABLE public.dispensaciones
    ADD CONSTRAINT dispensaciones_estado_entrega_domain_chk
    CHECK (estado_entrega IN ('Pendiente', 'Entregado', 'Anulado', 'Reclamada')) NOT VALID;
ALTER TABLE public.dispensaciones
    VALIDATE CONSTRAINT dispensaciones_estado_entrega_domain_chk;

-- ----------------------------------------------------------------------------
-- 3. reversa_pago_id — links a Reverso to the original credit pago it reverses.
--    Column + COMPOSITE self FK (RESTRICT) + XOR CHECK + partial UNIQUE.
--
--    The link is tenant-scoped by the SCHEMA, not by client discipline: the FK
--    carries optica_id, so a Reverso can only ever reverse an original of the
--    same optica. An offline client that uploads a Reverso pointing at another
--    tenant's pago id is rejected by the database instead of quietly moving
--    cash across tenants.
-- ----------------------------------------------------------------------------
ALTER TABLE public.pagos
    ADD COLUMN IF NOT EXISTS reversa_pago_id text;

COMMENT ON COLUMN public.pagos.reversa_pago_id IS
'For tipo=Reverso only: id of the original credit pago being reversed, which must belong to the same optica. NULL otherwise.';

-- Referenced key for the composite FK. id is already the PK, so this adds no
-- new uniqueness — it only makes (id, optica_id) addressable as a FK target.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'pagos_id_optica_id_key'
          AND conrelid = 'public.pagos'::regclass
    ) THEN
        ALTER TABLE public.pagos
            ADD CONSTRAINT pagos_id_optica_id_key UNIQUE (id, optica_id);
    END IF;
END$$;

-- Self-heal: if a pre-correction (single-column) form of this migration was
-- ever applied locally, drop it so the composite FK can take the name.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'pagos_reversa_pago_id_fkey'
          AND conrelid = 'public.pagos'::regclass
          AND pg_get_constraintdef(oid)
              NOT LIKE 'FOREIGN KEY (reversa_pago_id, optica_id)%'
    ) THEN
        ALTER TABLE public.pagos DROP CONSTRAINT pagos_reversa_pago_id_fkey;
    END IF;
END$$;

-- Composite self FK. MATCH SIMPLE: the check is skipped while reversa_pago_id
-- is NULL (every non-Reverso row) and optica_id is NOT NULL, so it binds
-- exactly on Reverso rows. RESTRICT keeps an original undeletable while a
-- Reverso still points at it.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'pagos_reversa_pago_id_fkey'
          AND conrelid = 'public.pagos'::regclass
    ) THEN
        ALTER TABLE public.pagos
            ADD CONSTRAINT pagos_reversa_pago_id_fkey
            FOREIGN KEY (reversa_pago_id, optica_id)
            REFERENCES public.pagos(id, optica_id)
            ON DELETE RESTRICT;
    END IF;
END$$;

-- XOR: Reverso <=> reversa_pago_id NOT NULL; every other tipo => NULL.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_pagos_reversa_link'
          AND conrelid = 'public.pagos'::regclass
    ) THEN
        ALTER TABLE public.pagos
            ADD CONSTRAINT chk_pagos_reversa_link
            CHECK (
                (tipo = 'Reverso' AND reversa_pago_id IS NOT NULL)
             OR (tipo <> 'Reverso' AND reversa_pago_id IS NULL)
            ) NOT VALID;
    END IF;
END$$;
-- Clean inventory (0 Reverso rows) => safe to validate now.
ALTER TABLE public.pagos VALIDATE CONSTRAINT chk_pagos_reversa_link;

-- Idempotency: at most one Reverso per original credit pago. Deliberately
-- GLOBAL, not per-optica: pago ids are global, and adding optica_id here would
-- let two rows claim the same original whenever a client sends a mismatched
-- tenant. Tenant scoping is the composite FK's job, uniqueness is this index's.
CREATE UNIQUE INDEX IF NOT EXISTS pagos_reversa_pago_id_uidx
    ON public.pagos (reversa_pago_id)
    WHERE tipo = 'Reverso' AND reversa_pago_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 4. Write-path convergence: trg_pagos_update_monto_pagado uses pago_effect so
--    monto_pagado / a_cuenta track the SIGNED effect (Reverso/Reembolso reduce
--    the balance). Atomic column += delta is preserved from the prior version.
--
--    An UPDATE may also MOVE a pago between sales (dispensacion_id or
--    servicio_extra_id changes, including dispensación <-> servicio). Resolving
--    the parent with COALESCE(NEW.x, OLD.x) alone would credit only the NEW
--    sale with the net delta and strand the OLD sale's balance forever — the
--    former parent keeps cash it no longer has. So the move case withdraws the
--    full OLD effect from the OLD parent and deposits the full NEW effect into
--    the NEW parent, and the net-delta fast path is kept for the common case
--    where the origin did not change.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    v_old_effect   NUMERIC := 0;
    v_new_effect   NUMERIC := 0;
    v_delta        NUMERIC;
    v_origin_moved BOOLEAN := false;
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_old_effect := public.pago_effect(OLD.tipo, OLD.monto);
    END IF;
    IF TG_OP IN ('UPDATE', 'INSERT') THEN
        v_new_effect := public.pago_effect(NEW.tipo, NEW.monto);
    END IF;

    IF TG_OP = 'UPDATE' THEN
        v_origin_moved :=
            NEW.dispensacion_id   IS DISTINCT FROM OLD.dispensacion_id
         OR NEW.servicio_extra_id IS DISTINCT FROM OLD.servicio_extra_id;
    END IF;

    -- Fast path: same parent. Covers INSERT (+effect(NEW)), DELETE
    -- (-effect(OLD)) and an UPDATE that only changed tipo/monto/anything else.
    -- Anulación contributes 0 on both sides.
    IF NOT v_origin_moved THEN
        v_delta := v_new_effect - v_old_effect;
        IF v_delta <> 0 THEN
            IF COALESCE(NEW.dispensacion_id, OLD.dispensacion_id) IS NOT NULL THEN
                UPDATE public.dispensaciones
                SET monto_pagado = monto_pagado + v_delta
                WHERE id = COALESCE(NEW.dispensacion_id, OLD.dispensacion_id);
            END IF;
            IF COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id) IS NOT NULL THEN
                UPDATE public.servicios_extra
                SET a_cuenta = a_cuenta + v_delta
                WHERE id = COALESCE(NEW.servicio_extra_id, OLD.servicio_extra_id);
            END IF;
        END IF;

        RETURN COALESCE(NEW, OLD);
    END IF;

    -- Origin moved: withdraw the whole OLD effect from whichever parent the
    -- pago used to belong to.
    IF v_old_effect <> 0 THEN
        IF OLD.dispensacion_id IS NOT NULL THEN
            UPDATE public.dispensaciones
            SET monto_pagado = monto_pagado - v_old_effect
            WHERE id = OLD.dispensacion_id;
        END IF;
        IF OLD.servicio_extra_id IS NOT NULL THEN
            UPDATE public.servicios_extra
            SET a_cuenta = a_cuenta - v_old_effect
            WHERE id = OLD.servicio_extra_id;
        END IF;
    END IF;

    -- ...and deposit the whole NEW effect into the parent it now belongs to.
    IF v_new_effect <> 0 THEN
        IF NEW.dispensacion_id IS NOT NULL THEN
            UPDATE public.dispensaciones
            SET monto_pagado = monto_pagado + v_new_effect
            WHERE id = NEW.dispensacion_id;
        END IF;
        IF NEW.servicio_extra_id IS NOT NULL THEN
            UPDATE public.servicios_extra
            SET a_cuenta = a_cuenta + v_new_effect
            WHERE id = NEW.servicio_extra_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- Recreate trigger (idempotent) and keep the function trigger-only.
DROP TRIGGER IF EXISTS trg_pagos_maintain_monto_pagado ON public.pagos;
CREATE TRIGGER trg_pagos_maintain_monto_pagado
    AFTER INSERT OR UPDATE OR DELETE ON public.pagos
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_pagos_update_monto_pagado();

REVOKE EXECUTE ON FUNCTION public.trg_pagos_update_monto_pagado()
    FROM PUBLIC, anon, authenticated;

-- ----------------------------------------------------------------------------
-- 5. Repair inherited parent-balance drift BEFORE any Reverso/Reembolso writers
--    exist. Preflight found Sersa rows where monto_pagado/a_cuenta disagree with
--    the Abono ledger (orphaned cash after deletes that missed the trigger, or
--    partial sync). Recompute from pago_effect so write-path and RPC aggregates
--    start from the same truth.
-- ----------------------------------------------------------------------------
UPDATE public.dispensaciones d
SET monto_pagado = COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.dispensacion_id = d.id
), 0)
WHERE abs(d.monto_pagado - COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.dispensacion_id = d.id
), 0)) > 0.005;

UPDATE public.servicios_extra se
SET a_cuenta = COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.servicio_extra_id = se.id
), 0)
WHERE abs(se.a_cuenta - COALESCE((
    SELECT SUM(public.pago_effect(p.tipo, p.monto))
    FROM public.pagos p
    WHERE p.servicio_extra_id = se.id
), 0)) > 0.005;
