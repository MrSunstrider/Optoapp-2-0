# Tasks — inventario-flujo-diario

Orden TDD (RED → GREEN). Aplicar **solo tras merge** de #82/#83.

- [ ] T1: Tests `MonturasViewModel.registrarEntrada/Salida(qty)` (qty inválida, qty=N, stock insuficiente si aplica)
- [ ] T2: Implementar diálogo ±N + wire VM
- [ ] T3: Tests/mensajes snackbar; reemplazar Text suelto en Monturas
- [ ] T4: Banner offline H8 en Monturas (patrón existente)
- [ ] T5: Wire `VarianceReportScreen` desde cierre de conteo + smoke navegación
- [ ] T6: Chips filtro Monturas | Accesorios + tests de filtrado
- [ ] T7: Spike/design venta accesorio con stock (sin código de writer)
- [ ] T8: `testDebugUnitTest` + GGA-eq R1–R4 + PR(s) issue-first

## Delivery forecast

| Slice | Contenido | ~líneas |
|-------|-----------|---------|
| PR-A | T1–T4 qty + snackbar + offline | ~250–350 |
| PR-B | T5–T7 variance + filtros + spike | ~200–300 |

## Status

**Opened** — awaiting merge Oleada A before apply.
