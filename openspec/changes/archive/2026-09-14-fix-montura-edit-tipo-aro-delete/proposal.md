# Proposal: Fix Montura Edit Tipo Aro & Delete

## Intent

Edit cannot change `tipoAro` reliably (deprecated `DropdownField`); Delete hard-deletes and crashes on FK children. Align edit UX with create, soft-delete like proveedores, amend ADR-2 for edit→sibling spawn, CASCADE child FKs as hard-delete safety net.

## Scope

### In Scope
- Edit: exclusive `FilterChip`s → `form.tipoAro`; material → `OptoDropdownMenuField`; accesorio unchanged
- Soft-delete `activo=false` + sync + try/catch; Delete UI = `AppRoles.canDeleteRecords`; list filters `activo`
- Edit CTA “Añadir variante”: sibling rows (shared sku/marca/modelo/costo/precio/stockMinimo/material; new `tipoAro` + stock) via `insertMonturas`; amend ADR-2
- Room `MIGRATION_52_53` + Supabase `ON DELETE CASCADE` on `orden_compra_items.monturaId`, `inventario_fisico_detalle.monturaId`
- `inventario-stock` deltas + ViewModel/migration tests (TDD)

### Out of Scope
- Reactivate UI; create multi-insert changes; UI hard-delete; RLS; SET NULL FKs

### Follow-up
- After `sdd-verify`, orchestrator runs **Judgment Day** before archive/push

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `inventario-stock`: edit chips/material; soft-delete + roles + activo list; edit→sibling (ADR-2); CASCADE FK policy (soft-delete stays UI path)

## Approach

1. Exclusive chips + `OptoDropdownMenuField` on edit forms.
2. Soft-delete via `updateMontura`; catch errors; gate Delete; filter activos.
3. Non-accesorio CTA excludes current/existing SKU tipos; save then `insertMonturas`.
4. Room rebuild v52→53; Supabase FK CASCADE (GGA). Soft-delete never fires CASCADE.

Supabase **schema** only (FK ON DELETE). **No RLS**.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `MonturaForm.kt`, `MonturasScreen.kt`, `MonturaList.kt` | Modified | Chips, material, CTA, Delete gate |
| `MonturasViewModel.kt`, coordinator, `OptoRepository` | Modified | Soft-delete, sibling insert, activo filter |
| OC/IF entities, `OptoDatabase*`, `supabase/migrations/` | Modified/New | CASCADE FKs, v52→53 |
| `inventario-stock/spec.md`, ViewModel + migration tests | Modified/New | Deltas + TDD |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Soft-delete keeps UNIQUE | Med | Document; reactivate deferred |
| CASCADE wipes OC/IF on hard delete | Med | Soft-delete-only UI; design warn |
| Apply switches UI to hard delete | Med | Spec locks soft-delete UX |
| Remote FK before soft-delete app | Med | Same release or app first |
| Diff >400 lines | High | Chained PRs (UX vs CASCADE) |

## Rollback Plan

- App: revert commits / prior APK.
- CASCADE: reverse FK migration (Room + Supabase) under **GGA**; avoid mixed clients.

## Dependencies

- Confirmed handoff (satisfied); GGA before remote migrate; Judgment Day post-verify

## Success Criteria

- [ ] Edit tipo aro via chips; material via `OptoDropdownMenuField`
- [ ] Soft-delete no crash; Delete admin|gerente only; inactive hidden
- [ ] Sibling spawn from edit; UNIQUE uses existing error
- [ ] Room 52→53 + Supabase CASCADE; UI still soft-deletes
- [ ] Tests green; Judgment Day done post-verify
