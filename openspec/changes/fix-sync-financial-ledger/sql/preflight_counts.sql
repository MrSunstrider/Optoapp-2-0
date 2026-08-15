-- Preflight counts — fix-sync-financial-ledger / WU-1 (task 1.1)
-- READ-ONLY. Run against the linked production project BEFORE applying the
-- ledger migration and again during GGA. No DML. No DDL. SELECT only.
--
-- Purpose: prove the pagos inventory is clean enough that the new domain
-- CHECKs / effect contract can be VALIDATEd without rejecting historical rows.
-- Each query returns a single labelled count; all counts MUST be 0 except
-- `total_pagos` and the tipo/metodo breakdown, which are informational.

-- 0. Inventory (informational)
SELECT 'total_pagos' AS metric, COUNT(*)::bigint AS value FROM public.pagos;

SELECT 'by_tipo' AS metric, tipo, COUNT(*)::bigint AS value
FROM public.pagos GROUP BY tipo ORDER BY tipo;

SELECT 'by_metodo' AS metric, metodo_pago, COUNT(*)::bigint AS value
FROM public.pagos GROUP BY metodo_pago ORDER BY metodo_pago;

-- 1. Negative magnitudes — MUST be 0 (pagos_monto_chk stays monto >= 0)
SELECT 'monto_negativo' AS metric, COUNT(*)::bigint AS value
FROM public.pagos WHERE monto < 0;

-- 2. Zero-amount rows (informational; allowed but flagged)
SELECT 'monto_cero' AS metric, COUNT(*)::bigint AS value
FROM public.pagos WHERE monto = 0;

-- 3. tipo out of domain — MUST be 0 before VALIDATE chk_pagos_tipo
SELECT 'tipo_fuera_de_dominio' AS metric, COUNT(*)::bigint AS value
FROM public.pagos
WHERE tipo IS NULL
   OR tipo NOT IN ('Abono', 'Pago completo', 'Reembolso', 'Reverso', 'Anulación');

-- 4. metodo_pago out of domain — MUST be 0 before VALIDATE chk_pagos_metodo
SELECT 'metodo_fuera_de_dominio' AS metric, COUNT(*)::bigint AS value
FROM public.pagos
WHERE metodo_pago IS NULL
   OR metodo_pago NOT IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin', 'Móvil');

-- 5. Origen XOR violation — a pago must attach to exactly one sale surface
--    (dispensacion_id XOR servicio_extra_id XOR venta_id). MUST be 0.
SELECT 'origen_xor_violado' AS metric, COUNT(*)::bigint AS value
FROM public.pagos
WHERE (CASE WHEN dispensacion_id   IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN servicio_extra_id IS NOT NULL THEN 1 ELSE 0 END
     + CASE WHEN venta_id          IS NOT NULL THEN 1 ELSE 0 END) <> 1;

-- 6. Existing Anulación rows with negative monto — MUST be 0 remotely
--    (Room repair normalizes local ABS; remote should already be clean).
SELECT 'anulacion_negativa' AS metric, COUNT(*)::bigint AS value
FROM public.pagos WHERE tipo = 'Anulación' AND monto < 0;

-- 7. Any existing Reverso rows already present (should be 0 pre-rollout)
SELECT 'reverso_existentes' AS metric, COUNT(*)::bigint AS value
FROM public.pagos WHERE tipo = 'Reverso';

-- 8. servicios_extra.estado out of the (about to be) expanded domain — MUST be 0
SELECT 'servicios_estado_ood' AS metric, COUNT(*)::bigint AS value
FROM public.servicios_extra
WHERE estado IS NULL OR estado NOT IN ('Pendiente', 'Entregado', 'Anulado');

-- 9. dispensaciones.estado_entrega out of the expanded domain — MUST be 0
SELECT 'dispensaciones_estado_ood' AS metric, COUNT(*)::bigint AS value
FROM public.dispensaciones
WHERE estado_entrega IS NULL
   OR estado_entrega NOT IN ('Pendiente', 'Entregado', 'Anulado', 'Reclamada');

-- 10. Reembolso inventory. Reclaim refunds are the other new ledger tipo, and
--     unlike Reverso they carry NO reversa_pago_id, so nothing links them back
--     to an original. Any pre-existing row means the reclaim path was already
--     exercised by an older client and its cash effect flips sign the moment
--     pago_effect lands. Expected 0 pre-rollout.
SELECT 'reembolso_existentes' AS metric, COUNT(*)::bigint AS value
FROM public.pagos WHERE tipo = 'Reembolso';

-- 10b. Reembolso sample (empty when 10 is 0). Inspect before applying.
SELECT 'reembolso_sample' AS metric, id, optica_id, fecha, monto, metodo_pago,
       dispensacion_id, servicio_extra_id
FROM public.pagos WHERE tipo = 'Reembolso'
ORDER BY fecha DESC, id LIMIT 20;

-- 10c. Inventory of every tipo whose sign changes under pago_effect, i.e. the
--      rows whose contribution to monto_pagado / a_cuenta will be recomputed
--      as negative instead of positive. MUST be 0 before the trigger rewrite,
--      otherwise parent balances need a one-off resync after apply.
SELECT 'tipos_que_invierten_signo' AS metric, tipo, COUNT(*)::bigint AS value,
       COALESCE(SUM(monto), 0) AS monto_total
FROM public.pagos WHERE tipo IN ('Reembolso', 'Reverso')
GROUP BY tipo ORDER BY tipo;

-- 11. Effect-vs-parent-balance drift. The stored dispensaciones.monto_pagado /
--     servicios_extra.a_cuenta must already equal the signed effect of their
--     pagos. The CASE below is INLINE on purpose: preflight runs BEFORE the
--     migration, so public.pago_effect does not exist yet, and the same query
--     must stay runnable after apply for comparison. Any non-zero count means
--     the stored balance and the ledger disagree BEFORE this change, so the
--     trigger rewrite would inherit — not cause — the drift.
--     Tolerance 0.005 matches the saldo threshold used across the RPCs.
SELECT 'drift_dispensaciones' AS metric, COUNT(*)::bigint AS value
FROM public.dispensaciones d
WHERE abs(d.monto_pagado - COALESCE((
        SELECT SUM(CASE btrim(COALESCE(p.tipo, ''))
                       WHEN 'Abono'         THEN p.monto
                       WHEN 'Pago completo' THEN p.monto
                       WHEN 'Reembolso'     THEN -p.monto
                       WHEN 'Reverso'       THEN -p.monto
                       ELSE 0 END)
        FROM public.pagos p WHERE p.dispensacion_id = d.id), 0)) > 0.005;

SELECT 'drift_servicios_extra' AS metric, COUNT(*)::bigint AS value
FROM public.servicios_extra se
WHERE abs(se.a_cuenta - COALESCE((
        SELECT SUM(CASE btrim(COALESCE(p.tipo, ''))
                       WHEN 'Abono'         THEN p.monto
                       WHEN 'Pago completo' THEN p.monto
                       WHEN 'Reembolso'     THEN -p.monto
                       WHEN 'Reverso'       THEN -p.monto
                       ELSE 0 END)
        FROM public.pagos p WHERE p.servicio_extra_id = se.id), 0)) > 0.005;

-- 11b. Worst drift samples (empty when 11 is 0). Drives the repair decision.
SELECT 'drift_dispensaciones_sample' AS metric, d.id, d.optica_id, d.fecha,
       d.monto_pagado AS stored, x.efecto AS ledger, d.monto_pagado - x.efecto AS diff
FROM public.dispensaciones d
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(CASE btrim(COALESCE(p.tipo, ''))
                            WHEN 'Abono'         THEN p.monto
                            WHEN 'Pago completo' THEN p.monto
                            WHEN 'Reembolso'     THEN -p.monto
                            WHEN 'Reverso'       THEN -p.monto
                            ELSE 0 END), 0) AS efecto
    FROM public.pagos p WHERE p.dispensacion_id = d.id
) x
WHERE abs(d.monto_pagado - x.efecto) > 0.005
ORDER BY abs(d.monto_pagado - x.efecto) DESC LIMIT 20;

SELECT 'drift_servicios_extra_sample' AS metric, se.id, se.optica_id, se.fecha,
       se.a_cuenta AS stored, x.efecto AS ledger, se.a_cuenta - x.efecto AS diff
FROM public.servicios_extra se
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(CASE btrim(COALESCE(p.tipo, ''))
                            WHEN 'Abono'         THEN p.monto
                            WHEN 'Pago completo' THEN p.monto
                            WHEN 'Reembolso'     THEN -p.monto
                            WHEN 'Reverso'       THEN -p.monto
                            ELSE 0 END), 0) AS efecto
    FROM public.pagos p WHERE p.servicio_extra_id = se.id
) x
WHERE abs(se.a_cuenta - x.efecto) > 0.005
ORDER BY abs(se.a_cuenta - x.efecto) DESC LIMIT 20;

-- 12. Cross-optica pago/parent mismatch. The new reversa FK is composite on
--     optica_id, so tenant-consistent parentage becomes a schema invariant.
--     MUST be 0, otherwise the tenant-scoped link would reject valid history.
SELECT 'pago_optica_distinta_a_padre' AS metric, COUNT(*)::bigint AS value
FROM public.pagos p
LEFT JOIN public.dispensaciones d ON d.id = p.dispensacion_id
LEFT JOIN public.servicios_extra se ON se.id = p.servicio_extra_id
WHERE (d.id IS NOT NULL AND d.optica_id IS DISTINCT FROM p.optica_id)
   OR (se.id IS NOT NULL AND se.optica_id IS DISTINCT FROM p.optica_id);
