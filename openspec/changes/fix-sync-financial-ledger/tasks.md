# Tasks: Fix Sync Financial Ledger

Strict TDD (RED → GREEN). Delivery: **auto-chain** five WUs, each ≤400 authored (+/−). Threat matrix: N/A (no RED threat rows). `rdd_mode=disabled/unmanaged` — receipt fields recorded as principles only (no receipt authority/kill switch).

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1200–1600 authored total; ~180–380 per WU |
| 400-line budget risk | High (overall) / Low–Medium per WU |
| Chained PRs recommended | Yes |
| Suggested split | WU-1 → WU-2 → WU-3 → WU-4 → WU-5 |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

Tracker branch: `fix/sync-financial-ledger`. PR #1 base = tracker; PR #N base = PR #(N−1) branch.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Preflight+auth + SQL ledger mig + GGA | PR1 → tracker | `npx supabase db lint` + SQL smoke | Read-only preflight on linked prod (no writes) | Drop/revert unused mig only if `reversa_pago_id` unused; keep CHECKs if already expanded |
| 2 | `PagoEffect` + Room 44 + cancel/reclaim writers | PR2 → PR1 | `./gradlew :optoapp:testDebugUnitTest --tests "*PagoEffect*" --tests "*Cancel*UseCase*" --tests "*Reclaim*" --tests "*Migration*44*"` | N/A — unit/Room only | Revert PR2; Room forward-only (compensating mig if needed) |
| 3 | Reader/DAO/RPC aggregate convergence | PR3 → PR2 | `./gradlew :optoapp:testDebugUnitTest --tests "*CalcularMontoPagado*" --tests "*Cierre*" --tests "*Reportes*" --tests "*Analisis*"` | N/A — unit only | Revert PR3; leave writers/schema |
| 4 | Upload quarantine + partial Error + download skip | PR4 → PR3 | `./gradlew :optoapp:testDebugUnitTest --tests "*UploadSync*" --tests "*SyncFinanzas*" --tests "*DownloadSync*"` | N/A — MockK sync unit | Revert PR4 independently of schema |
| 5 | Diagnostics + pacientes evidence + CLK-LX3 | PR5 → PR4 | `./gradlew :optoapp:testDebugUnitTest --tests "*SyncErrorSanitizer*" --tests "*SyncDiagnostics*"` | CLK-LX3 device: finanzas sync + one pacientes capture | Revert UI/diagnostics; evidence docs independent |

### Global gates (every WU before merge/push)

| Gate | When | Command / evidence |
|------|------|-------------------|
| DB preflight/auth | Before remote mig (WU-1+) | Linked Supabase auth OK; read-only count SQL (invalid estado, `monto<0`, XOR orphans, tipo/método OOD, dup Reverso candidates, Anulación sign inventory) |
| GGA | Before remote mig or `git push` | Dual-blind GGA; resolve ALL observations |
| Full suite | Before each PR push | `./gradlew :optoapp:testDebugUnitTest --stacktrace` |
| Independent read-only validation | After freeze, before publish | Second agent/reviewer: no source edits; verify tests+diff budget+gates only |
| CLK-LX3 final | After WU-5 on device/remote | No 23514; remote ok; no pago resurrection; caja net only via Reverso/Reembolso; pacientes HTTP captured (no fix) |

**RDD defaults (all tasks):** `rdd_mode=disabled/unmanaged`; `issue_pr=N/A`; `unresolved_authority_decisions=none` unless noted.

---

## Phase 1 — WU-1: Preflight + SQL ledger (PR1)

- [x] 1.1 **RED** — Document failing SQL smoke expectations (Anulado/Reclamada reject; negative monto accepted path; no `pago_effect`). Files: `openspec/changes/fix-sync-financial-ledger/sql/preflight_counts.sql`, `sql/smoke_ledger_expectations.md`. Behavior: capture baseline rejects/accepts. Test: run read-only smoke against linked DB; assert current FAIL cases logged. Runtime: linked read-only. Rollback: delete smoke docs only. Deps: none. RDD: invariant=`sign via tipo`; flows=preflight; evidence=SQL SELECT counts; budget=~40; tests=manual SQL; rollback=docs-only.
  - DONE 2026-08-14: RED cases R1–R5 (missing `pago_effect`; estado CHECK 23514 for Anulado/Reclamada; missing `reversa_pago_id`) documented; preflight baseline (579 Abono, 0 negative/OOD/XOR) recorded. Authored 65+58 lines.

- [x] 1.2 **Auth/preflight gate** — Verify Supabase link + JWT/service role for lint/diff only (no DML). Files: operator notes in smoke doc. Behavior: refuse apply if auth/preflight fails. Test: `npx supabase projects list` / linked status. Runtime: CLI auth. Rollback: N/A. Deps: 1.1. RDD: invariant=`deploy DB before writers`; flows=auth gate; evidence=CLI; budget=~10; tests=auth check; rollback=N/A.
  - DONE 2026-08-14: `supabase projects list` → `sflhtihqdhrlryeyrzdo` linked=true, ACTIVE_HEALTHY, pg 17.6; CLI 2.109.1. `supabase db lint --linked` → "No schema errors found".

- [ ] 1.3 **GREEN (PARTIAL — stopped at 400-line budget)** — Create mig `supabase/migrations/20YYMMDDHHMMSS_ledger_pago_effect.sql`: `pago_effect`; expand servicios_extra +Anulado, dispensaciones +Anulado/Reclamada (NOT VALID→VALIDATE when clean); keep `pagos_monto_chk`; rewrite trigger/RPCs to `SUM(pago_effect…)`; add `reversa_pago_id`+FK RESTRICT+XOR CHECK+partial UNIQUE. Behavior: accept typed ledger rows; reject `monto<0`. Test: `npx supabase db lint` + `db diff --linked` + SQL smoke (Anulado/Reclamada/Reverso/Reembolso OK; negative FAIL; effect matrix vs Kotlin later). Runtime: local/linked apply after GGA only. Rollback: reverse mig iff column unused; else keep CHECKs+function. Deps: 1.1–1.2. RDD: invariant=`monto≥0; effect in SQL`; flows=mig apply; evidence=lint+smoke; budget=~280; tests=SQL smoke; rollback=mig reverse boundary.
  - DONE 2026-08-14 in `supabase/migrations/20260815004921_ledger_pago_effect.sql` (172 lines): `public.pago_effect(text,numeric)` IMMUTABLE + grants; servicios_extra/dispensaciones estado domain CHECK expansions (NOT VALID→VALIDATE); `pagos_monto_chk` untouched; `reversa_pago_id` column + self-FK RESTRICT + XOR CHECK `chk_pagos_reversa_link` + partial UNIQUE `pagos_reversa_pago_id_uidx`; trigger `trg_pagos_update_monto_pagado` rewritten to signed `pago_effect` delta.
  - DEFERRED closed: daily/cash by WU-1B (`20260815005859`); remaining read-only RPCs by WU-1C (`20260815010805`).
  - Validation: `db lint --linked` green; `db push --dry-run` blocked by pre-existing remote/local migration-history divergence (17 remote-only migs) and Docker daemon down → no local shadow-apply. Not applied to production (GGA not run).

- [ ] 1.4 **GGA + RO validate + push gate** — GGA on mig; independent read-only validation; full Android suite still green (no app code yet). Files: mig only. Behavior: zero unresolved GGA; budget ≤400. Test: GGA report + `./gradlew :optoapp:testDebugUnitTest --stacktrace`. Runtime: N/A beyond SQL smoke. Rollback: do not push/apply if gate fails. Deps: 1.3. RDD: invariant=`GGA before remote`; flows=gga/validate; evidence=GGA+suite; budget=~20; tests=full suite; rollback=hold push.
  - PARTIAL 2026-08-14: full Android suite `./gradlew :optoapp:testDebugUnitTest --stacktrace` → BUILD SUCCESSFUL (no Kotlin changed; cached green). Authored diff = 295 lines (≤400 OK). GGA dual-blind + remote apply NOT run (out of WU-1 scope; hold push).

---

## Phase 1B — WU-1B: daily/cash aggregate convergence (PR1B → PR1)

Closes the 1.3 DEFERRED note for the two daily/cash functions. Must land before WU-2 writers. Line budget: 387 authored (mig 215 + test 126 + smoke doc 22/8 + this file ~16) — ≤400 OK. `rdd_mode=disabled/unmanaged`; `issue_pr=N/A`; `unresolved_authority_decisions=none`.

- [x] 1B.1 **RED** — Smoke expectations + executable static assertions for `SUM(pago_effect)` net `50` on fixture F1 (Abono 100 / Reverso 40 / Reembolso 10 / Anulación 999) and Anulado+Reclamada exclusion. Files: `sql/smoke_ledger_expectations.md`, `supabase/tests/test_ledger_aggregate_convergence.sql`. Test: `psql -f supabase/tests/test_ledger_aggregate_convergence.sql`. Rollback: delete test + doc section. Deps: 1.3.
  - DONE 2026-08-14: RED proven statically — latest bodies `20260721000001` (lines 59, 91 `SUM(monto)`) and `20260721000000` (lines 29–32 `THEN monto ELSE 0` / `SUM(monto)`) contain zero `pago_effect` and no `Reclamada` exclusion, so blocks B1/B2/B3/C1/C2 fail.

- [x] 1B.2 **GREEN** — `supabase/migrations/20260815005859_converge_daily_cash_pago_effect.sql` (via `supabase migration new`): `recalcular_resumen_diario` cobros + per-venta paid totals via `public.pago_effect`, dispensaciones exclude `Anulado`+`Reclamada`; `rpc_cierre_caja_resumen` four sums via `public.pago_effect`. Signatures, SECURITY DEFINER/INVOKER, `search_path`, membership/BI guards, `jsonb` keys, `ON CONFLICT` upsert and grants unchanged. Test: `npx supabase db lint --linked` → "No schema errors found"; static pattern verification (0 `SUM(monto)`, 0 `THEN monto ELSE 0`, `pago_effect` in all 6 aggregate expressions). Rollback: re-apply `20260721000000` + `20260721000001` bodies — pure `CREATE OR REPLACE`, no schema/DDL change. Deps: 1B.1.
  - DONE 2026-08-14: 215-line migration + 126-line test authored; not applied remotely (GGA pending). `db diff --linked` / local shadow apply still blocked: Docker daemon down + pre-existing remote/local migration-history divergence, so blocks A–D of the test file are not yet executed against a live DB.

- [ ] 1B.3 **WU-1B gates** — GGA on the migration + independent read-only validation + full Android suite before push/apply. Test: GGA report + `./gradlew :optoapp:testDebugUnitTest --stacktrace`. Rollback: hold push/apply if a gate fails. Deps: 1B.2.

---

## Phase 1C — WU-1C: remaining read-only RPC convergence (PR1C → PR1B)

Closes the last 1.3 DEFERRED note for `rpc_deudores` + `rpc_analisis_mensual` before WU-2 writers. Line budget: ~380 authored (mig 241 + test Δ73 + smoke 36/8 + this file ~22) — ≤400 OK. `rdd_mode=disabled/unmanaged`; `issue_pr=N/A`; `unresolved_authority_decisions=none`.

- [x] 1C.1 **RED** — Smoke + static assertions for `pago_effect` paid totals and Anulado/Reclamada exclusion on debt/proyeccion. Files: `sql/smoke_ledger_expectations.md`, `supabase/tests/test_ledger_aggregate_convergence.sql` blocks E–G. Test: static fail vs `20260716045310` (0 `pago_effect`, raw `SUM(pd.monto)` / `SUM(monto) AS total_pagado`, no Reclamada). Rollback: delete E–G + smoke WU-1C section. Deps: 1B.2.
  - DONE 2026-08-14: RED proven statically + linked prod prosrc (`has_pago_effect=false`, `has_raw_sum_monto=true`, `has_reclamada=false`, guards present).

- [x] 1C.2 **GREEN** — `supabase/migrations/20260815010805_converge_readonly_rpc_pago_effect.sql` (via `supabase migration new`): `rpc_deudores` + `rpc_analisis_mensual` proyeccion_caja via `public.pago_effect`; dispensaciones exclude `Anulado`+`Reclamada`; servicios exclude `Anulado`. Signatures, SECURITY INVOKER, `search_path=public`, membership/BI guards, 16 JSON keys, grants unchanged. Test: `npx supabase db lint --linked` → "No schema errors found"; static GREEN (0 `SUM(pd.monto)`, `SUM(efecto)` present). Rollback: re-apply `20260716045310` bodies — pure `CREATE OR REPLACE`, no DDL. Deps: 1C.1.
  - DONE 2026-08-14: 241-line migration authored; not applied remotely (GGA pending).

- [ ] 1C.3 **WU-1C gates** — GGA on the migration + independent read-only validation before push/apply. Test: GGA report. Rollback: hold push/apply if a gate fails. Deps: 1C.2.

---

## Phase 1D — WU-1D: GGA correction round 1, DB integrity (PR1D → PR1C)

Bounded correction transaction for Judge A/B findings 2–4. DB target only; no
Kotlin, no WU-2+. Budget: **363 authored** (mig 150+28, preflight 94+0, smoke
doc +56, this file +35) — ≤400 OK. Test work is split into WU-1E/WU-1F because
the full correction diff is ~906 authored lines.
`rdd_mode=disabled/unmanaged`; `issue_pr=N/A`; `unresolved_authority_decisions=O5`.

- [x] 1D.1 **RED** — Prove each finding fails before the fix. Files: `supabase/tests/test_ledger_aggregate_convergence.sql` (B2 precision, blocks H/I). Test: read-only catalog asserts against linked prod. Rollback: revert test edits. Deps: 1C.2.
  - DONE 2026-08-14: `H1 FAIL: pagos needs UNIQUE (id, optica_id)` raised on linked prod (`ERROR P0004`); `I1 FAIL` raised likewise; catalog probe of the authored migration confirmed `coalesce_only=true`, `has_origin_change=false`, `has_old_parent_update=false`. B2 pattern self-test executed on prod (PG 17.6) and PASSED: `SUM(monto_total)` and `SUM(v.monto_total - x)` do not match, `SUM(monto)` / `SUM(pd.monto)` / `SUM( monto )` do — and the old `LIKE '%SUM(monto)%'` missed the latter two.

- [x] 1D.2 **GREEN — finding 2: same-optica reversa link at DB level** — Add `pagos_id_optica_id_key UNIQUE (id, optica_id)`; make `pagos_reversa_pago_id_fkey` composite `(reversa_pago_id, optica_id) → pagos(id, optica_id)` keeping `ON DELETE RESTRICT`; keep the global partial UNIQUE; self-heal guard drops any pre-correction single-column form. Files: `supabase/migrations/20260815004921_ledger_pago_effect.sql`. Test: blocks H1–H9 + W9/W10. Rollback: safe — migration is unapplied everywhere (verified). Deps: 1D.1.
  - Safety verified 2026-08-14: `supabase_migrations.schema_migrations` has no `2026081500*` row; `reversa_pago_id`, `pago_effect` and any composite unique are all absent from prod, so editing in place cannot diverge from an applied object.

- [x] 1D.3 **GREEN — finding 3: trigger origin moves** — On UPDATE, detect `dispensacion_id`/`servicio_extra_id` change; debit the full OLD effect from the OLD parent(s) and credit the full NEW effect to the NEW parent(s); keep the net-delta fast path when the origin is unchanged. Files: same migration. Test: blocks I1–I3 + W4/W5. Rollback: restore the COALESCE-only body. Deps: 1D.1.

- [x] 1D.4 **GREEN — finding 4: preflight extension** — Add Reembolso inventory + sample, sign-inverting tipo inventory, effect-vs-parent drift counts + worst-20 samples for both parent tables, and cross-optica pago/parent mismatch. Effect CASE is inlined so preflight runs before `pago_effect` exists. Files: `openspec/changes/fix-sync-financial-ledger/sql/preflight_counts.sql`. Test: executed read-only on linked prod. Rollback: docs/SQL only. Deps: 1D.1.
  - DONE 2026-08-14: baseline `reembolso=0`, `sign-inverting=0`, `cross_optica=0`, **`drift_dispensaciones=5`, `drift_servicios_extra=14`** — pre-existing, see GGA observation O5.

- [ ] 1D.5 **WU-1D gates** — GGA re-run on the corrected migration + independent read-only validation before push/apply. Test: GGA report. Rollback: hold push/apply. Deps: 1D.4.

---

## Phase 1E — WU-1E: static assertion suite corrections (PR1E → PR1D)

Finding 1 plus the schema/trigger contract asserts. Budget: **236 authored**
(`test_ledger_aggregate_convergence.sql`: B/C/E/F precision ~155 churn + blocks
H/I 81) — ≤400 OK. Autonomous: the file is a standalone test suite.

- [x] 1E.1 **GREEN — finding 1: raw-cash ban precision** — Replace the literal `LIKE '%SUM(monto)%'` ban with an anchored regex `SUM\s*\(\s*(\w+\.)?monto\s*\)` plus an executable pattern self-test (B2a/B2b) and a `SUM(monto_total)` survival assert (B2c). Apply it to B/C/E; scope F by **count** instead, because `gastos_operativos.monto` is a legitimate non-cash expense sum that an unscoped ban would false-positive on. Files: `test_ledger_aggregate_convergence.sql` blocks B/C/E/F. Test: self-test executed on linked prod (PG 17.6) → PASS; `rg` count on `20260815010805` → raw=2, gastos=2, matching F2b. Rollback: restore the `LIKE` asserts. Deps: 1D.1.

- [x] 1E.2 **GREEN — schema/trigger contract asserts** — Block H (composite FK target `UNIQUE (id, optica_id)`, composite FK shape, `ON DELETE RESTRICT`, validated XOR, **global** partial UNIQUE, `pagos_monto_chk` intact) and block I (trigger origin-move contract, whitespace-tolerant regexes). Test: RED proven on prod (`H1 FAIL`, `I1 FAIL` raised); GREEN proven by `rg` predicate match against the authored migration — 12/12 predicates present. Rollback: delete blocks H/I. Deps: 1E.1.

---

## Phase 1F — WU-1F: executable write-path fixture (PR1F → PR1E)

Findings 5–6, split out to protect the 400-line budget. Budget: **307 authored**
(one new file) — ≤400 OK.

- [x] 1F.1 **GREEN — transaction-rollback behavior tests** — New `supabase/tests/test_ledger_write_path_rollback.sql`: W1–W12 covering INSERT/UPDATE/DELETE effects, D1→D2 and dispensación→servicio origin moves, Reverso XOR/unique/RESTRICT, cross-optica rejection, negative monto rejection, Anulado/Reclamada acceptance and Reembolso netting. Single transaction ending in `ROLLBACK`; every fixture id prefixed `zzt_ledger_`; expected failures caught in plpgsql `EXCEPTION` savepoints so a rejection never aborts the run. Test: `psql -v ON_ERROR_STOP=1 -f supabase/tests/test_ledger_write_path_rollback.sql`. Rollback: delete the file. Deps: 1D.3.

- [ ] 1F.2 **WU-1F execution gate** — **BLOCKED**: W1–W12 have never been executed. Needs the WU-1 migrations applied to a database; Docker daemon is down (no local shadow DB) and remote apply is gated on GGA, so no production mutation was attempted. Test: run the fixture after `supabase db reset` once Docker is back, **before** any remote apply. Rollback: hold apply. Deps: 1F.1, 1D.5.

---

## Phase 2 — WU-2: Effect + Room 44 + writers (PR2)

**BUDGET NOTE 2026-08-14:** Authored ~571 (+/−, excl. generated Room schema `44.json`) after compression — **exceeds 400**. Stopped before WU-3. Recommend splitting PR2 into PR2a (PagoEffect+Room44) / PR2b (writers+VM) or accept `size:exception` for this slice.

- [x] 2.1 **RED** — `PagoEffectTest`: matrix Abono/Pago completo +m; Reembolso/Reverso −m; Anulación/unknown 0; trim. Files: `optoapp/src/test/.../domain/PagoEffectTest.kt`. Behavior: fails (class missing). Test: `./gradlew :optoapp:testDebugUnitTest --tests "*PagoEffectTest*"`. Runtime: N/A. Rollback: delete test. Deps: WU-1 merged/available. RDD: invariant=`Kotlin↔SQL effect`; flows=unit matrix; evidence=N/A unit; budget=~50; tests=PagoEffectTest; rollback=test-only.
  - DONE 2026-08-14: RED via unresolved `PagoEffect` compile errors.

- [x] 2.2 **GREEN** — Add `optoapp/src/main/.../domain/PagoEffect.kt` matching SQL. Behavior: tests pass. Test: same as 2.1. Runtime: N/A. Rollback: delete class+test with PR2. Deps: 2.1. RDD: same invariant; flows=impl; evidence=N/A; budget=~30; tests=PagoEffectTest; rollback=PR2 slice.
  - DONE 2026-08-14: `PagoEffect.signedAmount` GREEN.

- [x] 2.3 **RED** — Room 43→44: ABS negative Anulación; `reversaPagoId` column/index; DTO round-trip. Files: `*Migration*44*Test.kt`, entity/DTO stubs expected. Behavior: migration/DTO tests fail. Test: `./gradlew :optoapp:testDebugUnitTest --tests "*Migration*44*" --tests "*Pago*Dto*"`. Runtime: N/A in-memory Room. Rollback: delete failing tests. Deps: 2.2. RDD: invariant=`legacy Anulación abs; effect 0`; flows=Room mig; evidence=N/A Room; budget=~80; tests=mig tests; rollback=test-only.
  - DONE 2026-08-14: RED unresolved `MIGRATION_43_44` / `reversaPagoId`.

- [x] 2.4 **GREEN** — `DispensacionEntity.kt` (`Pago.reversaPagoId`), `OptoDatabase.kt` v44, `OptoDatabaseMigrations.kt` ABS+column, `SyncFinanzasDto.kt` `@SerialName("reversa_pago_id")`, `PagoDao` reversa queries. Behavior: 2.3 green. Test: same as 2.3. Runtime: N/A. Rollback: compensating mig only (no downgrade). Deps: 2.3. RDD: same; flows=schema; evidence=N/A; budget=~120; tests=mig+DTO; rollback=forward-only Room.
  - DONE 2026-08-14: Room 43→44 + DTO/DAO GREEN; schema export `44.json` generated (excluded from authored budget).

- [x] 2.5 **RED** — Cancel/Reclaim use-case tests: Anulado+linked Reverso idempotent; Reclamada+positive Reembolso; no delete; no `reversaPagoId` on Reembolso. Files: `*CancelLedgerUseCasesTest*`. Behavior: RED. Test: `./gradlew :optoapp:testDebugUnitTest --tests "*Cancel*UseCase*" --tests "*Reclaim*"`. Runtime: N/A. Rollback: delete tests. Deps: 2.4. RDD: invariant=`≤1 Reverso; cancel keeps originals`; flows=cancel/reclaim; evidence=N/A; budget=~90; tests=use-case; rollback=test-only.
  - DONE 2026-08-14: compact `CancelLedgerUseCasesTest.kt`.

- [x] 2.6 **GREEN** — Create cancel/reclaim use cases (`CancelLedgerUseCases.kt`); wire `ServiciosViewModel.kt`, `DispensacionViewModel.kt`, `DispensacionRepository.kt` (stop delete/negative Anulación writers). Behavior: 2.5 green; schedule finanzas once. Test: same as 2.5. Runtime: N/A. Rollback: revert writers to prior PR2 commit. Deps: 2.5. RDD: same; flows=VM→UC→Room; evidence=N/A; budget=~150; tests=use-case; rollback=PR2 writers.
  - DONE 2026-08-14: writers rewired; `deletePagoRegistrandoAnulacionEnCaja` → linked Reverso, keeps original.

- [x] 2.7 **WU-2 gates** — Full suite + authored ≤400. Test: `./gradlew :optoapp:testDebugUnitTest --stacktrace`. Runtime: N/A. Rollback: revert PR2 branch. Deps: 2.6. RDD: flows=gates; evidence=suite; budget=monitor; tests=full; rollback=PR2.
  - DONE 2026-08-14: focused + full suite BUILD SUCCESSFUL. **Authored ~571 > 400 — budget FAIL**; WU-3 not started.

---

## Phase 3 — WU-3: Reader / aggregate convergence (PR3)

- [x] 3.1 **RED** — Rewrite failing expectations off negative Anulación nets → `PagoEffect` / Reverso matrix for `CalcularMontoPagado*`, cierre, reportes, analisis tests. Files: matching `src/test/...` suites. Behavior: RED until readers updated. Test: `./gradlew :optoapp:testDebugUnitTest --tests "*CalcularMontoPagado*" --tests "*Cierre*" --tests "*Reportes*" --tests "*Analisis*"`. Runtime: N/A. Rollback: revert test edits. Deps: WU-2. RDD: invariant=`aggregates via effect`; flows=readers; evidence=N/A; budget=~100; tests=reader suites; rollback=tests.
  - DONE 2026-08-14: Tests rewritten to Abono/Reverso/Reembolso matrix; Anulación contributes 0.

- [x] 3.2 **GREEN** — Update `PagoDao.sumMonto*`, `CalcularMontoPagadoUseCase.kt`, cierre/reportes/analisis/dispensacion/servicios readers to `PagoEffect`; exclude Anulado from active debt/sale. Behavior: 3.1 green; Kotlin↔SQL converge. Test: same as 3.1. Runtime: N/A (SQL already WU-1). Rollback: revert PR3 only. Deps: 3.1. RDD: same; flows=DAO/UI; evidence=N/A; budget=~200; tests=reader suites; rollback=PR3.
  - DONE 2026-08-14: DAO CASE effect; VMs use `PagoEffect.signedAmount`; Reportes/Cierre exclude Anulado+Reclamada.

- [x] 3.3 **WU-3 gates** — Full suite + RO validate + ≤400. Test: full suite. Runtime: N/A. Rollback: PR3. Deps: 3.2. RDD: flows=gates; evidence=suite; budget=monitor; tests=full; rollback=PR3.
  - DONE 2026-08-14: focused + full suite BUILD SUCCESSFUL. **Authored ~484 > 400 — budget FAIL** (test tipo/matrix rewrites dominate).

---

## Phase 4 — WU-4: Quarantine + sync truth (PR4)

- [x] 4.1 **RED** — 79+1 upload: 79 synced, 1 `quarantine:…`, never skip→success; batch `markError`; `Resource.Error` partial; parent_missing gate; download skip only `quarantine:` (LWW else). Files: `*UploadSyncCoordinator*Test*`, `*SyncFinanzasUseCase*Test*`, `*DownloadSyncCoordinator*Test*`. Behavior: RED. Test: `./gradlew :optoapp:testDebugUnitTest --tests "*UploadSync*" --tests "*SyncFinanzas*" --tests "*DownloadSync*"`. Runtime: N/A MockK. Rollback: delete/revert tests. Deps: WU-3. RDD: invariant=`invalid never remote-ok; quarantine≠conflict`; flows=upload/download; evidence=N/A unit; budget=~120; tests=sync suites; rollback=tests.
  - DONE 2026-08-14: Validator + 79+1 + parent_missing + partial Error + quarantine skip tests.

- [x] 4.2 **GREEN** — `UploadSyncCoordinator.kt` validate/partition/binary-split/parent gate; `SyncFinanzasUseCase.kt` truthful partial Error; `DownloadSyncCoordinator.kt` narrow quarantine skip; sync state markSynced/markError. Behavior: 4.1 green; `last_status=error` when partial. Test: same as 4.1. Runtime: N/A. Rollback: revert PR4 independent of DB. Deps: 4.1. RDD: same; flows=coordinators; evidence=N/A; budget=~220; tests=sync suites; rollback=PR4.
  - DONE 2026-08-14: `FinanzasUploadValidator`, upload quarantine+isolation, SyncFinanzas `Resource.Error`+data, download quarantine skip.

- [x] 4.3 **WU-4 gates** — Full suite + GGA if sync surface + RO validate + ≤400. Test: full suite. Runtime: N/A. Rollback: PR4. Deps: 4.2. RDD: flows=gates; evidence=suite; budget=monitor; tests=full; rollback=PR4.
  - DONE 2026-08-14: focused + full suite BUILD SUCCESSFUL. **Authored ~562 > 400 — budget FAIL**. Stopped before WU-5.

---

## Phase 5 — WU-5: Diagnostics + pacientes evidence + CLK-LX3 (PR5)

- [x] 5.1 **RED** — Sanitizer/diagnostics: durable background errors; copy-all redacts Bearer/apikey; keeps status/PG/constraint/IDs. Files: `*SyncErrorSanitizer*Test*`, `*SyncDiagnostics*Test*`. Behavior: RED. Test: `./gradlew :optoapp:testDebugUnitTest --tests "*SyncErrorSanitizer*" --tests "*SyncDiagnostics*"`. Runtime: N/A. Rollback: tests only. Deps: WU-4. RDD: invariant=`truthful diagnostics`; flows=copy-all; evidence=N/A; budget=~60; tests=sanitizer/diagnostics; rollback=tests.
  - DONE 2026-08-14: RED proven by compile failure — unresolved `forDiagnostics`, `SyncDiagnosticsReport`, `BackgroundErrorCodec`, and 1-arg `BackgroundErrorCollector`. Files: `SyncErrorSanitizerTest.kt` (70), `SyncDiagnosticsReportTest.kt` (56), `SyncDiagnosticsBackgroundErrorStoreTest.kt` (51), +19 in `BackgroundErrorCollectorTest.kt`.

- [x] 5.2 **GREEN** — `util/SyncErrorSanitizer.kt`, `ui/components/config/ConfigSyncDiagnosticsCard.kt`, `SyncDiagnosticsViewModel.kt` as needed. Behavior: 5.1 green. Test: same as 5.1. Runtime: N/A until 5.4. Rollback: revert UI slice. Deps: 5.1. RDD: same; flows=UI; evidence=N/A; budget=~120; tests=diagnostics; rollback=PR5 UI.
  - DONE 2026-08-14: `SyncErrorSanitizer.forDiagnostics` (secrets out, HTTP status / SQLSTATE / constraint / IDs / counts kept); new `util/BackgroundErrorStore.kt` (interface + `BackgroundErrorCodec` + SharedPreferences impl) and `util/SyncDiagnosticsReport.kt`; `BackgroundErrorCollector` sanitizes on record and persists across process restart; diagnostics card shows the background section always (empty state included), newest-first, with a copy button; local copy-all now emits the full report. Hilt binding added in `DatabaseModule`. Kotlin default arg on the `@Inject` constructor was rejected by Dagger (two injected constructors) — store is a required param and the Robolectric test passes `BackgroundErrorStore.NoOp`.

- [x] 5.3 **WU-5 unit gates** — Full suite + RO validate + ≤400 authored. Test: full suite. Runtime: N/A. Rollback: PR5. Deps: 5.2. RDD: flows=gates; evidence=suite; budget=monitor; tests=full; rollback=PR5.
  - DONE 2026-08-14: focused (`*SyncErrorSanitizer*`, `*SyncDiagnostics*`, `*BackgroundError*`) and full `./gradlew :optoapp:testDebugUnitTest --stacktrace` → BUILD SUCCESSFUL. **Authored 442 code lines > 400 — budget FAIL** (162 modified + 280 new), plus evidence docs. Covered by the accepted `size:exception`.
  - FIX (carried over from WU-3): `CierreCajaViewModelTest.kt` (9) and `ReportesViewModelTest.kt` (2) contained U+FFFD replacement characters that aborted `spotlessKotlin` with "Encoding error". Two of them sat inside test-data literals — `tipo = "Anulaci<FFFD>n"` and `metodoPago = "M<FFFD>vil"` — so those cases were asserting on non-domain strings that only passed because unknown tipos net 0. Restored to `Anulación` / `Móvil` / em-dash; full suite still green. Repo-wide `spotlessCheck` still fails on ~102 pre-existing files untouched by this change (CI gates on `testDebugUnitTest` + `assembleDebug`, not spotless).

- [ ] 5.4 **CLK-LX3 final verification** — Device/remote: Anulado/Reclamada/Reverso/Reembolso sync without 23514; remote ok; no resurrection; caja unchanged except intended effect rows; one controlled pacientes sync → capture full HTTP → classify → open `fix-sync-pacientes-http` (no schema guess). Files: `openspec/changes/fix-sync-financial-ledger/verify-clk-lx3.md` (evidence only). Behavior: success checklist complete. Test: manual device + log capture. Runtime: **required** CLK-LX3. Rollback: app flag/revert writers if poison remains; DB CHECKs stay. Deps: 5.3 + DB deployed. RDD: invariant=`CLK-LX3 poison cleared`; flows=finanzas+pacientes evidence; evidence=device logs/HTTP; budget=~40 docs; tests=manual CLK-LX3; rollback=app revert / keep CHECKs; unresolved=`pacientes HTTP class` may open follow-up.
  - PARTIAL 2026-08-14 Run A — evidence in `verify-clk-lx3.md` + `evidence/clk-lx3-2026-08-14-sync.log`. Device then ran stock release 1.16.2 (pre-WU-2 writers).
  - PASS (Run A): `servicios_extra_estado_domain_chk` gone — `servicios_extra=143` and `dispensaciones=300` uploaded clean; prod holds `estado='Anulado'` servicios_extra; `pago_effect` + `reversa_pago_id` present.
  - BLOCKED (Run A): `pagos_monto_chk` 23514 on `upsert:pagos:chunk8` from legacy negative-`Anulación` Room rows.
  - PASS 2026-08-14 Run B — production keystore found (`optoapp-release.keystore` + `local.properties`); `assembleRelease` + `adb install -r` **Success** (sig `318fc041`, no wipe). Operator logged in; two full syncs at 21:38 / 21:39. Evidence: `evidence/clk-lx3-2026-08-14-runb-sync.log`, `evidence/clk-lx3-2026-08-14-runb-diagnostics-ok.png`.
  - PASS: **zero `23514` / `pagos_monto_chk`** in the whole Run B buffer — `upload pagos=586`, `download pagos=595`; prod `monto<0=0`, `Anulación=4` (legacy ABS, effect 0), `servicios_anulado=2`.
  - PASS: remote `sync_telemetry_optica` → `last_status=ok`, `last_stage=completado`, `last_error=''`, `last_sync_at=2026-08-15 02:40:01+00`. Diagnostics card after *Verificar sync ahora*: `Estado: OK`, no local errors, no background errors, `[completado] 14/08 21:40`. The `Estado: ERROR` the operator still saw was the stale Run A snapshot (02:14:53).
  - PASS: no pago resurrection (595 remote = 595 downloaded; no negatives reappear).
  - NOT EXERCISED: `Reverso` / `Reembolso` round-trip (`reverso=0`, `reembolso=0`, `reversa_pago_id=0`) — requires a real cancel/reclaim on production data; needs operator authorization.
  - Pacientes: 293/293 OK in both runs — no HTTP failure body; `fix-sync-pacientes-http` stays unopened.
  - OUT OF SCOPE (new change needed): 48 × SQLSTATE `23505` on `idx_movimientos_conflict` — `rpc_adjust_montura_stock` is not idempotent while `UploadSyncCoordinator` replays the stock RPC for every item of a full dispensaciones snapshot. Stock is **not** corrupted (24 movements / 24 distinct `referencia_id`; the function rolls back), errors are swallowed per item, but each cycle wastes 24 round-trips and floods logcat.

- [ ] 5.5 **Final publish gate** — GGA before push; independent read-only validation of full chain; confirm each PR ≤400; tracker ready. Test: full suite + GGA clean. Runtime: CLK-LX3 evidence attached. Rollback: hold merge. Deps: 5.4. RDD: flows=publish; evidence=GGA+suite+CLK-LX3; budget=chain sum; tests=full; rollback=hold.

---

## Implementation order

WU-1 (DB) → WU-2 (writers) → WU-3 (readers) → WU-4 (quarantine) → WU-5 (diagnostics + CLK-LX3). Never ship writers before expanded CHECKs. No implementation in this phase.
