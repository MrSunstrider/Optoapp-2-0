# Tasks: Fix inventory movement referencia identity

- [x] Proposal authored
- [x] Identity helpers + unit tests (`MovimientoReferenciaIdentity`)
- [x] Writers: MonturasViewModel, RegaloDispensacionViewModel, OrdenCompraRepository, InventarioFisicoRepository (COMPLETADO guard)
- [x] Room `MIGRATION_45_46` backfill blank → `id`
- [x] Remote `20260815035245_backfill_empty_movimiento_referencia.sql` applied — prod `empty_refs=0`
      (local filename aligned to remote MCP apply timestamp).
- [ ] Full unit suite green
- [ ] Ship APK with Room 46 on CLK-LX3 (with inventory single-writer build)
