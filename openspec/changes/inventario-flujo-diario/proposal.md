# Proposal — inventario flujo diario

## Intent

Operación diaria en óptica: ajustar stock por cantidad (±N), feedback claro (snackbar + offline), cerrar conteo con reporte de variance, filtrar lista Monturas|Accesorios. Spike de diseño para venta/consumo de accesorio con stock (sin writer nuevo aún).

## Depends on

Merge de Oleada A: PR #82 (`feat/inventario-accesorios-alta`) + PR #83 (`feat/inventario-menu-conteo`).

## Evidence

- ±1 en lista es lento para reposición de vitrina.
- Mensajes de error/éxito aún Text suelto en varios flujos.
- `VarianceReportScreen` existe pero no se navega al cerrar conteo.
- Lista mezcla monturas y ACCESORIO sin filtro.
- Venta de líquidos hoy no descuenta stock automáticamente.

## Scope

- **IN**: diálogo qty; `registrarEntrada/Salida(qty)`; snackbar; banner offline H8; wire VarianceReport; chips filtro; design-only spike venta accesorio.
- **OUT**: receiveItems OC; migraciones remotas; segundo writer SALIDA_VENTA; barcode.

## Causal invariants

INV-1: Ajustes ±N reusan el mismo path de movimiento que ±1 (no nuevo writer de venta).
INV-2: Filtro Accesorios no cambia sync ni `categoria`.
INV-3: Variance report es solo lectura + navegación; cierre de sesión IF intacto.
