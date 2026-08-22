# Tasks — tenant-dao-optica-scope

## PR1 — Inventario físico (~150 LOC)

- [ ] T1 RED: `getById(id, wrongOptica) → null`
- [ ] T2 GREEN: DAO + repo + VM + close/upsert callers
- [ ] T3 evidence/pr1.md + verify-report
- [ ] T4 suite + GGA-eq + PR

## PR2 — Disp / Servicio / Item / Regalo / Pago (~350 LOC)

- [ ] T5 RED tests per DAO
- [ ] T6 GREEN plumb OptoRepository, CancelLedger, Bump, VMs; kill `PagoDao.getPagoById`
- [ ] T7 evidence/pr2.md
- [ ] T8 suite + GGA-eq + PR (base PR1)

## PR3 — Proveedor / OC / Movimiento (~250 LOC)

- [ ] T9 RED tests
- [ ] T10 GREEN repos + Bump
- [ ] T11 evidence/pr3.md + verify final
- [ ] T12 suite + GGA-eq + PR; archive change
