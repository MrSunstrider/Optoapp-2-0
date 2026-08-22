# Spec — inventario flujo diario

## Requirements

### R1 — Ajuste por cantidad
- WHEN el usuario elige entrada/salida con cantidad N (>0)
- THEN el stock cambia en N unidades y se registra un movimiento con `cantidad=N`.

### R2 — Feedback
- WHEN un ajuste o guardado termina
- THEN se muestra snackbar de éxito o error (no solo Text suelto).
- WHEN no hay red (H8)
- THEN Monturas muestra banner offline sin bloquear lectura local.

### R3 — Variance al cerrar
- WHEN se completa un conteo físico
- THEN se muestra VarianceReport con faltantes y sobrantes de esa sesión.

### R4 — Filtro de lista
- WHEN el usuario elige chip Monturas o Accesorios
- THEN la lista solo muestra ítems de ese kind (`categoria` / `InventarioItemKind`).

### R5 — Spike venta accesorio (spec-only)
- Documentar opciones para descontar stock de ACCESORIO en venta/consumo.
- MUST NOT implementar nuevo writer de venta en este change.
