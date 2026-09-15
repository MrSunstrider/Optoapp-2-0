## Exploration: fix-montura-edit-tipo-aro-delete

### Scope (expanded)

| # | Topic | Intent |
|---|--------|--------|
| 1 | Edit tipo aro UX | Exclusive `FilterChip`s on edit → `form.tipoAro`; material → `OptoDropdownMenuField`; accesorio unchanged |
| 2 | Delete crash | Soft-delete `activo=false` + sync + try/catch; Delete UI gated `admin`\|`gerente` |
| 3 | Multi-variant from edit | Spawn sibling rim-type row without leaving edit (extends ADR-2) |
| 4 | CASCADE FKs | Room + Supabase `ON DELETE CASCADE` for children → `monturas` (at least OC items + inventario físico detalle) |
| 5 | Post-verify gate | After `sdd-verify`, orchestrator runs **Judgment Day** (dual blind review) on this change |

Prior exploration (narrower) rejected CASCADE and edit multi-variant; **this document supersedes that**. Soft-delete remains the user-facing delete policy; CASCADE is a schema safety net for hard deletes and Remote/Room alignment.

### Current State

**Edit tipo aro / material**

- `MonturaEditForm` (`MonturaForm.kt`): create uses multi-select `FilterChip` + `stockPorTipoAro`; edit uses deprecated `DropdownField` (dialog only via trailing icon — hard to open). Material also uses `DropdownField` on both paths.
- `DropdownField` in `CommonComponents.kt` is `@Deprecated` → `OptoDropdownMenuField` (already used in dispensación / evaluación).
- Catalog: `OpticalCatalog.TIPO_ARO` = `Aro Completo` / `Semi al aire` / `Al aire`.
- Accesorio path clears `tipoAro` / material / dimensions and hides those fields.

**Create multi-insert (ADR-2 baseline)**

- Archive `2026-09-05-montura-tipo-aro-stock-variants` **ADR-2**: multi-insert on **create only**; edit = single `tipoAro` + `stockActual`.
- `MonturasViewModel.save`: create builds `variantStocks` from `selectedTiposAro` → `insertMontura` or transactional `insertMonturas` (one sync after commit). Edit updates one row via `updateMontura`.
- Uniqueness: `(sku, opticaId, tipoAro)` Room + Postgres.
- Spec `inventario-stock` encodes create multi-insert; no edit-spawn or soft-delete requirements yet.

**Delete crash / roles**

- `MonturasViewModel.delete`: `AuthorizationGuard.requireRole(admin|gerente)` then `repository.deleteMontura` — **no try/catch**. Uncaught `SQLiteConstraintException` / auth throw crashes the coroutine.
- Coordinator/DAO: hard `DELETE FROM monturas WHERE id AND opticaId`.
- Room FKs without `onDelete` (NO_ACTION):
  - `OrdenCompraItem.monturaId` → `Montura`
  - `InventarioFisicoDetalle.monturaId` → `Montura`
- Already CASCADE: `montura_movimientos`, `montura_proveedor`.
- Postgres (`remote_schema_dump.sql`): `orden_compra_items_montura_id_fkey` and `inventario_fisico_detalle_montura_id_fkey` have **no ON DELETE**; `montura_movimientos` / `montura_proveedor` CASCADE.
- UI: `MonturasScreen` uses `AppRoles.canEditInventory` (includes especialista, asesor/a, ventas) for Delete; VM requires admin|gerente. `AppRoles.canDeleteRecords` already exists and is unused here.
- Proveedor pattern: `softDelete` → `activo=false` + update; list via `getActivosByOptica`.
- Montura list: `getMonturasByOptica` returns all (`ORDER BY activo DESC`); `sortedMonturas` does **not** filter `activo` (only `porReponerMonturas` does). Soft-delete without list filter leaves inactive rows visible.
- Sync inventario: upsert by `id` including `activo`; soft-delete syncs as update (no remote DELETE). Hard local DELETE does **not** mark remote tombstone — remote row can reappear on download unless a delete-sync path exists (today soft-delete is safer for sync).

**Multi-variant on edit — gap**

- No edit-screen CTA to add another rim type. Operator must cancel edit → create with same SKU and other tipos (error-prone; UNIQUE if they reselect existing tipo).
- Create path already implements sibling insert; edit path does not reuse `selectedTiposAro` / `insertMonturas`.

**CASCADE / Room version**

- Room DB `version = 52`. FK change needs table rebuild migration (SQLite cannot ALTER FK) → `MIGRATION_52_53` pattern like prior OC/IF table recreations in `OptoDatabaseMigrations.kt`.
- Supabase: drop + recreate FKs with `ON DELETE CASCADE`; requires GGA before remote apply.
- Sync implication: if local hard-deletes montura, CASCADE wipes local OC lines / IF detalle; remote still RESTRICT until migration lands — mixed clients risk upload/download inconsistency. Soft-delete avoids this entirely for the primary UX path.

### Affected Areas

- `optoapp/.../ui/components/monturas/MonturaForm.kt` — edit exclusive chips; material → OptoDropdownMenuField; edit “add variant” UI
- `optoapp/.../ui/screens/MonturasScreen.kt` — `MonturaEditFullScreen` CTA; Delete gate `canDeleteRecords`
- `optoapp/.../ui/components/monturas/MonturaList.kt` — separate `canDelete` vs `canEdit`
- `optoapp/.../viewmodel/MonturasViewModel.kt` — soft-delete + try/catch; list `activo` filter; spawn-sibling save path
- `optoapp/.../data/montura/MonturaInventoryCoordinator.kt` — softDelete via `updateMontura`; keep/retire hard delete
- `optoapp/.../data/OptoRepository.kt` — facade; reuse `insertMonturas` for sibling spawn
- `optoapp/.../data/ordencompra/OrdenCompraEntity.kt` — `onDelete = CASCADE` on montura FK
- `optoapp/.../data/inventariofisico/InventarioFisicoEntity.kt` — same
- `optoapp/.../data/OptoDatabase.kt` + `OptoDatabaseMigrations.kt` — v52→53 rebuild
- `supabase/migrations/` — ALTER FKs to CASCADE (GGA)
- `openspec/specs/inventario-stock/spec.md` — deltas: edit chips, soft-delete, edit→sibling variant, FK policy
- `optoapp/src/test/.../MonturasViewModelTest.kt` — soft-delete, roles, sibling spawn, UNIQUE conflict
- Migration tests for Room 52→53
- **Post-pipeline**: Judgment Day after `sdd-verify` (orchestrator-owned; not code)

### Approaches

#### A. Edit UX + soft-delete (bugs 1–2)

1. **Confirmed product fix** — Exclusive FilterChips → `tipoAro`; material → `OptoDropdownMenuField`; soft-delete + try/catch; UI `canDeleteRecords`; filter catalog `activo==true`.
   - Pros: Matches proveedor pattern; no hard-delete crash; fixes role mismatch; low surface.
   - Cons: UNIQUE slot retained until reactivate; dialog copy should say deactivate.
   - Effort: Low–Medium

#### B. Multi-variant from edit (topic 3) — amend ADR-2

1. **Sibling spawn CTA (recommended)** — On edit (montura, not accesorio): action “Añadir variante de aro” that keeps shared fields (sku/marca/modelo/material/…) and opens a compact multi-select of **catalog tipos minus current `tipoAro` (and minus tipos already existing for that SKU if queryable)**. Save path: keep current row unchanged (or save edits first) + `insertMonturas` for new tipos with per-type stock. Optionally stay on edit of original or navigate to new row.
   - Pros: Reuses create multi-insert machinery; clear intent; does not mutate current row’s tipo by accident; UNIQUE conflicts map to existing message.
   - Cons: Needs sibling-existence check (query same SKU) for good UX; slight ADR-2 amendment.
   - Effort: Medium

2. **Inline multi-chip on edit** — Edit shows all catalog chips; current tipo selected; adding chips inserts siblings on save; changing current chip updates `tipoAro`.
   - Pros: One control language with create.
   - Cons: Easy to confuse “change this row’s tipo” vs “add sibling”; risk of UNIQUE on change; higher cognitive load.
   - Effort: Medium–High

3. **Prefill create only** — Button leaves edit and `startCreate()` with copied fields + empty `selectedTiposAro`.
   - Pros: Minimal code.
   - Cons: Leaves edit screen (fails “without leaving edit” requirement).
   - Effort: Low — **rejected by product wording**

#### C. CASCADE FKs (topic 4)

1. **CASCADE + soft-delete primary (recommended)** — Migrate Room + Supabase FKs for `orden_compra_items.montura_id` and `inventario_fisico_detalle.montura_id` to `ON DELETE CASCADE`. User delete remains soft-delete. Hard delete (if any internal/admin path) no longer crashes; CASCADE deletes child history.
   - Pros: Aligns with `montura_movimientos`; fixes crash if hard delete ever runs; Room/Postgres parity.
   - Cons: Destructive to PO lines / physical-count lines on hard delete; remote migration + GGA; Room table rebuild; soft+CASCADE coexistence must be documented so apply does not “switch UX to hard delete” by accident.
   - Effort: Medium–High

2. **CASCADE only, keep hard delete as UX** — Drop soft-delete; rely on CASCADE.
   - Pros: Frees UNIQUE slot; simpler mental model for “Eliminar”.
   - Cons: Destroys audit/history; sync tombstone gap; contradicts proveedor pattern and confirmed soft-delete policy.
   - Effort: Medium — **not recommended**

3. **SET NULL instead of CASCADE** — Would require nullable `monturaId` (today NOT NULL) — schema + UI breakage.
   - Effort: High — reject

### Recommendation

Ship **one change** covering all five topics with this policy stack:

| Area | Decision |
|------|----------|
| Edit tipo aro | Exclusive single-select FilterChips → `form.tipoAro` (catalog labels). Accesorio hides aro/material. |
| Material | Replace `DropdownField` with `OptoDropdownMenuField` in inventory montura form. |
| Delete UX | Soft-delete `activo=false` via `updateMontura` + inventario sync; try/catch (incl. `AuthorizationGuard`); Spanish error; Delete UI = `AppRoles.canDeleteRecords`; filter primary listing to `activo`. Copy: deactivate language. |
| Edit → sibling variant | Amend ADR-2: create multi-insert unchanged; edit gains **“Añadir variante”** spawn that inserts additional rows (shared attrs, new `tipoAro` + stock) without leaving the edit screen. Prefer exclude already-existing SKU tipos. |
| CASCADE | Room 52→53 + Supabase FK CASCADE for OC items + IF detalle (movimientos already CASCADE). Soft-delete remains the only UI delete; CASCADE is safety/consistency for hard DELETE. Document history loss if hard delete used. |
| Post-verify | Orchestrator MUST run **Judgment Day** after successful `sdd-verify` before archive/push. |

Strict TDD: ViewModel tests for soft-delete, unauthorized role, exception→error, sibling insert, UNIQUE sibling conflict; Room migration test 52→53.

Review budget note for `sdd-tasks`: UX + soft-delete + sibling spawn + Room/Supabase FK migrations likely **>400 authored lines** → forecast chained PRs (app UX/delete vs schema CASCADE) unless `delivery_strategy` accepts exception.

### Risks

- Soft-delete retains `(sku, opticaId, tipoAro)` UNIQUE — recreate needs reactivate (optional follow-up) or sibling-only different tipos.
- Filtering only in ViewModel may leave inactive rows in other consumers of `getMonturasByOptica`; dispensación/search mostly use `activo=1` queries — confirm in design.
- CASCADE hard-delete wipes OC line items and physical inventory detalle — operational data loss if anyone calls hard delete.
- Soft-delete vs CASCADE: soft-delete never fires CASCADE; implementers must not “simplify” UI to hard delete after CASCADE lands.
- Remote FK migration without simultaneous app soft-delete still allows crash on old clients; coordinate release (app soft-delete first or same release as CASCADE).
- Sync: no montura remote tombstone on hard delete today — soft-delete is the sync-safe path.
- Sibling spawn without SKU-tipo existence check → UNIQUE errors (handled message exists).
- ADR-2 amendment must be explicit in proposal/spec so inventorio-stock “create only” wording is updated.
- GGA mandatory before remote migration; Judgment Day after verify adds schedule cost.
- Prior exploration said “no CASCADE” — treat as superseded to avoid proposal drift.

### Ready for Proposal

Yes — root causes verified in code; expanded scope is coherent. Orchestrator should run **sdd-propose** for `fix-montura-edit-tipo-aro-delete` covering UX, soft-delete, edit→sibling variant, CASCADE migrations, and the post-verify Judgment Day obligation. Rollback: revert app for soft-delete/UX/sibling; CASCADE rollback is a reverse FK migration (GGA) and must be called out separately.
