# Tasks — tenant-dao-optica-scope

## PR1 — Inventario físico (~150 LOC)

- [x] T1 RED: `getById(id, wrongOptica) → null`
- [x] T2 GREEN: DAO + repo + VM + close/upsert callers
- [x] T3 evidence/pr1.md + verify-report
- [x] T4 suite + GGA-eq + PR

## PR2 — Disp / Servicio / Item / Regalo / Pago (~350 LOC)

- [x] T5 RED tests per DAO
- [x] T6 GREEN plumb OptoRepository, CancelLedger, Bump, VMs; kill `PagoDao.getPagoById`
- [x] T7 evidence/pr2.md
- [x] T8 suite + GGA-eq + PR (base PR1)

## PR3 — Proveedor / OC / Movimiento (~250 LOC)

- [x] T9 RED tests
- [x] T10 GREEN repos + Bump
- [x] T11 evidence/pr3.md + verify final
- [x] T12 suite + GGA-eq + PR; archive change
