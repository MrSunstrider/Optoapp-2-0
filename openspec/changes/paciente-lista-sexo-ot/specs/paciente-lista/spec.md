# Spec — paciente-lista-sexo-ot

## REQ-1 — Color de sexo en claro y oscuro

El avatar de lista MUST usar azul para `SexoSymbol.MARTE` y rojo/rosado para `SexoSymbol.VENUS`. MUST contrastar en tema claro y oscuro. `DESCONOCIDO` MAY seguir el primary del tema.

### Scenario: masculino en claro
- **GIVEN** `sexo` masculino y tema claro
- **THEN** el tint del glifo es el azul claro del token, no primary

### Scenario: femenino en oscuro
- **GIVEN** `sexo` femenino y tema oscuro
- **THEN** el tint es el rosa oscuro del token (más claro que el de tema claro)

## REQ-2 — Filtros de lista

`Saldo Pendiente` MUST listar pacientes con saldo no cancelado. El pagado MUST ser `SUM(PagoEffect)` de los cobros de esa entidad si existen; si no hay cobros, el cache `montoPagado`/`aCuenta`. `Estado de entrega` MUST listar pacientes que aún esperan entrega: dispensación `estadoEntrega = Pendiente` **y** `fechaEntrega` nula, o servicio `estado = Pendiente` **y** `fecha_entrega` nula.

### Scenario: saldo ignora anulado
- **GIVEN** una dispensación Anulado con cache de saldo > 0
- **THEN** el paciente no aparece en Saldo Pendiente

### Scenario: cobros cubren el total con cache en 0
- **GIVEN** `montoPagado = 0` y un Abono igual al `montoTotal`
- **THEN** el paciente no aparece en Saldo Pendiente

### Scenario: cache inflado y Abono parcial
- **GIVEN** `montoTotal = 170`, `montoPagado = 200`, un Abono 100
- **THEN** el paciente aparece en Saldo Pendiente

### Scenario: entrega pendiente
- **GIVEN** una dispensación Pendiente sin fecha de entrega
- **THEN** el paciente aparece en Estado de entrega

### Scenario: Todos muestra la lista completa
- **GIVEN** un filtro de Saldo o Entrega y texto en buscar
- **WHEN** se elige Todos
- **THEN** aparecen todos los pacientes de la óptica y la búsqueda queda vacía

## REQ-3 — Búsqueda por OT

La búsqueda MUST coincidir OT de dispensación y de servicio extra del paciente, además de nombre, id, teléfono e historia.

### Scenario: OT de dispensación
- **GIVEN** paciente Ana con OT `4582`
- **WHEN** se busca `4582`
- **THEN** Ana está en el resultado y no un paciente de otra óptica con la misma OT

### Scenario: búsqueda + filtro
- **GIVEN** filtro Saldo Pendiente y query OT
- **THEN** solo pacientes que cumplen ambos
