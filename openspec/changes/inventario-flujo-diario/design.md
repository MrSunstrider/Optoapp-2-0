# Design — inventario flujo diario

## Approach

1. **Qty dialog** — UI pide N; `MonturasViewModel.registrarEntrada/Salida(id, qty)` valida `qty > 0` y reusa coordinator existente.
2. **Feedback** — SnackbarHost en `MonturasScreen`; mapear Resource/errores a mensajes; banner offline con patrón H8 ya usado en otras pantallas.
3. **Variance** — al `closeSession` exitoso, navegar a `VarianceReportScreen(sessionId)` con faltantes/sobrantes.
4. **Filtros** — StateFlow `filtroKind: ALL|MONTURA|ACCESORIO` sobre lista ya cargada (`InventarioItemKind`).
5. **Spike venta accesorio** — solo `design.md` / ADR corto: opciones (línea OT no-armazón vs movimiento manual vs dispensación dedicada). No implementar writer hasta approval.

## Risks

| Risk | Mitigation |
|------|------------|
| Qty grande tipográfico | Cap razonable + confirmación si N > stock |
| Variance rompe back stack | Ruta explícita + pop a lista IF |
| Filtro desincroniza búsqueda | Aplicar filtro después de query text |

## Delivery

1–2 PRs encadenados tras merge A; TDD por task en `tasks.md`.
