# Tasks — servicio-extra multi producto regalos

## 0. SDD baseline

- [x] 0.1 exploration.md + proposal.md
- [x] 0.2 specs/servicio-extra/spec.md (ADDED delta)
- [x] 0.3 design.md
- [x] 0.4 tasks.md (this file)

## 1. MontoDraftFormatting (TDD first)

- [ ] 1.1 RED: `MontoDraftFormattingTest` — empty/zero → `""`; never `"0.0"`; integer `50.0` → `"50"`; parse `""` → null; comma decimal parse
- [ ] 1.2 GREEN: implement `MontoDraftFormatting` (`formatDraft`, `formatDraftFromAutofill`, `parseDraft`)
- [ ] 1.3 Wire editable servicio line/header monto drafts to helper (UI follow-up in §6)

## 2. Domain helpers / stock identity (TDD)

- [ ] 2.1 RED: tests for sold-line sale `referenciaId = item.id` and reverso without colliding on multi distinct monturas
- [ ] 2.2 RED: regalo referencia via `movimientoReferenciaForRegalo` for servicio regalos
- [ ] 2.3 GREEN: any new identity helpers (only if reverso must become item-scoped); keep `DispensacionStockHelper` as sole writer

## 3. Entities + DAOs (TDD)

- [ ] 3.1 RED: `ServicioExtraItem` entity shape + DAO CRUD/by-servicio tests
- [ ] 3.2 GREEN: `ServicioExtraItem` entity/DAO
- [ ] 3.3 RED: `RegaloServicioExtra` entity + DAO tests (mirror `RegaloDispensacionDaoTest`)
- [ ] 3.4 GREEN: `RegaloServicioExtra` entity/DAO
- [ ] 3.5 Register entities/DAOs on `OptoDatabase` (version bump in §4)

## 4. Room migration 53→54 (TDD)

- [ ] 4.1 RED: `Migration53To54Test` — creates both child tables; preserves N `servicios_extra` rows
- [ ] 4.2 RED: backfill — one item per servicio; when `monturaId` set, `item.id == servicio.id` and monto/descripcion copied
- [ ] 4.3 GREEN: `MIGRATION_53_54` + `OptoDatabase.version = 54` + `addMigrations`
- [ ] 4.4 Assert CASCADE parent delete removes children (instrumented or in-memory FK test)

## 5. Supabase migration + RLS

- [ ] 5.1 Author migration: `CREATE TABLE servicio_extra_items` + `regalos_servicio_extra`, indexes, FK CASCADE
- [ ] 5.2 RLS policies by `optica_id` (restrictive, mirror `regalos_dispensacion`)
- [ ] 5.3 Optional remote backfill matching Room id rule
- [ ] 5.4 Extend schema integrity tests listing new tables / RLS expectations

## 6. Repository + ViewModel save/load/stock/regalos (TDD)

- [ ] 6.1 RED: repository load servicio with items + regalos aggregate
- [ ] 6.2 GREEN: repository wiring (get/replace items, get/replace regalos)
- [ ] 6.3 RED: `ServiciosViewModel` create — multi items, header denorm, sum `montoTotal`, stock per item via helper
- [ ] 6.4 RED: edit — restock removed lines, deduct new, unchanged ids skip stock rewrite
- [ ] 6.5 RED: regalos replace-all in same txn as pagos
- [ ] 6.6 GREEN: VM save/load/stock/regalos implementation
- [ ] 6.7 RED/GREEN: reject duplicate sold `monturaId` on one servicio (if enforced)

## 7. UI wizard

- [ ] 7.1 Paso 1: multi-line product list add/remove; `MonturaSearchField` + `inventarioParaServicioExtra`
- [ ] 7.2 Line monto drafts use `MontoDraftFormatting`; autofill from precio
- [ ] 7.3 Paso 2: Pagos unchanged + Regalos section (reuse IF/`RegalosSection` pattern)
- [ ] 7.4 Edit path loads items + regalos into UI state

## 8. Cancel (TDD)

- [ ] 8.1 RED: `CancelServicioExtraUseCase` restocks all item monturas + all regalos; sets `Anulado`
- [ ] 8.2 RED: second cancel idempotent (no extra movements)
- [ ] 8.3 GREEN: cancel implementation iterates children (not header-only)

## 9. Sync upload/download (TDD)

- [ ] 9.1 RED: upload order — `servicios_extra` before `servicio_extra_items` and `regalos_servicio_extra`
- [ ] 9.2 RED: download order — parents before children
- [ ] 9.3 GREEN: `UploadSyncCoordinator` / `DownloadSyncCoordinator` methods + DTO mapping
- [ ] 9.4 GREEN: `SyncFinanzasUseCase` orchestration + `DeletionSyncHelper` entity types
- [ ] 9.5 Update `SyncFinanzasUseCaseKtTest` mocks/order assertions

## 10. Integration / regression

- [ ] 10.1 Room in-memory: save multi-item + regalos + cancel restock end-to-end
- [ ] 10.2 Legacy backfill + cancel still restocks pre-migration single-product sale (`item.id == servicio.id`)
- [ ] 10.3 Dispensación picker/stock paths unchanged (smoke/regression tests)
- [ ] 10.4 `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [ ] 10.5 GGA before push; no remote migration without GGA

## Out of scope checklist (do not implement)

- [ ] Dispensación multi/regalos rewrite
- [ ] qty > 1 on sold servicio lines
- [ ] IF hub for servicio regalos
- [ ] optoweb parity
- [ ] Dispensación `MontoDraftFormatting` UX (deferred follow-up)
