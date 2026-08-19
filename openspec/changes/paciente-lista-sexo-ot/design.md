# Design

## Color

`OptoTokens.semantic`: `maleBlueLight/Dark`, `femaleRoseLight/Dark`. Pure `sexoAvatarColor(symbol, darkTheme): Color?` — null = primary. No usar `alertRed` (es error, no género). Azul ~1565C0 / 90CAF9; rosa ~C2185B / F48FB1.

## Búsqueda

DAO `searchPacientesForOptica` añade `id IN (SELECT pacienteId FROM dispensaciones … ot LIKE)` y lo mismo en `servicios_extra`. Pure `pacienteMatchesListQuery` cubre el recorte in-memory y documenta el contrato. ViewModel: si hay query y filtro, `combine(search, pending) { ids ∩ }`.

## Filtros

Saldo: `montoTotal - COALESCE(SUM(PagoEffect), cache)` y estados no cancelados. Entrega pendiente: `estado = Pendiente` **y** fecha nula (OT 4676). Asignar fecha sincroniza estado a Entregado.

## RDD

`rdd_mode=disabled/unmanaged`. No hay issue aprobado ni receipts. Principios: tests de comportamiento, un invariante por filtro/búsqueda/color, rollback = revert del change.
