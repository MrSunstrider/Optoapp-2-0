# Tasks: Fix Montura Edit Tipo Aro & Delete

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 550–850 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 UX chips/material → PR2 soft-delete+sibling → PR3 CASCADE |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain (orchestrator DECIDED) |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain — branch `fix/montura-edit-tipo-aro-delete`; WU1→WU2→WU3 in order; no PRs yet
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Edit exclusive chips + material dropdown | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.MonturasViewModelTest --stacktrace` | Manual: open edit, pick chip + Aluminio | `MonturaForm.kt` (+ screen wiring only) |
| 2 | Soft-delete, roles, activo filter, sibling spawn | PR 2 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.MonturasViewModelTest --stacktrace` | Manual: deactivate row; add variant | VM/repo/coordinator/list/screen delete+sibling |
| 3 | Room 52→53 + Supabase FK CASCADE | PR 3 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.Migration52To53Test --stacktrace` | N/A — schema/unit only; GGA before remote | entities, `OptoDatabase*`, `supabase/migrations/*_montura_fk_cascade.sql` |

## Phase 1: Edit UX — chips & material (WU1)

- [x] 1.1 RED: assert edit save keeps selected `tipoAro` / material path in `optoapp/src/test/java/com/example/optoapp/viewmodel/MonturasViewModelTest.kt` (fail until form wires chips)
- [x] 1.2 GREEN: exclusive catalog `FilterChip`s → `form.tipoAro` in `optoapp/src/main/java/com/example/optoapp/ui/components/monturas/MonturaForm.kt`; accesorio hides rim/material
- [x] 1.3 GREEN: replace material `DropdownField` with `OptoDropdownMenuField` in `MonturaForm.kt` (Aluminio available); wire via `optoapp/src/main/java/com/example/optoapp/ui/screens/MonturasScreen.kt` if needed

## Phase 2: Soft-delete & list filter (WU2)

- [x] 2.1 RED: soft-delete sets `activo=false` + sync; unauthorized role blocked; catch→Spanish error; `sortedMonturas` hides inactive — `MonturasViewModelTest.kt`
- [x] 2.2 GREEN: `softDeleteMontura` via update path in `optoapp/src/main/java/com/example/optoapp/data/montura/MonturaInventoryCoordinator.kt` + facade `optoapp/src/main/java/com/example/optoapp/data/OptoRepository.kt`
- [x] 2.3 GREEN: VM `delete` try/catch + success “desactivado” in `optoapp/src/main/java/com/example/optoapp/viewmodel/MonturasViewModel.kt`; filter `activo` in sorted list
- [x] 2.4 GREEN: Delete gated by `AppRoles.canDeleteRecords`; deactivate dialog copy in `optoapp/src/main/java/com/example/optoapp/ui/components/monturas/MonturaList.kt` + `MonturasScreen.kt`

## Phase 3: Edit→sibling variant spawn (WU2)

- [x] 3.1 RED: sibling insert shares attrs + new tipo/stock; UNIQUE → “El SKU ya existe para ese tipo de aro.” — `MonturasViewModelTest.kt`
- [x] 3.2 GREEN: sibling CTA state (`siblingTiposAro`, `siblingStockPorTipo`) in `MonturaForm.kt`; exclude current + existing SKU tipos
- [x] 3.3 GREEN: save = `updateMontura` then `insertMonturas` in `MonturasViewModel.kt` / `OptoRepository.kt`; wire CTA in `MonturasScreen.kt`

## Phase 4: CASCADE FKs (WU3)

- [x] 4.1 RED: create `optoapp/src/test/java/com/example/optoapp/data/Migration52To53Test.kt` — capture SQL has CASCADE; migration registered
- [x] 4.2 GREEN: `onDelete = CASCADE` on montura FKs in `optoapp/src/main/java/com/example/optoapp/data/ordencompra/OrdenCompraEntity.kt` + `optoapp/src/main/java/com/example/optoapp/data/inventariofisico/InventarioFisicoEntity.kt`
- [x] 4.3 GREEN: `MIGRATION_52_53` rebuild OC items + IF detalle in `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt`; version 53 + register in `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt`
- [x] 4.4 GREEN: create `supabase/migrations/*_montura_fk_cascade.sql` (drop/recreate FKs `ON DELETE CASCADE`); **GGA before remote apply**

## Phase 5: Spec merge & post-verify gate

- [x] 5.1 Merge deltas into `openspec/specs/inventario-stock/spec.md` at archive (or when apply completes deltas)
- [x] 5.2 Checklist (orchestrator, post-`sdd-verify`): run **Judgment Day** before archive/push — not apply code
