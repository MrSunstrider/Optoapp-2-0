# Tasks — tenant-dao-optica-scope-batch2

## B1 — Costos (~120 LOC) #91

- [ ] T1 RED: lookup foreign optica → null (Producto + Biselado + Lc path)
- [ ] T2 GREEN: DAO + DispensacionViewModel pass session opticaId
- [ ] T3 evidence/pr1.md + suite + GGA-eq + PR

## B2 — Pago helpers (~200 LOC) #92

- [ ] T4 RED: sum/credit/reverso foreign → isolated
- [ ] T5 GREEN: DAO + CalcularMontoPagadoUseCase + CancelLedger + repos/VMs; kill unscoped production paths
- [ ] T6 evidence/pr2.md + suite + GGA-eq + PR (base B1)

## B3 — Parent FK / reassign (~180 LOC) #93

- [ ] T7 RED: items/movimientos/reassign foreign isolation
- [ ] T8 GREEN: DAOs + callers
- [ ] T9 evidence/pr3.md + verify-report + archive; suite + GGA-eq + PR
