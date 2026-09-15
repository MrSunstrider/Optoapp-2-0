# Exploration: fix-jd-reportes-cluster

## Topic

Fix **all** Judgment Day Round-1 SUSPECTS (S1–S6) and INFO items (I1–I4) from the financial reports cluster review (`judgment/reportes-cluster`). User direction: "solucionalo todo" — scope is the full ledger list, not only dual-confirmed severe.

Ledger source: Engram `#2120` JD R1 APPROVED (no dual-confirmed CRITICAL/HIGH; suspects + info only).

---

## Current State

### Cluster surfaces

| Surface | Primary files | Notes |
|---------|---------------|-------|
| Cierre de caja | `CierreCajaViewModel.kt`, `CierreCajaScreen.kt` | Local Room + PagoEffect; screen already uses `collectAsState(initial = null)` + null guard |
| Sync finanzas | `SyncFinanzasUseCase.kt` | Upload→download; `Resource.Error` still embeds `e.localizedMessage` |
| Análisis / BI | `AnalisisNegocioScreen.kt`, `rpc_analisis_mensual` | Role flash + gastos list bug; RPC regressed on stock_estancado / proyeccion |
| Reportes | `ReportesViewModel.kt`, `ReportesScreen.kt` | UI: `Diario\|Semanal\|Mensual\|Anual\|Total`; VM still has dead `"Este año"` branches |
| resumen_diario | `ResumenDiarioDao.kt`, `ResumenDiarioDaoTest.kt` | Missing `deleteAll()` (R13.1); test uses Robolectric + fake DELETE via raw SQL |

### Confirmed defect evidence

| ID | Evidence |
|----|----------|
| **S1** | `CierreCajaViewModel.kt:186` — `errorMessage = "Error al cargar datos: ${e.message}"`. Test asserts `.contains("DB corrupted")` (`CierreCajaViewModelTest.kt:717`). |
| **S2** | `rpc_cierre_caja_resumen` in `20260815005859_...sql:201-202` — `fecha >= p_from AND fecha < p_to`. Same-day `p_from = p_to` → empty. Android cierre uses inclusive `selectedDate`; no Android Kotlin caller of this RPC found (web/companion / future). Historical comments elsewhere call upper bound `fecha_fin_exclusiva`. |
| **S3** | `SyncFinanzasUseCase.kt:156-159` — `Resource.Error("...${e.localizedMessage}")` for `IOException` and generic `Exception`. Contrasts with already-static partial-upload string and with `ObtenerAnalisisMensualUseCase` static pattern (`REQ-2`). |
| **S4** | `AnalisisNegocioScreen.kt:49` — `collectAsState(initial = "admin")` → `canView` true until Auth resolves → unauthorized flash. `CierreCajaScreen.kt:49-50,94-97` is the correct pattern (`initial = null` + progress while null). |
| **S5** | `20260815010805_...sql:197-208` — hardcoded `ultima_venta=NULL`, `dias_sin_venta=999`, filter `stock_actual <= stock_minimo`. Spec **R23** requires remove low-stock filter + real dates from `montura_movimientos`/`dispensaciones`. Good reference body already exists in `20260709000003_fix_analisis_mensual_categorias.sql` (later overwritten by converge migration). |
| **S6** | `AnalisisNegocioScreen.kt:225-228` — card title "Gastos del mes" and `totalGastos` use `gastosMes`, but list iterates unfiltered `gastos`. |
| **I1** | `ResumenDiarioDaoTest` already uses `Room.inMemoryDatabaseBuilder` **and** `@RunWith(RobolectricTestRunner::class)`. All 18 `*DaoTest.kt` files still use Robolectric; AGENTS.md bans Robolectric for *new* tests. |
| **I2** | `ResumenDiarioDao` has no `deleteAll()`. Fake test `deleteAll_clearsData` runs raw `DELETE FROM resumen_diario` via `writableDatabase` — does not test a DAO API. Spec R13.1 requires `deleteAll()`. |
| **I3** | UI options at `ReportesScreen.kt:145` = Diario\|Semanal\|Mensual\|Anual\|Total. VM `dentroDelPeriodo` / `periodDateRange` still branch on `"Este año"` (unreachable from UI). Main spec `reportes-financieros` still documents `Este mes \| Este año \| Todo` — drift vs product. |
| **I4** | `v_proyeccion` built via `SELECT ... INTO` with `WHERE unpaid > 0.005`. Zero unpaid rows → PostgreSQL leaves `v_proyeccion` **NULL** → `jsonb_set` / return loses `egresos_programados`. |

---

## Affected Areas

### Android

- `optoapp/.../viewmodel/CierreCajaViewModel.kt` — S1
- `optoapp/.../viewmodel/CierreCajaViewModelTest.kt` — S1 test rewrite
- `optoapp/.../domain/SyncFinanzasUseCase.kt` — S3
- `optoapp/.../domain/SyncFinanzasUseCaseKtTest.kt` — S3 assertions if any assert on message shape
- `optoapp/.../ui/screens/AnalisisNegocioScreen.kt` — S4, S6
- `optoapp/.../ui/AnalisisNegocioScreenTest.kt` — optional role-guard coverage
- `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` — I2 `deleteAll()`
- `optoapp/.../data/resumendiario/ResumenDiarioDaoTest.kt` — I1/I2
- `optoapp/.../viewmodel/ReportesViewModel.kt` — I3 remove dead `"Este año"` (and any leftover `"Este mes"` if present)
- `optoapp/.../viewmodel/ReportesViewModelOtrosPeriodosTest.kt` — rename misleading `"Este año"` test names that already call `setPeriodo("Anual")`

### Supabase (new migrations only — do not edit applied history)

- New migration: `CREATE OR REPLACE` `rpc_cierre_caja_resumen` — S2 inclusive end
- New migration (same or companion): `CREATE OR REPLACE` `rpc_analisis_mensual` — S5 stock_estancado + I4 proyeccion always includes egresos
- Possibly update `supabase/tests/test_ledger_aggregate_convergence.sql` for inclusive-day assertion
- Reference restore body: `supabase/migrations/20260709000003_fix_analisis_mensual_categorias.sql` (stock_estancado CTE)

### Specs (delta in propose/spec phases)

- `openspec/specs/analisis-negocio/spec.md` — R13.1, R23, REQ-2 already cover I2/S5/S3; may need S4 role-loading scenario if missing
- `openspec/specs/cierre-caja/spec.md` — user-facing static errors; inclusive date semantics if RPC is documented
- `openspec/specs/reportes-financieros/spec.md` — **MODIFIED** Period Selection / Date Range to match UI labels Mensual\|Anual\|Total (I3)

---

## Approaches

### Overall delivery

1. **Single change / one PR cluster** — Implement all S+I in one branch with sequenced WUs.
   - Pros: Matches "solucionalo todo"; one verify cycle; shared SQL migration for S5+I4.
   - Cons: Likely >400-line review budget → chained PRs recommended at tasks phase.
   - Effort: Medium–High

2. **Split Android vs SQL PRs** — Android suspects/info first; SQL second with GGA+remote.
   - Pros: Clear blast radius; SQL can ship after GGA independently.
   - Cons: Two verify loops; temporary Android/SQL inconsistency if web relies on RPC.
   - Effort: Medium

### S2 date-bound convention

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. Inclusive `fecha <= p_to`** (prefer) | Matches Android `selectedDate` and cierre-caja inclusive day; same-day works with `p_from=p_to` | Breaks callers that pass exclusive end (`p_to = day+1`) | Low |
| B. Keep exclusive; document `p_to = day+1` | Preserves historical `fecha_fin_exclusiva` | Android/web must remember +1; easy to regress | Low |

**Recommendation:** Approach A. No Kotlin callers found; update COMMENT ON FUNCTION; grep optoweb companion before apply. Add SQL assertion: same-day range returns non-zero when pagos exist.

### S5 stock_estancado

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **Restore R23 CTE from `20260709000003`** | Already reviewed once; matches spec scenarios | Must re-apply atop current converge body (pago_effect proyeccion, etc.) | Medium |
| Rewrite from scratch | Fresh | Re-invent known-good SQL | High |

**Recommendation:** Port the `ventas_montura` / `montura_venta_agg` CTE into a new `CREATE OR REPLACE` of the **current** `rpc_analisis_mensual` (keep pago_effect paths). Same migration fixes I4.

### I4 proyeccion NULL

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **Always build jsonb via scalar subqueries / COALESCE fallback object** | egresos survive zero unpaid ventas | Slight SQL reshape | Low |
| Split SELECT egresos then COALESCE(v_proyeccion, zeros) | Minimal change | Easy to forget saldo_neto | Low |

**Recommendation:** Compute `egresos_programados` once; `COALESCE(v_proyeccion, jsonb_build_object(ingresos:=0, egresos:=..., saldo_neto:=...))` then set saldo_neto.

### I1 Robolectric

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **Keep Robolectric for this DAO test (parity with 17 siblings); fix deleteAll test** | Zero harness invention; I2 covered | Violates literal AGENTS "no Robolectric" | Low |
| Remove Robolectric without shared Context factory | AGENTS-pure | Will fail on JVM (`ApplicationProvider` needs Robolectric/instrumentation) | High / blocked |
| Move test to `androidTest` | True device/emulator Room | Slower CI; different package layout | Medium |

**Recommendation:** For this change, keep Robolectric (honest repo reality), add real `dao.deleteAll()`, rewrite test to call it. Record AGENTS debt as follow-up (shared non-Robolectric Room harness or androidTest migration for all DAOs). Do **not** block the JD fix cluster on harness work.

### I3 Reportes periods

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **Remove dead `"Este año"` (and unused `"Este mes"`) branches; keep Mensual/Anual/Total** | Matches UI; less dead code | Spec main still says Este año — needs delta | Low |
| Re-add Este año to UI | Spec-aligned labels | Product already chose Mensual/Anual/Total | Medium |

**Recommendation:** Remove unreachable branches; MODIFY `reportes-financieros` Period Selection requirement to document current UI labels. Rename tests that say "Este año" but already set `"Anual"`.

---

## Recommendation

Ship **one named change** `fix-jd-reportes-cluster` covering all S1–S6 and I1–I4 with Strict TDD, sequenced as work units below. Prefer **inclusive** RPC date bound (S2) and **restore known-good stock_estancado CTE** plus **NULL-safe proyeccion** in **one new Supabase migration** (do not edit `20260815005859` / `20260815010805`). Android static-error pattern mirrors `ObtenerAnalisisMensualUseCase` / analisis-negocio REQ-2. AnalisisNegocio role gate mirrors CierreCajaScreen. Reportes: delete dead period branches + spec delta.

### Recommended WU order

| WU | Scope | Findings | Notes |
|----|-------|----------|-------|
| **WU-1** | Static user-facing errors | S1, S3 | Red→green tests first; Log.e keeps raw `e.message` |
| **WU-2** | AnalisisNegocio UI | S4, S6 | Role null guard + iterate `gastosMes` |
| **WU-3** | ResumenDiarioDao | I2 (+ I1 pragmatic) | Add `deleteAll()`; rewrite test to call DAO; keep Robolectric this WU |
| **WU-4** | ReportesViewModel cleanup | I3 | Remove dead branches; rename misleading tests; flag spec MODIFIED |
| **WU-5** | Supabase RPCs | S2, S5, I4 | Single new migration preferred; GGA R1/R3/R4; remote apply; optional SQL test update |

Chained PRs likely: **PR-A** WU-1..4 (Android), **PR-B** WU-5 (migrations + GGA + remote). Or stack PR-B on PR-A.

### SQL migrations: GGA + remote apply?

**Yes — mandatory.**

- New migration versions are not yet on remote `schema_migrations`.
- Per AGENTS.md + project preference: run GGA (R1/R3/R4) before push / before remote DB migration; resolve all observations.
- Apply via normal Supabase migration path (`db push` / CI linked project) after GGA CLEAN — not by editing already-applied files.
- After apply: verify `rpc_cierre_caja_resumen` same-day inclusive; `rpc_analisis_mensual` stock_estancado + proyeccion with zero unpaid ventas.

---

## Risks

1. **S2 caller contract break** — Any client still passing exclusive `p_to` (day+1) will double-count the next day after inclusive fix. Mitigate: search optoweb + edge functions; update COMMENT; add regression SQL test.
2. **S5 result-set size** — Removing low-stock filter returns all active monturas with `stock_actual > 0` → larger JSONB for large inventories; UI must tolerate volume (existing R23 intent).
3. **I4 + S5 single REPLACE** — Must copy **full current** `rpc_analisis_mensual` body and only patch stock/proyeccion sections; accidental omission of pago_effect paths would regress converge work.
4. **I1 AGENTS tension** — Literal "no Robolectric" vs repo-wide DAO test pattern; over-scoping harness work delays JD fixes.
5. **I3 spec drift** — Leaving main `reportes-financieros` on Este año labels confuses future JD. Spec delta required in sdd-spec.
6. **Review budget** — Full cluster likely exceeds 400 authored lines → tasks phase should forecast chained PRs.
7. **No Android caller for S2** — Fix is still correct for web/RPC consumers and future Android; verify production call sites before labeling "done".

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `fix-jd-reportes-cluster` with scope = all S1–S6 + I1–I4, WU order above, inclusive S2, restore stock_estancado CTE + proyeccion COALESCE, Android static strings / role guard / gastosMes / deleteAll / dead period removal, and explicit GGA+remote for the new migration(s).

Clarify only if product wants to **keep** exclusive RPC bounds (Approach B) — default is inclusive.
