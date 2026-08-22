# Archive report: tenant-dao-optica-scope

Archived: 2026-08-22
Change: tenant-dao-optica-scope
Mode: hybrid (Engram + OpenSpec)

## Verdict

PASS — all three chained PRs green on `:optoapp:testDebugUnitTest` with GGA-eq R1–R4 APPROVED.

## Delivery

| PR | Branch | Issue | Result |
|----|--------|-------|--------|
| PR1 | `fix/tenant-scope-inventario-fisico` | #84 | InventarioFisicoDao scoped |
| PR2 | `fix/tenant-scope-dispensacion-servicio` | #85 | Disp/Servicio/Item/Regalo; kill Pago legacy |
| PR3 | `fix/tenant-scope-proveedor-oc-movimiento` | #86 | Proveedor/OC/Movimiento + receiveItems parent gate |

## Invariants

- INV-1: No DAO with opticaId exposes unscoped PK get/delete (in-scope set)
- INV-2: Sync bump uses scoped signatures
- INV-3: Single-writer venta / `(referenciaId,tipo,monturaId)` unchanged

## Out of scope (batch 2)

PagoDao sum/credit helpers; CostoProducto/CostoBiselado lookups without optica_id.

## Artifacts

proposal.md, design.md, specs/tenant-isolation/spec.md, tasks.md, verify-report.md, evidence/pr1–pr3.md, archive-report.md (this file).
